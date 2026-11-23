package cl.duoc.casajojo.domain.usecase

import cl.duoc.casajojo.domain.model.EstadoPedido
import cl.duoc.casajojo.domain.model.PlatoDraft
import java.time.LocalTime

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

    const val PLAZO_CANCELACION_MS = EstadoPedido.PLAZO_CANCELACION_MS

    /**
     * Solo se puede cancelar si sigue "recibido" y no pasaron 15 s desde su fecha.
     * EFT (POO): la regla la decide cada EstadoPedido (polimorfismo).
     */
    fun puedeCancelar(fecha: Long, ahora: Long, estadoPedido: String): Boolean =
        EstadoPedido.desdeOrNull(estadoPedido)?.puedeCancelar(fecha, ahora) ?: false

    /** Milisegundos restantes, sin valores negativos. */
    fun msParaCancelar(fecha: Long, ahora: Long): Long =
        (PLAZO_CANCELACION_MS - (ahora - fecha)).coerceIn(0L, PLAZO_CANCELACION_MS)

    /** Fecha mínima para seguir siendo cancelable "ahora" (la usa el UPDATE de Room). */
    fun limiteCancelacion(ahora: Long): Long = ahora - PLAZO_CANCELACION_MS

    // ---------- B7: hora de retiro ----------

    /** Horarios de retiro que ofrece el local (los mismos de la pantalla Retiro). */
    val HORAS_RETIRO = listOf("12:00", "12:30", "13:00", "13:30", "14:00", "14:30", "15:00")

    /** Solo se ofrecen horas que todavía no pasan (el pedido es para hoy). */
    fun horasDisponibles(ahora: LocalTime, horas: List<String> = HORAS_RETIRO): List<String> =
        horas.filter { LocalTime.parse(it).isAfter(ahora) }

    /** null si la hora sirve; si no, el mensaje a mostrar. */
    fun validarHoraRetiro(hora: String, ahora: LocalTime): String? = when {
        horasDisponibles(ahora).isEmpty() -> "Por hoy ya no quedan horarios de retiro"
        hora !in horasDisponibles(ahora) -> "Elige una hora de retiro que aún no haya pasado"
        else -> null
    }

    // ---------- RF-14: retiro con QR e historial ----------

    /**
     * Fase 2 (M2): el retiro con QR solo procede cuando la cocina marcó el pedido
     * como "listo"; es la misma regla que aplica ms-ventas (LISTO → ENTREGADO).
     */
    fun puedeRetirarConQr(estadoPedido: String): Boolean =
        EstadoPedido.desdeOrNull(estadoPedido)?.puedeRetirarConQr() ?: false

    // ---------- EFT / RF-12: editar el pedido ----------

    /** El cliente puede editar los datos del retiro mientras la cocina no empiece (RECIBIDO). */
    fun puedeEditar(estadoPedido: String): Boolean =
        EstadoPedido.desdeOrNull(estadoPedido)?.puedeEditar() ?: false

    /** null si se puede guardar la edición; si no, el mensaje a mostrar. */
    fun validarEdicion(estadoPedido: String, nombre: String): String? = when {
        !puedeEditar(estadoPedido) -> "Tu pedido ya está en preparación y no se puede editar"
        else -> validarNombre(nombre)
    }
}
