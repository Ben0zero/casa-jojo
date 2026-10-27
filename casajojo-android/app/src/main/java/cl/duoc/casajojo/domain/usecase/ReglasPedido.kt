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
}
