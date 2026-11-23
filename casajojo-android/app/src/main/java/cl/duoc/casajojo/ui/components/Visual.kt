package cl.duoc.casajojo.ui.components

import cl.duoc.casajojo.domain.model.EstadoPedido
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cl.duoc.casajojo.ui.theme.Ac
import cl.duoc.casajojo.ui.theme.FotoFondo
import cl.duoc.casajojo.ui.theme.FotoTexto
import cl.duoc.casajojo.ui.theme.Gold
import cl.duoc.casajojo.ui.theme.Ink
import cl.duoc.casajojo.ui.theme.Line
import cl.duoc.casajojo.ui.theme.Mut
import cl.duoc.casajojo.ui.theme.VerdeOscuro

/**
 * Piezas visuales reutilizables del nuevo diseño.
 *
 * Importante: acá NO hay lógica de negocio. Son composables "tontos" que dibujan
 * tarjetas, badges, espacios para foto y líneas de estado. El espacio de foto
 * queda reservado a propósito: cuando se elijan las imágenes se reemplaza este
 * composable por un Image sin tocar ninguna pantalla.
 */

/** Espacio reservado para una foto. Se reemplaza por Image cuando haya assets. */
@Composable
fun FotoPlaceholder(
    modifier: Modifier = Modifier,
    alto: Dp = 140.dp,
    etiqueta: String = "Foto del plato",
    redondeo: Dp = 16.dp,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(alto)
            .clip(RoundedCornerShape(redondeo))
            .background(Brush.linearGradient(listOf(FotoFondo, Color(0xFFD3EAD8)))),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("📷", fontSize = if (etiqueta.isBlank()) 20.sp else 28.sp)
            if (etiqueta.isNotBlank()) {
                Text(
                    etiqueta,
                    color = FotoTexto,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    "espacio reservado",
                    color = Color(0xB09E6250),
                    fontSize = 10.sp,
                )
            }
        }
    }
}

/** Etiqueta redondeada, por ejemplo "Listo en 10 min" o "★ Popular". */
@Composable
fun Badge(
    texto: String,
    color: Color = Ac,
    colorTexto: Color = Color.White,
) {
    Surface(
        shape = RoundedCornerShape(99.dp),
        color = color,
        shadowElevation = 3.dp,
    ) {
        Text(
            texto,
            color = colorTexto,
            fontSize = 11.sp,
            fontWeight = FontWeight.ExtraBold,
            modifier = Modifier.padding(horizontal = 11.dp, vertical = 6.dp),
        )
    }
}

/** Tarjeta grande de menú con foto, nombre y precio en pastilla. */
@Composable
fun TarjetaMenu(
    nombre: String,
    precio: String,
    disponible: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    destacado: Boolean = false,
) {
    val elevacion = if (destacado) 8.dp else 3.dp
    Surface(
        onClick = onClick,
        enabled = disponible,
        shape = RoundedCornerShape(22.dp),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = elevacion,
        border = if (destacado) BorderStroke(2.dp, Ac) else BorderStroke(1.dp, Line),
        modifier = modifier
            .fillMaxWidth()
            .alpha(if (disponible) 1f else 0.55f),
    ) {
        Column {
            Box(Modifier.padding(8.dp)) {
                FotoPlaceholder(alto = 124.dp, etiqueta = "Foto de $nombre", redondeo = 15.dp)
                if (destacado) {
                    Box(
                        modifier = Modifier.fillMaxSize().padding(8.dp),
                        contentAlignment = Alignment.TopStart,
                    ) {
                        Badge("★ Más pedido")
                    }
                }
                if (!disponible) {
                    Box(
                        modifier = Modifier.fillMaxSize().padding(8.dp),
                        contentAlignment = Alignment.TopEnd,
                    ) {
                        Badge("Agotado", color = Mut)
                    }
                }
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, end = 16.dp, bottom = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f)) {
                    Text(nombre, fontWeight = FontWeight.ExtraBold, fontSize = 17.sp)
                    Text(
                        "Incluye tus acompañamientos",
                        color = Mut,
                        fontSize = 12.sp,
                    )
                }
                Spacer(Modifier.width(8.dp))
                Surface(
                    shape = RoundedCornerShape(99.dp),
                    color = if (disponible) Ac else Mut,
                ) {
                    Text(
                        precio,
                        color = Color.White,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 13.sp,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                    )
                }
            }
        }
    }
}

/** Tarjeta compacta de acompañamiento con check animado. */
@Composable
fun TarjetaAcompanamiento(
    nombre: String,
    seleccionado: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val anchoBorde = if (seleccionado) 2.dp else 1.dp
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 3.dp,
        border = BorderStroke(
            anchoBorde,
            if (seleccionado) Ac else Line,
        ),
        modifier = modifier,
    ) {
        Column(Modifier.padding(8.dp)) {
            Box {
                FotoPlaceholder(
                    alto = 86.dp,
                    etiqueta = "Foto de $nombre",
                    redondeo = 12.dp,
                )
                Box(
                    modifier = Modifier.fillMaxSize().padding(6.dp),
                    contentAlignment = Alignment.TopEnd,
                ) {
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .clip(CircleShape)
                            .background(if (seleccionado) Ac else Color.White)
                            .border(1.dp, if (seleccionado) Ac else Line, CircleShape),
                        contentAlignment = Alignment.Center,
                    ) {
                        if (seleccionado) {
                            Text(
                                "✓",
                                color = Color.White,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                            )
                        }
                    }
                }
            }
            Text(
                nombre,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                modifier = Modifier.padding(horizontal = 6.dp, vertical = 9.dp),
            )
        }
    }
}

/** Línea de tiempo vertical del estado del pedido. */
@Composable
fun LineaEstado(estado: String) {
    val pasos = listOf(
        "Recibido" to "Entramos tu pedido",
        "Preparación" to "Lo estamos armando",
        "Listo" to "Ya puedes retirar",
        "Entregado" to "¡Buen provecho!",
    )
    // EFT (POO): cada EstadoPedido conoce su posición en la línea.
    val actual = EstadoPedido.desdeOrNull(estado)?.paso ?: 0

    Column {
        pasos.forEachIndexed { i, (titulo, detalle) ->
            val hecho = actual >= i
            Row {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.width(34.dp),
                ) {
                    Box(
                        modifier = Modifier
                            .size(26.dp)
                            .clip(CircleShape)
                            .background(if (hecho) Ac else Line),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            if (hecho) "✓" else "${i + 1}",
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                    if (i < pasos.lastIndex) {
                        Box(
                            Modifier
                                .width(3.dp)
                                .height(30.dp)
                                .background(if (actual > i) Ac else Line),
                        )
                    }
                }
                Column(Modifier.padding(start = 10.dp, bottom = 14.dp)) {
                    Text(
                        titulo,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = if (hecho) Ink else Mut,
                    )
                    Text(detalle, fontSize = 12.sp, color = Mut)
                }
            }
        }
    }
}

/** Panel verde con el total del pedido, reutilizable en carrito y retiro. */
@Composable
fun PanelTotal(
    valor: Int,
    etiqueta: String = "Total",
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(
                Brush.horizontalGradient(listOf(VerdeOscuro, Ac)),
                RoundedCornerShape(18.dp),
            )
            .padding(horizontal = 18.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            etiqueta,
            color = Color.White,
            fontWeight = FontWeight.Bold,
            fontSize = 15.sp,
            modifier = Modifier.weight(1f),
        )
        TotalAnimado(valor = valor, fontSize = 24.sp, color = Color.White)
    }
}

/** Encabezado de bloque con un glifo small y título en negrita. */
@Composable
fun TituloBloque(glifo: String, texto: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(glifo, fontSize = 17.sp)
        Spacer(Modifier.width(8.dp))
        Text(texto, fontWeight = FontWeight.ExtraBold, fontSize = 16.sp)
    }
}

/**
 * CTA flotante estilo app de comida: barra oscura, glifo verde y precio.
 * No decide nada: solo dibuja y propaga el onClick que ya tenga la pantalla.
 */
@Composable
fun BarraPedido(
    texto: String,
    detalle: String? = null,
    onClick: () -> Unit,
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(99.dp),
        color = Ink,
        shadowElevation = 10.dp,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(6.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(Ac),
                contentAlignment = Alignment.Center,
            ) {
                Text("🛍️", fontSize = 18.sp)
            }
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    texto,
                    color = Color.White,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 15.sp,
                )
                if (detalle != null) {
                    Text(
                        detalle,
                        color = Color(0xFFB7C9B9),
                        fontSize = 11.sp,
                    )
                }
            }
            Text(
                "→",
                color = Gold,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(end = 14.dp),
            )
        }
    }
}

/**
 * Barra inferior de 4 ítems. Usa SOLO callbacks que ya existían: no agrega
 * rutas ni destinations nuevas, nada más visibiliza lo que ya estaba.
 */
@Composable
fun BarraNavegacion(
    onInicio: () -> Unit,
    onMenu: () -> Unit,
    onPedido: () -> Unit,
    onPerfil: () -> Unit,
) {
    Surface(
        color = Color.White,
        shape = RoundedCornerShape(20.dp),
        shadowElevation = 4.dp,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            ItemNav("🏠", "Inicio", activo = true, onClick = onInicio)
            ItemNav("🍽️", "Menú", onClick = onMenu)
            ItemNav("🧾", "Pedido", onClick = onPedido)
            ItemNav("👤", "Perfil", onClick = onPerfil)
        }
    }
}

@Composable
private fun ItemNav(
    glifo: String,
    texto: String,
    activo: Boolean = false,
    onClick: () -> Unit,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clip(RoundedCornerShape(14.dp))
            .background(if (activo) MaterialTheme.colorScheme.primaryContainer else Color.White)
            .padding(horizontal = 12.dp, vertical = 6.dp)
            .clickable(onClick = onClick),
    ) {
        Text(glifo, fontSize = 18.sp)
        Spacer(Modifier.height(2.dp))
        Text(
            texto,
            color = if (activo) Ac else Mut,
            fontSize = 11.sp,
            fontWeight = if (activo) FontWeight.ExtraBold else FontWeight.Bold,
        )
    }
}

/** Fila decorativa de puntos de colores, usada en la confirmación. */
@Composable
fun Confeti() {
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        listOf(Ac, Color(0xFFFFC83D), Color(0xFF2E9E5B), Ac, Color(0xFFFF8A5B)).forEach {
            Box(
                Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(it),
            )
        }
    }
}
