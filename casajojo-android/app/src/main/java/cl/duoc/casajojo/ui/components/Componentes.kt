package cl.duoc.casajojo.ui.components

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun BarraSuperior(
    cartCount: Int,
    onCartClick: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("La Casa ", fontWeight = FontWeight.Bold, fontSize = 18.sp)
            Text("JoJo", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = MaterialTheme.colorScheme.primary)
        }
        if (cartCount > 0) {
            Surface(
                onClick = onCartClick,
                shape = RoundedCornerShape(99.dp),
                border = androidx.compose.foundation.BorderStroke(
                    1.5.dp, MaterialTheme.colorScheme.outline
                ),
                color = Color.Transparent,
            ) {
                Text(
                    "Mi pedido ($cartCount)",
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp),
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp,
                )
            }
        }
    }
}

@Composable
fun PasoBar(paso: Int) {
    val pasos = listOf("Proteína", "Acompañamientos", "Retiro y pago")
    Row(modifier = Modifier.fillMaxWidth().padding(bottom = 22.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        pasos.forEachIndexed { i, text ->
            val activo = paso >= i + 1
            Column(
                modifier = Modifier.weight(1f).padding(top = 8.dp).clip(
                    RoundedCornerShape(2.dp)
                ).background(
                    if (activo) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
                )
            ) {
                Text(
                    text,
                    color = if (activo) MaterialTheme.colorScheme.onBackground else MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(top = 4.dp),
                )
            }
        }
    }
}

@Composable
fun Marcador(
    estilo: String,
    marcado: Boolean,
) {
    val primario = MaterialTheme.colorScheme.primary
    val contorno = MaterialTheme.colorScheme.outline
    Box(
        modifier = Modifier
            .size(22.dp)
            .clip(if (estilo == "radio") CircleShape else RoundedCornerShape(6.dp))
            .border(2.dp, if (marcado) primario else contorno, if (estilo == "radio") CircleShape else RoundedCornerShape(6.dp))
            .background(if (marcado) primario else Color.Transparent),
        contentAlignment = Alignment.Center,
    ) {
        when {
            estilo == "radio" && marcado -> Box(
                Modifier.size(8.dp).clip(CircleShape).background(Color.White)
            )
            estilo == "check" && marcado -> Text("✓", color = Color.White, fontSize = 14.sp, lineHeight = 14.sp)
        }
    }
}

@Composable
fun OptionCard(
    texto: String,
    marcado: Boolean,
    onClick: () -> Unit,
    estilo: String = "radio",
    detalle: String? = null,
    precio: String? = null,
    deshabilitado: Boolean = false,
    modifier: Modifier = Modifier,
) {
    val primario = MaterialTheme.colorScheme.primary
    Surface(
        onClick = onClick,
        enabled = !deshabilitado,
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surface,
        border = androidx.compose.foundation.BorderStroke(
            1.5.dp, if (marcado) primario else MaterialTheme.colorScheme.outline
        ),
        modifier = modifier.fillMaxWidth().padding(top = 10.dp).alpha(if (deshabilitado) 0.5f else 1f),
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Marcador(estilo, marcado)
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    texto,
                    fontWeight = FontWeight.SemiBold,
                    textDecoration = if (deshabilitado) androidx.compose.ui.text.style.TextDecoration.LineThrough else null,
                )
                if (detalle != null) {
                    Text(detalle, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 15.sp)
                }
            }
            if (precio != null) {
                Text(precio, color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@Composable
fun Pill(
    texto: String,
    color: Color,
) {
    Surface(
        shape = RoundedCornerShape(99.dp),
        color = color,
        modifier = Modifier.padding(end = 8.dp),
    ) {
        Text(
            texto,
            color = Color.White,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp),
        )
    }
}

@Composable
fun Boton(
    texto: String,
    onClick: () -> Unit,
    estilo: String = "acento",
    enabled: Boolean = true,
) {
    val colorFondo: Color
    val colorTexto: Color
    when (estilo) {
        "acento" -> {
            colorFondo = MaterialTheme.colorScheme.primary
            colorTexto = MaterialTheme.colorScheme.onPrimary
        }
        "oscuro" -> {
            colorFondo = MaterialTheme.colorScheme.onBackground
            colorTexto = MaterialTheme.colorScheme.background
        }
        else -> {
            colorFondo = Color.Transparent
            colorTexto = MaterialTheme.colorScheme.onBackground
        }
    }
    val borde = if (estilo == "fantasma") {
        androidx.compose.foundation.BorderStroke(1.5.dp, MaterialTheme.colorScheme.outline)
    } else null
    Surface(
        onClick = onClick,
        enabled = enabled,
        shape = RoundedCornerShape(12.dp),
        color = colorFondo,
        border = borde,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.padding(vertical = 16.dp).fillMaxWidth(),
        ) {
            Text(texto, color = colorTexto, fontWeight = FontWeight.SemiBold, fontSize = 17.sp)
        }
    }
}

@Composable
fun Volver(onClick: () -> Unit) {
    Text(
        "‹ Volver",
        modifier = Modifier.padding(bottom = 12.dp),
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        fontWeight = FontWeight.SemiBold,
        fontSize = 15.sp,
    )
}

@Composable
fun Titulo(
    titulo: String,
    subtitulo: String? = null,
) {
    Column {
        Text(titulo, fontSize = 24.sp, fontWeight = FontWeight.Bold)
        if (subtitulo != null) {
            Text(subtitulo, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 15.sp)
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ChipRow(
    opciones: List<String>,
    seleccion: String,
    onSeleccionar: (String) -> Unit,
) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        opciones.forEach { op ->
            Surface(
                onClick = { onSeleccionar(op) },
                shape = RoundedCornerShape(10.dp),
                color = if (seleccion == op) MaterialTheme.colorScheme.onBackground else MaterialTheme.colorScheme.surface,
                border = androidx.compose.foundation.BorderStroke(
                    1.5.dp, MaterialTheme.colorScheme.outline
                ),
            ) {
                Text(
                    op,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 11.dp),
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 15.sp,
                    color = if (seleccion == op) MaterialTheme.colorScheme.background else MaterialTheme.colorScheme.onSurface,
                )
            }
        }
    }
}

@Composable
fun ErrorEcho(
    error: String?,
    onMostrado: () -> Unit,
) {
    val context = LocalContext.current
    LaunchedEffect(error) {
        if (error != null) {
            Toast.makeText(context, error, Toast.LENGTH_LONG).show()
            onMostrado()
        }
    }
}