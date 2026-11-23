package cl.duoc.casajojo.data.remote.dto

/** Contrato del microservicio ms-ventas adaptado a pedidos Casa JoJo. */

data class PedidoItemRequestDto(
    val nombre: String,
    val precio: Int,
    val solo: Boolean = false,
    val acompanamientos: List<String> = emptyList(),
)

data class PedidoRequestDto(
    val numero: Int,
    val nombre: String,
    val correo: String,
    val horaRetiro: String,
    val origen: String,
    val formaPago: String,
    val indicaciones: String,
    val items: List<PedidoItemRequestDto>,
    val total: Int,
    /** M9 / A4: UUID del pedido; ms-ventas no duplica un pedido reenviado con la misma referencia. */
    val referencia: String? = null,
)

data class PedidoItemResponseDto(
    val nombre: String? = null,
    val precio: Int? = null,
    val solo: Boolean? = null,
    val acompanamientos: List<String>? = null,
    val cantidad: Int? = null,
)

data class PedidoResponseDto(
    val id: Long? = null,
    val numero: Int? = null,
    val nombre: String? = null,
    val correo: String? = null,
    val horaRetiro: String? = null,
    val origen: String? = null,
    val formaPago: String? = null,
    val indicaciones: String? = null,
    val estadoPedido: String? = null,
    val estadoPago: String? = null,
    val total: Int? = null,
    val fechaCreacion: String? = null,
    val items: List<PedidoItemResponseDto>? = null,
    val referencia: String? = null,
)

/** EFT / RF-12: datos del retiro editables mientras el pedido sigue RECIBIDO. */
data class PedidoEdicionDto(
    val nombre: String,
    val horaRetiro: String,
    val formaPago: String,
    val indicaciones: String,
)
