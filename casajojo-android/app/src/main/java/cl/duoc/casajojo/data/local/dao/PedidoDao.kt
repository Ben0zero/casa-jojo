package cl.duoc.casajojo.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import cl.duoc.casajojo.data.local.entity.Pedido
import cl.duoc.casajojo.data.local.entity.PedidoWithPlatos
import cl.duoc.casajojo.data.local.entity.Plato
import kotlinx.coroutines.flow.Flow

@Dao
interface PedidoDao {
    @Insert
    suspend fun insertPedido(pedido: Pedido): Long

    @Insert
    suspend fun insertPlato(plato: Plato)

    @Query("SELECT MAX(numero) FROM pedidos")
    suspend fun maxNumero(): Int?

    /**
     * CAMBIO (login): antes devolvía el último pedido de CUALQUIERA.
     * Ahora devuelve el último pedido del usuario que inició sesión.
     * @Transaction: Room lee el pedido y sus platos en una sola operación consistente.
     */
    @Transaction
    @Query("SELECT * FROM pedidos WHERE usuarioId = :usuarioId ORDER BY id DESC LIMIT 1")
    fun ultimoDelUsuario(usuarioId: Long): Flow<PedidoWithPlatos?>

    /**
     * NUEVO: historial completo del usuario, del más reciente al más antiguo.
     * Los pedidos nunca se borraban; la pantalla solo pedía el último (LIMIT 1).
     */
    @Transaction
    @Query("SELECT * FROM pedidos WHERE usuarioId = :usuarioId ORDER BY id DESC")
    fun todosDelUsuario(usuarioId: Long): Flow<List<PedidoWithPlatos>>
}