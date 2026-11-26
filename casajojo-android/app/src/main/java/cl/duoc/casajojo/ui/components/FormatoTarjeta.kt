package cl.duoc.casajojo.ui.components

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation

/**
 * Billetera: formato SOLO visual de los campos de tarjeta.
 *
 * El ViewModel guarda únicamente los dígitos ("4242424242424242"); estas
 * transformaciones agregan los espacios o la barra al dibujar y traducen la posición
 * del cursor (OffsetMapping). Si el formato se metiera en el texto, el cursor quedaría
 * desfasado y los dígitos se desordenarían al escribir.
 */

/** "4242424242424242" → "4242 4242 4242 4242" (un espacio cada 4 dígitos). */
object FormatoNumeroTarjeta : VisualTransformation {
    override fun filter(text: AnnotatedString): TransformedText {
        val d = text.text
        val formateado = d.chunked(4).joinToString(" ")
        val mapeo = object : OffsetMapping {
            // Cada 4 dígitos escritos se suma un espacio delante.
            override fun originalToTransformed(offset: Int): Int =
                offset + (if (offset > 0) (offset - 1) / 4 else 0)

            override fun transformedToOriginal(offset: Int): Int =
                (offset - offset / 5).coerceIn(0, d.length)
        }
        return TransformedText(AnnotatedString(formateado), mapeo)
    }
}

/** "1229" → "12/29" (barra después del mes). */
object FormatoVencimiento : VisualTransformation {
    override fun filter(text: AnnotatedString): TransformedText {
        val d = text.text
        val formateado = if (d.length <= 2) d else d.take(2) + "/" + d.drop(2)
        val mapeo = object : OffsetMapping {
            override fun originalToTransformed(offset: Int): Int = if (offset <= 2 || d.length <= 2) offset else offset + 1

            override fun transformedToOriginal(offset: Int): Int =
                (if (offset <= 2) offset else offset - 1).coerceIn(0, d.length)
        }
        return TransformedText(AnnotatedString(formateado), mapeo)
    }
}
