package cl.duoc.casajojo.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import cl.duoc.casajojo.data.dao.PedidoDao
import cl.duoc.casajojo.data.dao.ProductoDao
import cl.duoc.casajojo.data.dao.UsuarioDao
import cl.duoc.casajojo.data.entity.Pedido
import cl.duoc.casajojo.data.entity.Plato
import cl.duoc.casajojo.data.entity.Producto
import cl.duoc.casajojo.data.entity.Usuario
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * Base de datos local (Room).
 * Versión 1: productos, pedidos, platos.
 * Versión 2: + tabla usuarios y columna pedidos.usuarioId (login).
 */
@Database(
    entities = [Producto::class, Pedido::class, Plato::class, Usuario::class],
    version = 2,
    exportSchema = false,
)
@TypeConverters(Converters::class)
abstract class CasaJojoDatabase : RoomDatabase() {
    abstract fun productoDao(): ProductoDao
    abstract fun pedidoDao(): PedidoDao
    abstract fun usuarioDao(): UsuarioDao

    companion object {
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

        fun get(context: Context): CasaJojoDatabase =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    CasaJojoDatabase::class.java,
                    "casa_jojo.db",
                )
                    .addMigrations(MIGRACION_1_2)
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
                    val dao = INSTANCE?.productoDao() ?: return@launch
                    if (dao.count() == 0) {
                        dao.insertAll(MenuSeed.menu())
                    }
                }
            }
        }
    }
}