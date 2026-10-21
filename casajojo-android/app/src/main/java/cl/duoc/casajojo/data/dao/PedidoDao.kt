package cl.duoc.casajojo.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import cl.duoc.casajojo.data.entity.Pedido
import cl.duoc.casajojo.data.entity.PedidoWithPlatos
import cl.duoc.casajojo.data.entity.Plato
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
}