package cl.duoc.casajojo.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
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

    /** EA3: actualiza el estado del pedido cuando llega una respuesta de la API. */
    @Update
    suspend fun actualizar(pedido: Pedido)

    @Query("SELECT * FROM pedidos WHERE numero = :numero ORDER BY id DESC LIMIT 1")
    suspend fun porNumero(numero: Int): Pedido?

    /**
     * CAMBIO (login): antes devolvía el último pedido de CUALQUIERA.
     * Ahora devuelve el último pedido del usuario que inició sesión.
     * @Transaction: Room lee el pedido y sus platos en una sola operación consistente.
     */
    @Transaction
    @Query("SELECT * FROM pedidos WHERE usuarioId = :usuarioId ORDER BY id DESC LIMIT 1")
    fun ultimoDelUsuario(usuarioId: Long): Flow<PedidoWithPlatos?>

    /** RF-14: todos los pedidos del usuario, el más reciente primero. */
    @Transaction
    @Query("SELECT * FROM pedidos WHERE usuarioId = :usuarioId ORDER BY id DESC")
    fun todosDelUsuario(usuarioId: Long): Flow<List<PedidoWithPlatos>>

    /** RF-14: pedidos del usuario filtrados por estado (Entregados / Cancelados). */
    @Transaction
    @Query("SELECT * FROM pedidos WHERE usuarioId = :usuarioId AND estadoPedido = :estado ORDER BY fecha DESC, id DESC")
    fun delUsuarioPorEstado(usuarioId: Long, estado: String): Flow<List<PedidoWithPlatos>>

    /** RF-13: cancelación segura; devuelve 0 si el plazo ya pasó o el estado cambió. */
    @Query("UPDATE pedidos SET estadoPedido = 'cancelado' WHERE id = :id AND estadoPedido = 'recibido' AND fecha >= :limite")
    suspend fun cancelar(id: Long, limite: Long): Int

    /** RF-14: retiro con QR → entregado y pagado; 0 si estaba cancelado/entregado. */
    @Query("UPDATE pedidos SET estadoPedido = 'entregado', estadoPago = 'pagado' WHERE id = :id AND estadoPedido NOT IN ('cancelado', 'entregado')")
    suspend fun marcarEntregado(id: Long): Int
}