package cl.duoc.casajojo.domain

/**
 * Plato "en borrador": lo que el cliente va armando antes de confirmar.
 * Vive solo en memoria (en el carrito del ViewModel). Recién al confirmar
 * se convierte en una entidad Plato de Room.
 */
data class PlatoDraft(
    val nombreProteina: String,
    val precio: Int,
    val solo: Boolean,
    val acompanamientos: List<String> = emptyList(),
)

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

    fun total(platos: List<PlatoDraft>): Int = platos.sumOf { it.precio }
}