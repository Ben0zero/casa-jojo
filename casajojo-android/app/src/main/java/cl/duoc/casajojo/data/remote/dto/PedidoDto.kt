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
)
