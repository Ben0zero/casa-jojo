package cl.duoc.casajojo.data.local.entity

import androidx.room.ColumnInfo
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
    // NUEVO (base de datos v2): id del usuario que hizo el pedido.
    // Es "Long?" (puede ser null) porque los pedidos creados antes del login
    // no tienen dueño. Así "Mi pedido" muestra solo los pedidos de quien inició sesión.
    val usuarioId: Long? = null,
    // NUEVO (base de datos v3, A4 / M9): id del pedido en ms-ventas (Oracle).
    // null = el pedido todavía no llega al servidor (pendiente de envío).
    val remotoId: Long? = null,
    // NUEVO (v3): UUID que identifica el pedido en la app y en el servidor; evita
    // duplicados al reenviar y reemplaza la búsqueda por número. "" en pedidos antiguos.
    @ColumnInfo(defaultValue = "")
    val referencia: String = "",
)

data class PedidoWithPlatos(
    @Embedded val pedido: Pedido,
    @Relation(parentColumn = "id", entityColumn = "pedidoId")
    val platos: List<Plato> = emptyList(),
)