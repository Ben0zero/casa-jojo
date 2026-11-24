package cl.duoc.casajojo.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * EFT: mensaje visual en línea (éxito verde ✅ o error rojo ❌). Complementa al
 * Snackbar: queda a la vista junto al formulario hasta que se cierra o cambia.
 * Si [onReintentar] no es null, muestra el botón "Reintentar" (errores de red).
 */
@Composable
fun Aviso(
    texto: String?,
    esError: Boolean,
    modifier: Modifier = Modifier,
    onReintentar: (() -> Unit)? = null,
) {
    AnimatedVisibility(
        visible = texto != null,
        enter = fadeIn() + expandVertically(),
        exit = fadeOut() + shrinkVertically(),
    ) {
        // Colores del tema: en oscuro el aviso de éxito ya no es un bloque claro encandilante.
        val fondo = if (esError) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.primaryContainer
        val color = if (esError) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onPrimaryContainer
        Surface(
            shape = RoundedCornerShape(14.dp),
            color = fondo,
            modifier = modifier.fillMaxWidth().padding(top = 12.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            ) {
                Text(if (esError) "❌" else "✅", fontSize = 16.sp)
                Spacer(Modifier.width(10.dp))
                Text(
                    texto.orEmpty(),
                    color = color,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f),
                )
                if (onReintentar != null) {
                    TextButton(onClick = onReintentar) { Text("Reintentar") }
                }
            }
        }
    }
}
