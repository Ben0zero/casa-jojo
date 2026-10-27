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

    /**
     * NUEVO (RF-14): pedidos del usuario en un estado (Historial: "entregado"
     * o "cancelado"), del más reciente al más antiguo.
     */
    @Transaction
    @Query("SELECT * FROM pedidos WHERE usuarioId = :usuarioId AND estadoPedido = :estado ORDER BY fecha DESC, id DESC")
    fun delUsuarioPorEstado(usuarioId: Long, estado: String): Flow<List<PedidoWithPlatos>>

    /**
     * NUEVO (RF-13): cancelación segura. Solo cambia el pedido si sigue
     * "recibido" y su fecha está dentro del plazo (fecha >= limite).
     * Devuelve cuántas filas cambió: 0 = ya no se podía cancelar.
     */
    @Query("UPDATE pedidos SET estadoPedido = 'cancelado' WHERE id = :id AND estadoPedido = 'recibido' AND fecha >= :limite")
    suspend fun cancelar(id: Long, limite: Long): Int

    /**
     * NUEVO (RF-14): al retirar con QR el pedido queda entregado y pagado.
     * Un pedido cancelado o ya entregado no cambia (devuelve 0).
     */
    @Query("UPDATE pedidos SET estadoPedido = 'entregado', estadoPago = 'pagado' WHERE id = :id AND estadoPedido NOT IN ('cancelado', 'entregado')")
    suspend fun marcarEntregado(id: Long): Int
}