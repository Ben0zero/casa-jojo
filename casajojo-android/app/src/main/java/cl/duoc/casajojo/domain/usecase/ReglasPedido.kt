package cl.duoc.casajojo.domain.usecase

import cl.duoc.casajojo.domain.model.PlatoDraft

/**
 * NUEVO (P1): errores por campo del formulario de Retiro, igual que
 * RegistroErrores. null = el campo está bien; un texto = el aviso bajo el campo.
 */
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

    /** NUEVO (P1): revisa todos los campos del formulario de Retiro de una vez. */
    fun validarRetiro(nombre: String): RetiroErrores =
        RetiroErrores(nombre = validarNombre(nombre))

    fun total(platos: List<PlatoDraft>): Int = platos.sumOf { it.precio }

    // ---------- RF-13: cancelar pedido ----------

    /** Plazo para cancelar, contado desde Pedido.fecha (15 segundos). */
    const val PLAZO_CANCELACION_MS = 15_000L

    /**
     * NUEVO (RF-13): un pedido se puede cancelar solo si sigue "recibido" y no
     * han pasado más de 15 s desde su fecha. El límite es inclusivo (justo a los
     * 15 s todavía se puede), igual que el UPDATE del DAO (fecha >= limite).
     * Se calcula con la fecha guardada en Room, no con un temporizador en memoria,
     * así que funciona aunque se salga de la pantalla o se gire el teléfono.
     */
    fun puedeCancelar(fecha: Long, ahora: Long, estadoPedido: String): Boolean =
        estadoPedido == "recibido" && ahora - fecha <= PLAZO_CANCELACION_MS

    /** Milisegundos que quedan para cancelar (0 si ya pasó el plazo). */
    fun msParaCancelar(fecha: Long, ahora: Long): Long =
        (PLAZO_CANCELACION_MS - (ahora - fecha)).coerceIn(0L, PLAZO_CANCELACION_MS)

    /** Fecha mínima que debe tener un pedido para seguir siendo cancelable "ahora". */
    fun limiteCancelacion(ahora: Long): Long = ahora - PLAZO_CANCELACION_MS

    // ---------- RF-14: retiro e historial ----------

    /**
     * NUEVO (RF-14): solo los pedidos activos se retiran con QR. Un pedido
     * cancelado (o ya entregado) no puede avanzar más.
     */
    fun puedeRetirarConQr(estadoPedido: String): Boolean =
        estadoPedido != "cancelado" && estadoPedido != "entregado"
}
