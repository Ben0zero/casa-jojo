package cl.duoc.casajojo.domain.model

/**
 * EFT (POO): forma de pago como jerarquía sellada.
 *
 * Cada subclase sabe cómo se describe y cuándo se paga ([pagaAlRetirar]); la UI y las
 * reglas preguntan al objeto en vez de comparar textos como "Pagar en tienda".
 * Room y ms-ventas guardan [etiqueta] (el mismo texto de siempre).
 */
sealed class FormaPago(val etiqueta: String) {

    /** true si el cliente paga en el local al retirar (con el QR de la tienda). */
    abstract fun pagaAlRetirar(): Boolean

    /** Ayuda corta que se muestra al elegirla. */
    abstract fun descripcion(): String

    data object Transferencia : FormaPago("Transferencia") {
        override fun pagaAlRetirar() = false
        override fun descripcion() = "Transfiere y muestra el comprobante al retirar"
    }

    data object PagarEnTienda : FormaPago("Pagar en tienda") {
        override fun pagaAlRetirar() = true
        override fun descripcion() = "Pagas en el local al escanear el QR de retiro"
    }

    companion object {
        val todas: List<FormaPago> by lazy { listOf(Transferencia, PagarEnTienda) }

        /** Etiquetas en el orden en que se muestran los chips. */
        val etiquetas: List<String> get() = todas.map { it.etiqueta }

        /** Forma de pago desde el texto guardado; desconocido → Transferencia (la de siempre). */
        fun desde(etiqueta: String?): FormaPago = todas.firstOrNull { it.etiqueta == etiqueta } ?: Transferencia
    }
}
