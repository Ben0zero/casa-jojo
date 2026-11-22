package cl.duoc.casajojo.domain.usecase

import cl.duoc.casajojo.domain.model.PlatoDraft

/** Errores por campo del formulario de Retiro. null = el campo está bien. */
data class RetiroErrores(
    val nombre: String? = null,
) {
    val hayErrores: Boolean get() = nombre != null
}

/**
 * Reglas de negocio del pedido, separadas de la UI para poder probarlas
 * con tests unitarios (ver ReglasPedidoTest).
 * Cada validación devuelve el mensaje de error, o null si todo está bien.
 */
object ReglasPedido {
    const val PRECIO_SOLO_PROTEINA = 3000

    fun precioProteina(precioProteina: Int, solo: Boolean): Int =
        if (solo) PRECIO_SOLO_PROTEINA else precioProteina

    /** Un plato normal necesita al menos un acompañamiento; "solo proteína" no. */
    fun validarAcompanamientos(solo: Boolean, acompanamientos: List<String>): String? =
        if (!solo && acompanamientos.isEmpty()) {
            "Elige al menos un acompañamiento"
        } else {
            null
        }

    /** NUEVO: un pedido debe tener al menos un plato para poder confirmarse. */
    fun validarCarrito(platos: List<PlatoDraft>): String? =
        if (platos.isEmpty()) "Agrega al menos un plato a tu pedido" else null

    /** NUEVO: el nombre es obligatorio porque es con lo que se entrega el pedido. */
    fun validarNombre(nombre: String): String? =
        if (nombre.isBlank()) "Escribe tu nombre para el retiro" else null

    /** Revisa todos los campos del Retiro de una vez (validación por campo). */
    fun validarRetiro(nombre: String): RetiroErrores =
        RetiroErrores(nombre = validarNombre(nombre))

    fun total(platos: List<PlatoDraft>): Int = platos.sumOf { it.precio }

    // ---------- RF-13: cancelar pedido dentro de 15 segundos ----------

    const val PLAZO_CANCELACION_MS = 15_000L

    /** Solo se puede cancelar si sigue "recibido" y no pasaron 15 s desde su fecha. */
    fun puedeCancelar(fecha: Long, ahora: Long, estadoPedido: String): Boolean =
        estadoPedido == "recibido" && ahora - fecha <= PLAZO_CANCELACION_MS

    /** Milisegundos restantes, sin valores negativos. */
    fun msParaCancelar(fecha: Long, ahora: Long): Long =
        (PLAZO_CANCELACION_MS - (ahora - fecha)).coerceIn(0L, PLAZO_CANCELACION_MS)

    /** Fecha mínima para seguir siendo cancelable "ahora" (la usa el UPDATE de Room). */
    fun limiteCancelacion(ahora: Long): Long = ahora - PLAZO_CANCELACION_MS

    // ---------- RF-14: retiro con QR e historial ----------

    /** Un pedido cancelado o ya entregado no puede avanzar con el QR. */
    fun puedeRetirarConQr(estadoPedido: String): Boolean =
        estadoPedido != "cancelado" && estadoPedido != "entregado"
}
