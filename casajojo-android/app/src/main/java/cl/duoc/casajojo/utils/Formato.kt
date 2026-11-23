package cl.duoc.casajojo.utils

import cl.duoc.casajojo.domain.model.EstadoPedido
import androidx.compose.ui.graphics.Color

fun Int.money(): String =
    "$" + this.toString().reversed().chunked(3).joinToString(".").reversed()

/** EFT (POO): el texto lo entrega cada EstadoPedido (polimorfismo). */
fun textoEstadoPedido(estado: String): String =
    EstadoPedido.desdeOrNull(estado)?.etiqueta() ?: estado.replaceFirstChar { it.uppercase() }

/** EFT (POO): el color lo entrega cada EstadoPedido (polimorfismo). */
fun colorEstadoPedido(estado: String): Color =
    Color(EstadoPedido.desdeOrNull(estado)?.color() ?: 0xFF6D6D67)

fun textoEstadoPago(estado: String): String = when (estado) {
    "pagado" -> "Pagado"
    "pendiente" -> "Pendiente"
    "no_pagado" -> "No pagado"
    else -> estado.replaceFirstChar { it.uppercase() }
}

fun colorEstadoPago(estado: String): Color = when (estado) {
    "pagado" -> Color(0xFF2F855A)
    "pendiente" -> Color(0xFFB7791F)
    "no_pagado" -> Color(0xFFC53030)
    else -> Color(0xFF6D6D67)
}