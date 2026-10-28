package cl.duoc.casajojo.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import cl.duoc.casajojo.data.local.entity.Producto
import kotlinx.coroutines.flow.Flow

@Dao
interface ProductoDao {
    @Query("SELECT * FROM productos ORDER BY id")
    fun todos(): Flow<List<Producto>>

    @Query("SELECT COUNT(*) FROM productos")
    suspend fun count(): Int

    @Insert
    suspend fun insertAll(productos: List<Producto>)
}