package cl.duoc.casajojo.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import cl.duoc.casajojo.data.dao.PedidoDao
import cl.duoc.casajojo.data.dao.ProductoDao
import cl.duoc.casajojo.data.entity.Pedido
import cl.duoc.casajojo.data.entity.Plato
import cl.duoc.casajojo.data.entity.Producto
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

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
        @Volatile
        private var INSTANCE: CasaJojoDatabase? = null

        fun get(context: Context): CasaJojoDatabase =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    CasaJojoDatabase::class.java,
                    "casa_jojo.db",
                ).build().also { db ->
                    INSTANCE = db
                    seedMenu(db)
                }
            }

        private fun seedMenu(db: CasaJojoDatabase) {
            CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
                if (db.productoDao().count() == 0) {
                    db.productoDao().insertAll(MenuSeed.menu())
                }
            }
        }
    }
}