package cl.duoc.casajojo.domain.model

/**
 * EFT (POO): estado del pedido como jerarquía sellada.
 *
 * - Herencia: cada estado es una subclase (object) de [EstadoPedido].
 * - Polimorfismo: [etiqueta], [color], [siguiente], [puedeCancelar], [puedeEditar] y
 *   [puedeRetirarConQr] se resuelven según el objeto concreto, sin "when" repartidos
 *   por la app.
 * - Encapsulamiento: Room y ms-ventas siguen guardando el texto ([codigo] / [remoto]);
 *   la conversión vive solo aquí ([desde] y [desdeRemoto]).
 *
 *     RECIBIDO → EN_PREPARACION → LISTO → ENTREGADO
 *         └──────────────→ CANCELADO (solo dentro de 15 s)
 */
sealed class EstadoPedido(
    /** Valor guardado en Room ("recibido", "preparacion"...). */
    val codigo: String,
    /** Valor que usa ms-ventas ("RECIBIDO", "EN_PREPARACION"...). */
    val remoto: String,
    /** Posición en la línea de estado de la UI (0..3); -1 si no aplica. */
    val paso: Int,
) {
    /** Texto para mostrar al cliente. */
    abstract fun etiqueta(): String

    /** Color ARGB de la píldora del estado (la UI lo envuelve en Color). */
    abstract fun color(): Long

    /** Estado al que avanza la cocina; null si es final. */
    abstract fun siguiente(): EstadoPedido?

    /** Por defecto un estado no se puede cancelar; Recibido lo sobrescribe. */
    open fun puedeCancelar(fecha: Long, ahora: Long): Boolean = false

    /** RF-12: por defecto no se puede editar; Recibido lo sobrescribe. */
    open fun puedeEditar(): Boolean = false

    /** RF-14: por defecto no se retira con QR; Listo lo sobrescribe. */
    open fun puedeRetirarConQr(): Boolean = false

    /** true si el pedido ya terminó (no cambia más). */
    val esFinal: Boolean get() = siguiente() == null

    data object Recibido : EstadoPedido("recibido", "RECIBIDO", 0) {
        override fun etiqueta() = "Recibido"
        override fun color() = 0xFF6D6D67
        override fun siguiente() = EnPreparacion
        override fun puedeCancelar(fecha: Long, ahora: Long) = ahora - fecha <= PLAZO_CANCELACION_MS
        override fun puedeEditar() = true
    }

    data object EnPreparacion : EstadoPedido("preparacion", "EN_PREPARACION", 1) {
        override fun etiqueta() = "En preparación"
        override fun color() = 0xFFB7791F
        override fun siguiente() = Listo
    }

    data object Listo : EstadoPedido("listo", "LISTO", 2) {
        override fun etiqueta() = "Listo"
        override fun color() = 0xFF2B6CB0
        override fun siguiente() = Entregado
        override fun puedeRetirarConQr() = true
    }

    data object Entregado : EstadoPedido("entregado", "ENTREGADO", 3) {
        override fun etiqueta() = "Entregado"
        override fun color() = 0xFF2F855A
        override fun siguiente(): EstadoPedido? = null
    }

    data object Cancelado : EstadoPedido("cancelado", "CANCELADO", -1) {
        override fun etiqueta() = "Cancelado"
        override fun color() = 0xFFC53030
        override fun siguiente(): EstadoPedido? = null
    }

    companion object {
        /** RF-13: 15 s para cancelar desde que el pedido quedó registrado. */
        const val PLAZO_CANCELACION_MS = 15_000L

        val todos: List<EstadoPedido> by lazy { listOf(Recibido, EnPreparacion, Listo, Entregado, Cancelado) }

        /** Estado desde el código de Room; null si el texto no es conocido. */
        fun desdeOrNull(codigo: String?): EstadoPedido? = todos.firstOrNull { it.codigo == codigo }

        /** Estado desde el código de Room; un texto desconocido se trata como Recibido. */
        fun desde(codigo: String?): EstadoPedido = desdeOrNull(codigo) ?: Recibido

        /** Estado desde ms-ventas (RECIBIDO, EN_PREPARACION...); desconocido → Recibido. */
        fun desdeRemoto(remoto: String?): EstadoPedido =
            todos.firstOrNull { it.remoto.equals(remoto, ignoreCase = true) } ?: Recibido
    }
}
