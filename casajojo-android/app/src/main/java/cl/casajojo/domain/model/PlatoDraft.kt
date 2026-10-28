package cl.duoc.casajojo.domain.model

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
