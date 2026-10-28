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
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cl.duoc.casajojo.utils.money

/**
 * Indicador de progreso del armado: Proteína → Acompañamientos → Retiro y pago.
 * "paso" es 1, 2 o 3; los pasos alcanzados se pintan con el color de acento.
 */
@Composable
fun PasoBar(paso: Int) {
    val pasos = listOf("Proteína", "Acompañamientos", "Retiro y pago")
    Row(modifier = Modifier.fillMaxWidth().padding(bottom = 22.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        pasos.forEachIndexed { i, text ->
            val activo = paso >= i + 1
            // ANIMACIÓN: la barra de progreso cambia de gris a naranjo de forma
            // gradual al avanzar de paso, en vez de cambiar de golpe.
            val colorBarra by animateColorAsState(
                targetValue = if (activo) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                animationSpec = tween(320),
                label = "colorPaso$i",
            )
            // CORRECCIÓN VISUAL: antes el color se aplicaba como fondo de toda la
            // columna, así que el texto quedaba encima de un bloque naranjo.
            // Ahora cada paso es una línea delgada de color y, debajo, su nombre.
            Column(modifier = Modifier.weight(1f).padding(top = 8.dp)) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(4.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(colorBarra),
                )
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

/**
 * CAMBIO (P3): los estilos eran textos ("radio", "acento"…); un error de tipeo
 * compilaba y caía en el estilo por defecto sin aviso. Con enums el compilador
 * solo acepta los valores que existen.
 */
enum class EstiloMarcador { Radio, Check }

/** Estilos de Boton: Acento (naranjo), Oscuro y Fantasma (solo borde). */
enum class EstiloBoton { Acento, Oscuro, Fantasma }

/** Círculo (radio) o cuadrado (check) que indica si una opción está marcada. */
@Composable
fun Marcador(
    estilo: EstiloMarcador,
    marcado: Boolean,
) {
    val primario = MaterialTheme.colorScheme.primary
    val contorno = MaterialTheme.colorScheme.outline
    val forma = if (estilo == EstiloMarcador.Radio) CircleShape else RoundedCornerShape(6.dp)
    Box(
        modifier = Modifier
            .size(22.dp)
            .clip(forma)
            .border(2.dp, if (marcado) primario else contorno, forma)
            .background(if (marcado) primario else Color.Transparent),
        contentAlignment = Alignment.Center,
    ) {
        when {
            estilo == EstiloMarcador.Radio && marcado -> Box(
                Modifier.size(8.dp).clip(CircleShape).background(Color.White)
            )
            estilo == EstiloMarcador.Check && marcado -> Text("✓", color = Color.White, fontSize = 14.sp, lineHeight = 14.sp)
        }
    }
}

/**
 * Tarjeta seleccionable para proteínas y acompañamientos.
 * Si "deshabilitado" es true (producto agotado) no responde al toque,
 * se ve transparente y el nombre aparece tachado.
 */
@Composable
fun OptionCard(
    texto: String,
    marcado: Boolean,
    onClick: () -> Unit,
    estilo: EstiloMarcador = EstiloMarcador.Radio,
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
            Column(modifier = Modifier.weight(1f).padding(start = 12.dp)) {
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

/** Etiqueta redondeada de color, usada para el estado del pedido y del pago. */
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

/**
 * Botón de ancho completo (ver EstiloBoton).
 * Con enabled = false el botón no responde y se ve atenuado.
 * icono (opcional) se dibuja a la izquierda del texto.
 */
@Composable
fun Boton(
    texto: String,
    onClick: () -> Unit,
    estilo: EstiloBoton = EstiloBoton.Acento,
    enabled: Boolean = true,
    icono: ImageVector? = null,
) {
    val colorFondo: Color
    val colorTexto: Color
    when (estilo) {
        EstiloBoton.Acento -> {
            colorFondo = MaterialTheme.colorScheme.secondary
            colorTexto = MaterialTheme.colorScheme.onPrimary
        }
        EstiloBoton.Oscuro -> {
            colorFondo = MaterialTheme.colorScheme.onBackground
            colorTexto = MaterialTheme.colorScheme.background
        }
        EstiloBoton.Fantasma -> {
            colorFondo = Color.Transparent
            colorTexto = MaterialTheme.colorScheme.onBackground
        }
    }
    val borde = if (estilo == EstiloBoton.Fantasma) {
        androidx.compose.foundation.BorderStroke(1.5.dp, MaterialTheme.colorScheme.outline)
    } else null
    Surface(
        onClick = onClick,
        enabled = enabled,
        shape = RoundedCornerShape(12.dp),
        color = colorFondo,
        border = borde,
        // Atenuamos el botón deshabilitado para que el usuario note que no puede avanzar.
        modifier = Modifier.fillMaxWidth().alpha(if (enabled) 1f else 0.45f),
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.padding(vertical = 16.dp).fillMaxWidth(),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (icono != null) {
                    Icon(icono, contentDescription = null, tint = colorTexto, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(8.dp))
                }
                Text(texto, color = colorTexto, fontWeight = FontWeight.SemiBold, fontSize = 17.sp)
            }
        }
    }
}

/** Enlace "‹ Volver" que aparece arriba de cada pantalla del flujo. */
@Composable
fun Volver(onClick: () -> Unit) {
    // CORRECCIÓN: el parámetro onClick se recibía pero nunca se usaba, así que
    // tocar "Volver" no hacía nada (solo funcionaba el botón atrás del teléfono).
    // Modifier.clickable conecta el toque con la acción de navegación que
    // entrega App.kt (normalmente nav.popBackStack()).
    Text(
        "‹ Volver",
        modifier = Modifier
            .padding(top = 8.dp, bottom = 12.dp)
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 6.dp, horizontal = 4.dp), // área de toque más cómoda
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

/**
 * Muestra un Toast cada vez que el ViewModel publica un error y luego lo limpia,
 * para que no se repita al volver a dibujar la pantalla.
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

/**
 * Total del pedido con contador animado: al agregar o quitar platos el número
 * se mueve hasta el nuevo valor en ~400 ms, en vez de cambiar de golpe.
 */
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