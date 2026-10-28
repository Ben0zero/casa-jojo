package cl.duoc.casajojo.utils

import androidx.compose.ui.graphics.Color
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

fun Int.money(): String =
    "$" + this.toString().reversed().chunked(3).joinToString(".").reversed()

private val FORMATO_FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")

/** Fecha en milisegundos (Pedido.fecha) → "27/10/2026 13:05", en la zona horaria del teléfono. */
fun Long.fechaHora(): String =
    Instant.ofEpochMilli(this).atZone(ZoneId.systemDefault()).format(FORMATO_FECHA)

fun textoEstadoPedido(estado: String): String = when (estado) {
    "recibido" -> "Recibido"
    "preparacion" -> "En preparación"
    "listo" -> "Listo"
    "entregado" -> "Entregado"
    "cancelado" -> "Cancelado"
    else -> estado.replaceFirstChar { it.uppercase() }
}

fun colorEstadoPedido(estado: String): Color = when (estado) {
    "recibido" -> Color(0xFF6D6D67)
    "preparacion" -> Color(0xFFB7791F)
    "listo" -> Color(0xFF2B6CB0)
    "entregado" -> Color(0xFF2F855A)
    "cancelado" -> Color(0xFFC53030)
    else -> Color(0xFF6D6D67)
}

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