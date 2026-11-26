package cl.duoc.casajojo.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import cl.duoc.casajojo.data.local.entity.Tarjeta
import kotlinx.coroutines.flow.Flow

/**
 * Billetera: consultas de la tabla tarjetas. Todas filtran por usuarioId, así una
 * cuenta nunca ve ni modifica las tarjetas de otra en el mismo teléfono.
 */
@Dao
interface TarjetaDao {

    /** Reactiva: la predeterminada primero y luego la más reciente. */
    @Query("SELECT * FROM tarjetas WHERE usuarioId = :usuarioId ORDER BY predeterminada DESC, id DESC")
    fun deUsuario(usuarioId: Long): Flow<List<Tarjeta>>

    @Query("SELECT * FROM tarjetas WHERE usuarioId = :usuarioId ORDER BY id DESC")
    suspend fun listaDeUsuario(usuarioId: Long): List<Tarjeta>

    @Insert
    suspend fun insertar(tarjeta: Tarjeta): Long

    /** Deja como predeterminada SOLO la tarjeta [id] (las demás pasan a 0) en un único UPDATE. */
    @Query("UPDATE tarjetas SET predeterminada = (id = :id) WHERE usuarioId = :usuarioId")
    suspend fun marcarPredeterminada(usuarioId: Long, id: Long)

    @Query("DELETE FROM tarjetas WHERE id = :id AND usuarioId = :usuarioId")
    suspend fun eliminar(usuarioId: Long, id: Long): Int

    /** Al eliminar la cuenta se borran todas sus tarjetas. */
    @Query("DELETE FROM tarjetas WHERE usuarioId = :usuarioId")
    suspend fun borrarDelUsuario(usuarioId: Long)
}
