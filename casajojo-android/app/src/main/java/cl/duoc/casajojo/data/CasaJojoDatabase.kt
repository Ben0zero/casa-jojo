package cl.duoc.casajojo.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.sqlite.db.SupportSQLiteDatabase
import cl.duoc.casajojo.data.dao.PedidoDao
import cl.duoc.casajojo.data.dao.ProductoDao
import cl.duoc.casajojo.data.entity.Pedido
import cl.duoc.casajojo.data.entity.Plato
import cl.duoc.casajojo.data.entity.Producto
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * Base de datos local (Room) con tres tablas: productos, pedidos y platos.
 * Se usa como Singleton: una sola instancia para toda la app.
 */
@Database(
    entities = [Producto::class, Pedido::class, Plato::class],
    version = 1,
    exportSchema = false,
)
@TypeConverters(Converters::class)
abstract class CasaJojoDatabase : RoomDatabase() {
    abstract fun productoDao(): ProductoDao
    abstract fun pedidoDao(): PedidoDao

    companion object {
        // @Volatile: todos los hilos ven el mismo valor actualizado de INSTANCE.
        @Volatile
        private var INSTANCE: CasaJojoDatabase? = null

        /** Devuelve la instancia existente o la crea (synchronized evita crear dos a la vez). */
        fun get(context: Context): CasaJojoDatabase =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    CasaJojoDatabase::class.java,
                    "casa_jojo.db",
                )
                    // CORRECCIÓN: antes la siembra del menú se lanzaba en cada
                    // llamada que creaba la instancia, en paralelo con la primera
                    // lectura. Ahora usamos el callback onCreate de Room, que se
                    // ejecuta UNA sola vez: cuando el archivo de la base se crea
                    // por primera vez en el teléfono.
                    .addCallback(SembrarMenu)
                    .build()
                    .also { INSTANCE = it }
            }

        /** Inserta el menú inicial (MenuSeed) al crear la base de datos. */
        private object SembrarMenu : RoomDatabase.Callback() {
            override fun onCreate(db: SupportSQLiteDatabase) {
                super.onCreate(db)
                // onCreate corre durante la primera consulta, que ocurre después
                // de que get() ya asignó INSTANCE; por eso podemos usar sus DAO.
                // Se inserta en un hilo de IO para no bloquear la apertura.
                CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
                    val dao = INSTANCE?.productoDao() ?: return@launch
                    // Doble seguridad: solo sembrar si la tabla sigue vacía.
                    if (dao.count() == 0) {
                        dao.insertAll(MenuSeed.menu())
                    }
                }
            }
        }
    }
}