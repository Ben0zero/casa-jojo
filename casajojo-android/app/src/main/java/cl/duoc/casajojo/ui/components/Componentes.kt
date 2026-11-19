package cl.duoc.casajojo.ui.components

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cl.duoc.casajojo.ui.theme.Ac
import cl.duoc.casajojo.utils.money

/**
 * Indicador de progreso del armado: Proteína → Acompañamientos → Retiro y pago.
 * Ahora son círculos numerados unidos por una línea que se prende en rojo.
 */
@Composable
fun PasoBar(paso: Int) {
    val pasos = listOf("Proteína", "Acompañamientos", "Retiro y pago")
    Row(
        modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        pasos.forEachIndexed { i, texto ->
            val activo = paso >= i + 1
            val colorBarra by animateColorAsState(
                targetValue = if (activo) Ac else MaterialTheme.colorScheme.outline,
                animationSpec = tween(320),
                label = "colorPaso$i",
            )
            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    if (i > 0) {
                        Box(Modifier.height(3.dp).weight(1f).background(colorBarra))
                    }
                    Box(
                        modifier = Modifier
                            .size(26.dp)
                            .clip(CircleShape)
                            .background(colorBarra),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            if (activo) "✓" else "${i + 1}",
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                    if (i < pasos.lastIndex) {
                        Box(
                            Modifier
                                .height(3.dp)
                                .weight(1f)
                                .background(
                                    if (paso > i + 1) Ac else MaterialTheme.colorScheme.outline,
                                ),
                        )
                    }
                }
                Text(
                    texto,
                    color = if (activo) MaterialTheme.colorScheme.onBackground
                    else MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(top = 6.dp),
                )
            }
        }
    }
}

/** Círculo (radio) o cuadrado (check) que indica si una opción está marcada. */
@Composable
fun Marcador(
    estilo: String,
    marcado: Boolean,
) {
    val forma = if (estilo == "radio") CircleShape else RoundedCornerShape(7.dp)
    Box(
        modifier = Modifier
            .size(24.dp)
            .clip(forma)
            .border(2.dp, if (marcado) Ac else MaterialTheme.colorScheme.outline, forma)
            .background(if (marcado) Ac else Color.Transparent),
        contentAlignment = Alignment.Center,
    ) {
        when {
            estilo == "radio" && marcado -> Box(
                Modifier.size(9.dp).clip(CircleShape).background(Color.White),
            )
            estilo == "check" && marcado -> Text(
                "✓",
                color = Color.White,
                fontSize = 15.sp,
                lineHeight = 15.sp,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}

/**
 * Tarjeta seleccionable. Mantiene la misma firma de siempre; solo cambia el
 * aspecto: esquinas más redondas, sombra y borde rojo cuando está marcada.
 */
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
    Surface(
        onClick = onClick,
        enabled = !deshabilitado,
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = if (marcado) 6.dp else 2.dp,
        border = BorderStroke(
            if (marcado) 2.dp else 1.dp,
            if (marcado) Ac else MaterialTheme.colorScheme.outline,
        ),
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 10.dp)
            .alpha(if (deshabilitado) 0.5f else 1f),
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Marcador(estilo, marcado)
            Column(modifier = Modifier.weight(1f).padding(start = 12.dp)) {
                Text(
                    texto,
                    fontWeight = FontWeight.Bold,
                    textDecoration = if (deshabilitado) {
                        androidx.compose.ui.text.style.TextDecoration.LineThrough
                    } else {
                        null
                    },
                )
                if (detalle != null) {
                    Text(
                        detalle,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 14.sp,
                    )
                }
            }
            if (precio != null) {
                Surface(
                    shape = RoundedCornerShape(99.dp),
                    color = if (deshabilitado) MaterialTheme.colorScheme.outline else Ac,
                ) {
                    Text(
                        precio,
                        color = Color.White,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(horizontal = 11.dp, vertical = 6.dp),
                    )
                }
            }
        }
    }
}

/** Etiqueta redondeada de color, usada para el estado del pedido y del pago. */
@Composable
fun Pill(
    texto: String,
    color: Color,
) {
    Surface(
        shape = RoundedCornerShape(99.dp),
        color = color,
        shadowElevation = 2.dp,
        modifier = Modifier.padding(end = 8.dp),
    ) {
        Text(
            texto,
            color = Color.White,
            fontSize = 12.sp,
            fontWeight = FontWeight.ExtraBold,
            modifier = Modifier.padding(horizontal = 11.dp, vertical = 5.dp),
        )
    }
}

/**
 * Botón de ancho completo. Estilos: "acento" (rojo), "oscuro" y "fantasma".
 * Con enabled = false el botón no responde y se ve atenuado.
 */
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
        BorderStroke(1.5.dp, MaterialTheme.colorScheme.outline)
    } else {
        null
    }
    Surface(
        onClick = onClick,
        enabled = enabled,
        shape = RoundedCornerShape(18.dp),
        color = colorFondo,
        border = borde,
        shadowElevation = if (estilo == "fantasma") 0.dp else 6.dp,
        modifier = Modifier.fillMaxWidth().alpha(if (enabled) 1f else 0.45f),
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.padding(vertical = 17.dp).fillMaxWidth(),
        ) {
            Text(
                texto,
                color = colorTexto,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 16.sp,
            )
        }
    }
}

/** Enlace "‹ Volver" que aparece arriba de cada pantalla del flujo. */
@Composable
fun Volver(onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(99.dp),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 1.dp,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
    ) {
        Text(
            "← Volver",
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = FontWeight.Bold,
            fontSize = 13.sp,
        )
    }
}

@Composable
fun Titulo(
    titulo: String,
    subtitulo: String? = null,
) {
    Column {
        Box(
            Modifier
                .padding(bottom = 10.dp)
                .background(Ac, RoundedCornerShape(3.dp))
                .height(4.dp)
                .width(38.dp),
        )
        Text(
            titulo,
            fontSize = 27.sp,
            fontWeight = FontWeight.Black,
        )
        if (subtitulo != null) {
            Text(
                subtitulo,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 15.sp,
                modifier = Modifier.padding(top = 4.dp),
            )
        }
    }
}

/** Fila de chips de selección única (hora de retiro, forma de pago). */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ChipRow(
    opciones: List<String>,
    seleccion: String,
    onSeleccionar: (String) -> Unit,
) {
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        opciones.forEach { op ->
            val activo = seleccion == op
            Surface(
                onClick = { onSeleccionar(op) },
                shape = RoundedCornerShape(14.dp),
                color = if (activo) Ac else MaterialTheme.colorScheme.surface,
                shadowElevation = if (activo) 4.dp else 1.dp,
                border = BorderStroke(
                    1.5.dp,
                    if (activo) Ac else MaterialTheme.colorScheme.outline,
                ),
            ) {
                Text(
                    op,
                    modifier = Modifier.padding(horizontal = 15.dp, vertical = 11.dp),
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = if (activo) Color.White else MaterialTheme.colorScheme.onSurface,
                )
            }
        }
    }
}

/**
 * Muestra un Toast cada vez que el ViewModel publica un error y luego lo limpia.
 */
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

/** Total del pedido con contador animado. */
@Composable
fun TotalAnimado(
    valor: Int,
    modifier: Modifier = Modifier,
    fontSize: TextUnit = 20.sp,
    fontWeight: FontWeight = FontWeight.Bold,
    color: Color = Color.Unspecified,
) {
    val animado by animateIntAsState(valor, animationSpec = tween(400), label = "total")
    Text(
        animado.money(),
        modifier = modifier,
        fontSize = fontSize,
        fontWeight = fontWeight,
        color = color,
    )
}
