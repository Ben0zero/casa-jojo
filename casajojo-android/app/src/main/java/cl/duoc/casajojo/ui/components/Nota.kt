package cl.duoc.casajojo.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** Línea corta de color bajo un título, como separador visual. */
@Composable
fun Regla() {
    Box(
        modifier = Modifier
            .padding(top = 12.dp, bottom = 18.dp)
            .background(
                MaterialTheme.colorScheme.primary,
                RoundedCornerShape(2.dp),
            )
            .height(3.dp)
            .fillMaxWidth(0.35f),
    )
}

/** Recuadro de información o advertencia, con una barra de color a la izquierda. */
@Composable
fun Nota(texto: String) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(topStart = 0.dp, topEnd = 10.dp, bottomEnd = 10.dp, bottomStart = 0.dp),
        modifier = Modifier.padding(top = 20.dp),
    ) {
        Row {
            Box(
                modifier = Modifier
                    .width(3.dp)
                    .background(MaterialTheme.colorScheme.primary),
            )
            Text(
                texto,
                modifier = Modifier.weight(1f).padding(horizontal = 16.dp, vertical = 14.dp),
                fontSize = 15.sp,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
    }
}
