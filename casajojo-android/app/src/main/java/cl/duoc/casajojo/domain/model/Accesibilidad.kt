package cl.duoc.casajojo.domain.model

/** Accesibilidad: tema elegido por la persona (Sistema sigue al ajuste del teléfono). */
enum class TemaApp(val etiqueta: String) {
    SISTEMA("Sistema"),
    CLARO("Claro"),
    OSCURO("Oscuro");

    companion object {
        /** Tema desde el texto guardado en DataStore; desconocido → SISTEMA. */
        fun desde(nombre: String?): TemaApp = entries.firstOrNull { it.name == nombre } ?: SISTEMA
    }
}

/**
 * Opciones de accesibilidad de una cuenta. Se guardan en DataStore por usuario y
 * CasaJojoTheme las aplica en toda la app (tema, escala de texto y animaciones).
 */
data class OpcionesAccesibilidad(
    val tema: TemaApp = TemaApp.SISTEMA,
    val textoGrande: Boolean = false,
    val reducirAnimaciones: Boolean = false,
) {
    /** Multiplicador de la tipografía: 1.0 normal, 1.15 grande. */
    val escalaTexto: Float get() = if (textoGrande) ESCALA_GRANDE else 1f

    companion object {
        const val ESCALA_GRANDE = 1.15f
    }
}
