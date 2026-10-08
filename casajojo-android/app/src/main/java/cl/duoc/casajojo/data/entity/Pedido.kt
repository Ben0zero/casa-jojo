package cl.duoc.casajojo.data.entity

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.Relation

@Entity(tableName = "pedidos")
data class Pedido(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val numero: Int,
    val nombre: String,
    val horaRetiro: String,
    val formaPago: String,
    val indicaciones: String,
    val estadoPedido: String,
    val estadoPago: String,
    val total: Int,
    val fecha: Long,
)

data class PedidoWithPlatos(
    @Embedded val pedido: Pedido,
    @Relation(parentColumn = "id", entityColumn = "pedidoId")
    val platos: List<Plato> = emptyList(),
)