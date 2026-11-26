package cl.duoc.casajojo.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Perfil estilo Uber: secciones con un título pequeño y una tarjeta que agrupa filas.
 * Son piezas visuales "tontas": solo dibujan y avisan el toque con onClick.
 */

/** Título de sección ("Cuenta", "Pedidos"…) + tarjeta con sus filas separadas por una línea. */
@Composable
fun SeccionOpciones(titulo: String, filas: List<@Composable () -> Unit>) {
    Text(
        titulo.uppercase(),
        fontSize = 12.sp,
        fontWeight = FontWeight.ExtraBold,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(start = 4.dp, top = 22.dp, bottom = 8.dp),
    )
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 2.dp,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column {
            filas.forEachIndexed { i, fila ->
                if (i > 0) {
                    HorizontalDivider(
                        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                        modifier = Modifier.padding(start = 66.dp),
                    )
                }
                fila()
            }
        }
    }
}

/**
 * Fila de opción: ícono en un círculo, título, subtítulo corto y flecha.
 * Alto mínimo de 56 dp (≥ 48 dp de área táctil) y rol de botón para TalkBack.
 */
@Composable
fun FilaOpcion(
    icono: String,
    titulo: String,
    subtitulo: String,
    onClick: () -> Unit,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(role = Role.Button, onClick = onClick)
            .defaultMinSize(minHeight = 56.dp)
            .padding(horizontal = 14.dp, vertical = 10.dp),
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(38.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.primaryContainer),
        ) {
            Text(icono, fontSize = 18.sp)
        }
        Column(Modifier.weight(1f)) {
            Text(titulo, fontWeight = FontWeight.Bold, fontSize = 15.sp)
            Text(
                subtitulo,
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Spacer(Modifier.width(4.dp))
        Text("›", fontSize = 24.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

/** Tarjeta blanca con sombra para bloques de contenido (formularios, textos). */
@Composable
fun TarjetaBloque(modifier: Modifier = Modifier, contenido: @Composable () -> Unit) {
    Surface(
        shape = RoundedCornerShape(22.dp),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 3.dp,
        modifier = modifier.fillMaxWidth().padding(top = 18.dp),
    ) {
        Column(Modifier.padding(horizontal = 18.dp, vertical = 16.dp)) { contenido() }
    }
}
