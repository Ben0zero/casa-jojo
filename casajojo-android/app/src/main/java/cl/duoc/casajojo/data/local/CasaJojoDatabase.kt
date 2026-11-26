package cl.duoc.casajojo.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.withTransaction
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import cl.duoc.casajojo.data.local.dao.PedidoDao
import cl.duoc.casajojo.data.local.dao.ProductoDao
import cl.duoc.casajojo.data.local.dao.UsuarioDao
import cl.duoc.casajojo.data.local.dao.TarjetaDao
import cl.duoc.casajojo.data.local.entity.Tarjeta
import cl.duoc.casajojo.data.local.entity.Pedido
import cl.duoc.casajojo.data.local.entity.Plato
import cl.duoc.casajojo.data.local.entity.Producto
import cl.duoc.casajojo.data.local.entity.Usuario
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * Base de datos local (Room).
 * Versión 1: productos, pedidos, platos.
 * Versión 2: + tabla usuarios y columna pedidos.usuarioId (login).
 * Versión 3: + columnas pedidos.remotoId y pedidos.referencia (A4 / M9: envío
 *            pendiente y vínculo con ms-ventas sin depender del número).
 * Versión 4: + tabla tarjetas (Billetera simulada: solo marca, últimos 4 y vencimiento).
 */
@Database(
    entities = [Producto::class, Pedido::class, Plato::class, Usuario::class, Tarjeta::class],
    version = CasaJojoDatabase.VERSION,
    exportSchema = false,
)
@TypeConverters(Converters::class)
abstract class CasaJojoDatabase : RoomDatabase() {
    abstract fun productoDao(): ProductoDao
    abstract fun pedidoDao(): PedidoDao
    abstract fun usuarioDao(): UsuarioDao
    abstract fun tarjetaDao(): TarjetaDao

    companion object {
        /** Versión actual del esquema. Subirla exige agregar su Migration en [MIGRACIONES]. */
        const val VERSION = 4

        @Volatile
        private var INSTANCE: CasaJojoDatabase? = null

        /**
         * MIGRACIÓN 1 → 2.
         * Quien ya tenía la app instalada tiene la base en versión 1. En vez de
         * borrarla (y perder sus pedidos), le aplicamos estos cambios en SQL.
         * El SQL debe calzar EXACTO con las entidades; si no, Room avisa con un
         * error al abrir la base.
         */
        private val MIGRACION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Tabla nueva para Usuario.kt
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `usuarios` (" +
                            "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                            "`nombre` TEXT NOT NULL, " +
                            "`correo` TEXT NOT NULL, " +
                            "`telefono` TEXT NOT NULL, " +
                            "`passwordHash` TEXT NOT NULL, " +
                            "`salt` TEXT NOT NULL)"
                )
                // Índice único del correo (el mismo nombre que genera Room).
                db.execSQL(
                    "CREATE UNIQUE INDEX IF NOT EXISTS `index_usuarios_correo` ON `usuarios` (`correo`)"
                )
                // Columna nueva en pedidos; los pedidos antiguos quedan con NULL.
                db.execSQL("ALTER TABLE `pedidos` ADD COLUMN `usuarioId` INTEGER")
            }
        }

        /**
         * MIGRACIÓN 2 → 3 (A4 / M9). Los pedidos existentes quedan con remotoId NULL y
         * referencia vacía: se vinculan con el servidor por número solo esa vez (pedidos
         * antiguos); los nuevos usan su referencia (UUID).
         */
        val MIGRACION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `pedidos` ADD COLUMN `remotoId` INTEGER")
                db.execSQL("ALTER TABLE `pedidos` ADD COLUMN `referencia` TEXT NOT NULL DEFAULT ''")
            }
        }

        /**
         * MIGRACIÓN 3 → 4 (Billetera). Solo CREA la tabla nueva: no toca pedidos, platos,
         * productos ni usuarios, así que nadie pierde datos al actualizar la app.
         * El SQL calza EXACTO con Tarjeta.kt (Room lo compara al abrir la base):
         * Long/Int/Boolean → INTEGER NOT NULL, String → TEXT NOT NULL, índice por usuarioId.
         * SEGURIDAD: no hay columnas para el número completo ni para el CVV.
         */
        val MIGRACION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `tarjetas` (" +
                        "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                        "`usuarioId` INTEGER NOT NULL, " +
                        "`marca` TEXT NOT NULL, " +
                        "`ultimos4` TEXT NOT NULL, " +
                        "`titular` TEXT NOT NULL, " +
                        "`mesVencimiento` INTEGER NOT NULL, " +
                        "`anioVencimiento` INTEGER NOT NULL, " +
                        "`alias` TEXT NOT NULL, " +
                        "`predeterminada` INTEGER NOT NULL)"
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_tarjetas_usuarioId` ON `tarjetas` (`usuarioId`)"
                )
            }
        }

        /** Cadena completa 1 → 2 → 3 → 4 (la prueba unitaria revisa que no falte ningún salto). */
        val MIGRACIONES: Array<Migration> by lazy { arrayOf(MIGRACION_1_2, MIGRACION_2_3, MIGRACION_3_4) }

        fun get(context: Context): CasaJojoDatabase =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    CasaJojoDatabase::class.java,
                    "casa_jojo.db",
                )
                    // Sin fallbackToDestructiveMigration: cada versión se migra con SQL propio.
                    .addMigrations(*MIGRACIONES)
                    // Siembra el menú solo la primera vez que se crea la base.
                    .addCallback(SembrarMenu)
                    .build()
                    .also { INSTANCE = it }
            }

        /** Inserta el menú inicial (MenuSeed) al crear la base de datos. */
        private object SembrarMenu : RoomDatabase.Callback() {
            override fun onCreate(db: SupportSQLiteDatabase) {
                super.onCreate(db)
                // onCreate corre durante la primera consulta, cuando INSTANCE ya
                // está asignada; por eso podemos usar sus DAO.
                CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
                    val base = INSTANCE ?: return@launch
                    val dao = base.productoDao()
                    // B6: revisar y sembrar dentro de una transacción. La sincronización
                    // del menú también usa una, así que no se pueden intercalar y el menú
                    // ya no queda duplicado en la primera instalación.
                    base.withTransaction {
                        if (dao.count() == 0) {
                            dao.insertAll(MenuSeed.menu())
                        }
                    }
                }
            }
        }
    }
}