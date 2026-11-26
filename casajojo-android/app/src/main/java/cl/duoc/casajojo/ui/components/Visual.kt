package cl.duoc.casajojo.ui.components

import androidx.compose.foundation.text.TextAutoSize
import cl.duoc.casajojo.domain.model.EstadoPedido
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.res.painterResource
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.TextUnit
import cl.duoc.casajojo.data.local.PerfilPhotoStorage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
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
import cl.duoc.casajojo.ui.theme.Gold
import cl.duoc.casajojo.ui.theme.Ink
import cl.duoc.casajojo.ui.theme.Line
import cl.duoc.casajojo.ui.theme.Mut
import cl.duoc.casajojo.ui.theme.VerdeOscuro

/**
 * Piezas visuales reutilizables del nuevo diseño.
 *
 * Importante: acá NO hay lógica de negocio. Son composables "tontos" que dibujan
 * tarjetas, badges, imágenes de plato, avatares y líneas de estado.
 */

/** Glifo según el plato o acompañamiento real de Casa JoJo. */
fun emojiPlato(nombre: String): String {
    val n = nombre.lowercase()
    return when {
        "pollo" in n -> "🍗"
        "carne" in n -> "🥩"
        "guatita" in n -> "🍲"
        "cerdo" in n -> "🥓"
        "poroto" in n -> "🫘"
        "arroz" in n -> "🍚"
        "fideo" in n || "pesto" in n -> "🍝"
        "papa" in n -> "🍟"
        "puré" in n || "pure" in n -> "🥔"
        "ensalada" in n -> "🥗"
        "verdura" in n -> "🥦"
        else -> "🍽️"
    }
}

/**
 * Imagen de un plato. Si [imagenDePlato] encuentra la foto real se dibuja recortada
 * (ContentScale.Crop + clip con [forma]) para que nunca se deforme ni deje bordes vacíos.
 * Si no hay foto (por ejemplo, un plato nuevo del backend) se usa la ilustración con el
 * glifo del plato como respaldo. [agotado] la muestra en escala de grises y atenuada.
 */
@Composable
fun FotoPlato(
    nombre: String,
    modifier: Modifier = Modifier,
    alto: Dp = 140.dp,
    redondeo: Dp = 16.dp,
    sku: String? = null,
    forma: Shape = RoundedCornerShape(redondeo),
    agotado: Boolean = false,
    degradado: Boolean = false,
) {
    val foto = imagenDePlato(sku, nombre)
    val contenedor = modifier
        .fillMaxWidth()
        .height(alto)
        .clip(forma)
    if (foto != null) {
        Box(contenedor) {
            Image(
                painter = painterResource(foto),
                contentDescription = nombre,
                contentScale = ContentScale.Crop,
                colorFilter = if (agotado) FILTRO_AGOTADO else null,
                alpha = if (agotado) 0.6f else 1f,
                modifier = Modifier.fillMaxSize(),
            )
            if (degradado) DegradadoInferior()
        }
        return
    }
    val oscuro = isSystemInDarkTheme()
    val fondo = if (oscuro) {
        listOf(Color(0xFF1F4A37), Color(0xFF12301F))
    } else {
        listOf(FotoFondo, Color(0xFFD3EAD8))
    }
    Box(
        modifier = contenedor
            .background(Brush.linearGradient(fondo))
            .alpha(if (agotado) 0.6f else 1f),
        contentAlignment = Alignment.Center,
    ) {
        // Círculo dorado suave detrás del glifo: da profundidad sin parecer un error.
        Box(
            Modifier
                .size(alto * 0.78f)
                .clip(CircleShape)
                .background(Gold.copy(alpha = if (oscuro) 0.22f else 0.30f)),
        )
        Text(
            emojiPlato(nombre),
            fontSize = (alto.value * 0.40f).sp,
            modifier = Modifier.semantics { contentDescription = nombre },
        )
    }
}

/** Escala de grises para platos agotados. */
private val FILTRO_AGOTADO = ColorFilter.colorMatrix(ColorMatrix().apply { setToSaturation(0f) })

/** Degradado oscuro suave en la parte baja de una foto, para que las etiquetas se lean. */
@Composable
fun BoxScope.DegradadoInferior(alturaRelativa: Float = 0.55f) {
    Box(
        Modifier
            .align(Alignment.BottomCenter)
            .fillMaxWidth()
            .fillMaxHeight(alturaRelativa)
            .background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.55f)))),
    )
}

/**
 * Avatar circular del usuario. Con foto: se recorta en círculo (ContentScale.Crop) y se
 * decodifica al tamaño real en pantalla (nítida, sin thumbnail). Sin foto: iniciales.
 */
@Composable
fun AvatarUsuario(
    foto: PerfilPhotoStorage.Foto?,
    iniciales: String,
    tamano: Dp,
    modifier: Modifier = Modifier,
    tamanoTexto: TextUnit = 16.sp,
) {
    val px = with(LocalDensity.current) { tamano.roundToPx() }
    val imagen by produceState<ImageBitmap?>(null, foto?.ruta, foto?.version, px) {
        value = foto?.let {
            withContext(Dispatchers.IO) {
                PerfilPhotoStorage.decodificar(it.ruta, px)?.asImageBitmap()
            }
        }
    }
    Box(
        modifier = modifier
            .size(tamano)
            .clip(CircleShape)
            .background(Ac),
        contentAlignment = Alignment.Center,
    ) {
        val actual = imagen
        if (actual != null) {
            Image(
                bitmap = actual,
                contentDescription = "Foto de perfil",
                contentScale = ContentScale.Crop,
                filterQuality = FilterQuality.High,
                modifier = Modifier.fillMaxSize(),
            )
        } else {
            Text(iniciales, color = Color.White, fontWeight = FontWeight.Black, fontSize = tamanoTexto)
        }
    }
}

/** "Coca Mendoza" → "CM"; si no hay nombre, el sello "CJ". */
fun inicialesDe(nombre: String?): String =
    nombre.orEmpty().trim().split(Regex("\\s+"))
        .filter { it.isNotEmpty() }
        .take(2)
        .joinToString("") { it.first().uppercase() }
        .ifEmpty { "CJ" }

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
        modifier = modifier.fillMaxWidth(),
    ) {
        Column {
            Box(Modifier.padding(8.dp)) {
                // La foto llena todo el recuadro superior; agotado → escala de grises.
                FotoPlato(nombre, alto = 150.dp, redondeo = 15.dp, agotado = !disponible)
                if (destacado) {
                    Box(
                        modifier = Modifier.matchParentSize().padding(8.dp),
                        contentAlignment = Alignment.TopStart,
                    ) {
                        Badge("★ Más pedido")
                    }
                }
                if (!disponible) {
                    Box(
                        modifier = Modifier.matchParentSize().padding(8.dp),
                        contentAlignment = Alignment.TopEnd,
                    ) {
                        Badge("Agotado", color = Mut)
                    }
                }
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, end = 16.dp, bottom = 16.dp)
                    .alpha(if (disponible) 1f else 0.6f),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f)) {
                    Text(
                        nombre,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 17.sp,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        "Incluye tus acompañamientos",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
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
                FotoPlato(nombre, alto = 96.dp, redondeo = 12.dp)
                Box(
                    modifier = Modifier.matchParentSize().padding(6.dp),
                    contentAlignment = Alignment.TopEnd,
                ) {
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .clip(CircleShape)
                            .background(if (seleccionado) Ac else MaterialTheme.colorScheme.surface)
                            .border(1.5.dp, if (seleccionado) Ac else MaterialTheme.colorScheme.outline, CircleShape),
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
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
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
                            .background(if (hecho) Ac else MaterialTheme.colorScheme.outline),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            if (hecho) "✓" else "${i + 1}",
                            color = if (hecho) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                    if (i < pasos.lastIndex) {
                        Box(
                            Modifier
                                .width(3.dp)
                                .height(30.dp)
                                .background(if (actual > i) Ac else MaterialTheme.colorScheme.outline),
                        )
                    }
                }
                Column(Modifier.padding(start = 10.dp, bottom = 14.dp)) {
                    Text(
                        titulo,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = if (hecho) {
                            MaterialTheme.colorScheme.onSurface
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                    )
                    Text(detalle, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
 * CTA flotante estilo app de comida: barra dorada con texto y flecha verde oscuro.
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
        color = Gold,
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
                // Una línea cada uno: en pantallas chicas la barra no crece y no tapa contenido.
                Text(
                    texto,
                    color = Ink,
                    fontWeight = FontWeight.ExtraBold,
                    maxLines = 1,
                    autoSize = TextAutoSize.StepBased(minFontSize = 11.sp, maxFontSize = 15.sp),
                )
                if (detalle != null) {
                    Text(
                        detalle,
                        color = Ink,
                        fontSize = 11.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            Text(
                "→",
                color = Ink,
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
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(20.dp),
        shadowElevation = 4.dp,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Cada ítem ocupa un cuarto: en pantallas angostas ninguno se corta ("Per…").
            val celda = Modifier.weight(1f).padding(horizontal = 4.dp)
            ItemNav("🏠", "Inicio", activo = true, onClick = onInicio, modifier = celda)
            ItemNav("🍽️", "Menú", onClick = onMenu, modifier = celda)
            ItemNav("🧾", "Pedido", onClick = onPedido, modifier = celda)
            ItemNav("👤", "Perfil", onClick = onPerfil, modifier = celda)
        }
    }
}

@Composable
private fun ItemNav(
    glifo: String,
    texto: String,
    activo: Boolean = false,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(if (activo) MaterialTheme.colorScheme.primaryContainer else Color.Transparent)
            .clickable(onClick = onClick)
            .defaultMinSize(minHeight = 48.dp)
            .padding(horizontal = 4.dp, vertical = 6.dp),
    ) {
        Text(glifo, fontSize = 18.sp)
        Spacer(Modifier.height(2.dp))
        Text(
            texto,
            color = if (activo) {
                MaterialTheme.colorScheme.onPrimaryContainer
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            },
            maxLines = 1,
            autoSize = TextAutoSize.StepBased(minFontSize = 8.sp, maxFontSize = 11.sp),
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
