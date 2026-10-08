package cl.duoc.casajojo.domain

data class PlatoDraft(
    val nombreProteina: String,
    val precio: Int,
    val solo: Boolean,
    val acompanamientos: List<String> = emptyList(),
)

object ReglasPedido {
    const val PRECIO_SOLO_PROTEINA = 3000

    fun precioProteina(precioProteina: Int, solo: Boolean): Int =
        if (solo) PRECIO_SOLO_PROTEINA else precioProteina

    fun validarAcompanamientos(solo: Boolean, acompanamientos: List<String>): String? =
        if (!solo && acompanamientos.isEmpty()) {
            "Elige al menos un acompañamiento"
        } else {
            null
        }

    fun total(platos: List<PlatoDraft>): Int = platos.sumOf { it.precio }
}