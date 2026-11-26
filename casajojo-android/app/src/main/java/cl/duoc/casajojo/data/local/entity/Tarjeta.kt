package cl.duoc.casajojo.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Billetera (SIMULADA): tarjeta guardada de un usuario.
 *
 * SEGURIDAD: a propósito NO existen campos para el número completo ni para el CVV.
 * Solo se guarda lo necesario para reconocer la tarjeta ("Visa •••• 4242"), igual que
 * hacen las apps reales, que dejan los datos sensibles en la pasarela de pago.
 * El índice por usuarioId hace rápida la consulta "tarjetas de esta cuenta".
 */
@Entity(
    tableName = "tarjetas",
    indices = [Index("usuarioId")],
)
data class Tarjeta(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val usuarioId: Long,
    /** Etiqueta de MarcaTarjeta: "Visa", "Mastercard", "American Express" u "Otra". */
    val marca: String,
    val ultimos4: String,
    val titular: String,
    val mesVencimiento: Int,
    /** Año completo (2029), no "29". */
    val anioVencimiento: Int,
    val alias: String = "",
    val predeterminada: Boolean = false,
) {
    /** "Visa •••• 4242": la forma en que la tarjeta se muestra en toda la app. */
    val etiqueta: String get() = "$marca •••• $ultimos4"

    /** "12/29" */
    val vencimiento: String get() = "%02d/%02d".format(mesVencimiento, anioVencimiento % 100)
}
