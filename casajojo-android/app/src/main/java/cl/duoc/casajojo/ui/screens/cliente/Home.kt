package cl.duoc.casajojo.ui.screens.cliente

import androidx.compose.material3.TextButton
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import cl.duoc.casajojo.ui.theme.esTemaOscuro
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import cl.duoc.casajojo.ui.components.Badge
import cl.duoc.casajojo.ui.components.BarraNavegacion
import cl.duoc.casajojo.ui.components.BarraPedido
import cl.duoc.casajojo.ui.components.AvatarUsuario
import cl.duoc.casajojo.ui.components.DegradadoInferior
import cl.duoc.casajojo.ui.components.FotoPlato
import cl.duoc.casajojo.ui.components.imagenDePlato
import cl.duoc.casajojo.ui.components.inicialesDe
import cl.duoc.casajojo.data.local.PerfilPhotoStorage
import android.annotation.SuppressLint
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import cl.duoc.casajojo.ui.components.Nota
import cl.duoc.casajojo.ui.layouts.Pantalla
import cl.duoc.casajojo.ui.theme.Ac
import cl.duoc.casajojo.ui.theme.Gold
import cl.duoc.casajojo.ui.theme.Ink
import cl.duoc.casajojo.ui.theme.Mut
import cl.duoc.casajojo.ui.theme.VerdeOscuro
import cl.duoc.casajojo.ui.viewmodels.ClienteViewModel
import cl.duoc.casajojo.ui.viewmodels.ClimaViewModel

/**
 * PASO 0 — Portada.
 * Mismo flujo de siempre (comenzar / ver pedido / perfil / cerrar sesión),
 * con una portada tipo app de comida rápida, imagen del plato del día y avatar del usuario.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ViewHome(
    vm: ClienteViewModel,
    onComenzar: () -> Unit,
    onMiPedido: () -> Unit,
    onPerfil: () -> Unit,
    fotoPerfil: PerfilPhotoStorage.Foto? = null,
) {
    val usuario by vm.usuario.collectAsState()
    val proteinas by vm.proteinas.collectAsState()

    // API externa (Open-Meteo): estado separado del ViewModel del pedido.
    val climaVm: ClimaViewModel = viewModel()
    val clima by climaVm.estado.collectAsState()
    val oscuro = esTemaOscuro()

    // Los 6 platos reales (Room/API). Si aún no cargó, usamos el seed local.
    val antojos = proteinas.take(6).map { it.nombre to it.disponible }.ifEmpty {
        listOf(
            "Pollo asado" to true,
            "Carne al jugo" to true,
            "Guatitas" to true,
            "Cerdo mongoliano" to false,
            "Filete de pollo" to true,
            "Tortilla de porotos verdes" to true,
        )
    }

    Pantalla(
        bar = {
            Column {
                BarraPedido(
                    texto = "Comenzar mi pedido",
                    detalle = "Elige proteína y acompañamientos",
                    onClick = onComenzar,
                )
                Spacer(Modifier.height(8.dp))
                BarraNavegacion(
                    onInicio = {},
                    onMenu = onComenzar,
                    onPedido = onMiPedido,
                    onPerfil = onPerfil,
                )
            }
        },
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp, bottom = 18.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f)) {
                    Text(
                        "¡Hola, ${usuario?.nombre?.substringBefore(' ') ?: "cliente"}!",
                        fontSize = 19.sp,
                        fontWeight = FontWeight.Black,
                    )
                    Text(
                        "¿Qué se te antoja hoy?",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                // Avatar: misma foto que Perfil (PerfilPhotoStorage.foto); sin foto, iniciales.
                Surface(
                    onClick = onPerfil,
                    shape = CircleShape,
                    color = Ac,
                    shadowElevation = 4.dp,
                    border = BorderStroke(2.dp, Gold),
                ) {
                    AvatarUsuario(
                        foto = fotoPerfil,
                        iniciales = inicialesDe(usuario?.nombre),
                        tamano = 48.dp,
                    )
                }
            }

            Surface(
                shape = RoundedCornerShape(26.dp),
                color = Ink,
                shadowElevation = 6.dp,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Box {
                    // Círculo dorado decorativo: ahora queda abajo a la derecha, detrás del
                    // texto y recortado por la tarjeta, en vez de asomar tras la imagen.
                    Box(
                        Modifier
                            .align(Alignment.BottomEnd)
                            .offset(x = 46.dp, y = 46.dp)
                            .size(150.dp)
                            .clip(CircleShape)
                            .background(Gold.copy(alpha = 0.16f)),
                    )
                    Column {
                        Box(Modifier.padding(9.dp)) {
                            ImagenPlatoDelDia(alto = 160.dp)
                            // Las dos etiquetas van en fila (y bajan de línea si no caben),
                            // así nunca se tapan entre sí.
                            // Abajo, sobre el degradado oscuro de la foto, para que se lean.
                            Box(
                                Modifier.matchParentSize().padding(12.dp),
                                contentAlignment = Alignment.BottomStart,
                            ) {
                                FlowRow(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalArrangement = Arrangement.spacedBy(6.dp),
                                ) {
                                    Badge("🔥 Listo en 10 min", color = Ac)
                                    Badge("Retiro express", color = Gold, colorTexto = Ink)
                                }
                            }
                        }
                        Column(Modifier.padding(horizontal = 18.dp, vertical = 14.dp)) {
                            Badge("Solo hoy", color = Gold, colorTexto = Ink)
                            Spacer(Modifier.height(10.dp))
                            Text(
                                "Tu almuerzo,\nsin esperar.",
                                color = Color.White,
                                fontSize = 27.sp,
                                fontWeight = FontWeight.Black,
                                lineHeight = 31.sp,
                            )
                            Spacer(Modifier.height(6.dp))
                            Text(
                                "Elige tu proteína, suma tus acompañamientos y retira a la hora que quieras.",
                                color = Color(0xFFD8E8DC),
                                fontSize = 14.sp,
                            )
                        }
                    }
                }
            }

            // API externa (Open-Meteo). Dato secundario para el retiro en Viña:
            // no interfiere con el menú, el carrito ni los microservicios.
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("🌤️", fontSize = 18.sp)
                    Spacer(Modifier.width(10.dp))
                    Column(Modifier.weight(1f)) {
                        Text(
                            "Viña del Mar ahora",
                            color = if (oscuro) Color.White else Mut,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                        )
                        // EFT (API externa): estados de carga, éxito y error con reintento.
                        val detalleClima = when {
                            clima.cargando -> "Consultando clima…"
                            clima.texto != null -> clima.texto ?: "Clima disponible"
                            clima.fallo -> clima.error ?: "Sin conexión con el clima"
                            else -> "Disponible al abrir la app"
                        }
                        Text(
                            detalleClima,
                            // Error en rojo; si no, el color del tema (claro u oscuro).
                            color = when {
                                clima.fallo -> MaterialTheme.colorScheme.error
                                oscuro -> Color.White
                                else -> VerdeOscuro
                            },
                            fontSize = 12.sp,
                            fontWeight = FontWeight.ExtraBold,
                        )
                    }
                    if (clima.cargando) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.dp,
                            color = if (oscuro) Color.White else VerdeOscuro,
                        )
                    } else if (clima.fallo) {
                        TextButton(onClick = climaVm::cargar) { Text("Reintentar") }
                    }
                }
            }

            Spacer(Modifier.height(22.dp))
            Text("¿Qué te antoja?", fontWeight = FontWeight.Black, fontSize = 18.sp)
            Spacer(Modifier.height(10.dp))
            Column(modifier = Modifier.fillMaxWidth()) {
                antojos.chunked(3).forEachIndexed { fila, items ->
                    if (fila > 0) Spacer(Modifier.height(12.dp))
                    // Cada chip ocupa un tercio exacto: los nombres largos se parten en
                    // 2 líneas centradas en vez de empujar a los vecinos.
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        items.forEach { (nombre, disponible) ->
                            CategoriaChip(
                                nombre,
                                disponible,
                                Modifier.weight(1f),
                            )
                        }
                        repeat(3 - items.size) { Spacer(Modifier.weight(1f)) }
                    }
                }
            }

            Spacer(Modifier.height(22.dp))
            Text("Hoy en Casa JoJo", fontWeight = FontWeight.Black, fontSize = 18.sp)
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(10.dp)) {
                Surface(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(18.dp),
                    color = MaterialTheme.colorScheme.secondaryContainer,
                    shadowElevation = 2.dp,
                ) {
                    Column(Modifier.padding(14.dp)) {
                        Text(
                            "2x1",
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Black,
                            color = if (oscuro) Color.White else VerdeOscuro,
                        )
                        Text(
                            "en acompañamientos",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (oscuro) Gold else Color(0xFF4A3600),
                        )
                    }
                }
                Surface(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(18.dp),
                    color = MaterialTheme.colorScheme.primaryContainer,
                    shadowElevation = 2.dp,
                ) {
                    Column(Modifier.padding(14.dp)) {
                        Text(
                            "Sin fila",
                            fontSize = 19.sp,
                            fontWeight = FontWeight.Black,
                            color = if (oscuro) Gold else VerdeOscuro,
                        )
                        Text(
                            "retira a tu hora",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (oscuro) Color.White else Ac,
                        )
                    }
                }
            }

            // Los accesos rápidos (perfil, pedido, historial y cerrar sesión) ahora viven
            // en la pestaña Perfil, como en las apps de delivery.
            Nota("Pide antes de las 12 y retira sin esperar: tu almuerzo sale en menos de 10 minutos.")
        }
    }
}

/**
 * Imagen de la tarjeta principal. Prioridad:
 *  1. res/drawable-nodpi/plato_del_dia.(jpg|png|webp), si existe (se busca por nombre:
 *     basta con copiar el archivo y recompilar, sin tocar código);
 *  2. la foto real del Pollo asado (imagenDePlato);
 *  3. la composición ilustrada con la marca.
 * La foto llena el recuadro con esquinas redondeadas y un degradado oscuro abajo.
 */
@SuppressLint("DiscouragedApi")
@Composable
private fun ImagenPlatoDelDia(alto: Dp) {
    val context = LocalContext.current
    val delDia = remember {
        context.resources.getIdentifier("plato_del_dia", "drawable", context.packageName)
    }
    val foto = delDia.takeIf { it != 0 } ?: imagenDePlato("CJ-PROT-01", "Pollo asado")
    if (foto != null) {
        Box(
            Modifier
                .fillMaxWidth()
                .height(alto)
                .clip(RoundedCornerShape(18.dp)),
        ) {
            Image(
                painter = painterResource(foto),
                contentDescription = if (delDia != 0) "Plato del día de La Casa JoJo" else "Pollo asado",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
            DegradadoInferior()
        }
        return
    }
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(alto)
            .clip(RoundedCornerShape(18.dp))
            .background(Brush.linearGradient(listOf(Ac, VerdeOscuro, Ink))),
        contentAlignment = Alignment.Center,
    ) {
        // Halo dorado detrás del plato principal.
        Box(
            Modifier
                .size(118.dp)
                .clip(CircleShape)
                .background(Gold.copy(alpha = 0.28f)),
        )
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier.padding(top = 26.dp)
                .semantics { contentDescription = "Ilustración de un almuerzo de La Casa JoJo" },
        ) {
            Text("🥗", fontSize = 34.sp)
            Text("🍗", fontSize = 64.sp)
            Text("🍚", fontSize = 34.sp)
        }
    }
}

/** Chips circulares del menú real: decorativos, no crean rutas nuevas. */
@Composable
private fun CategoriaChip(
    texto: String,
    disponible: Boolean,
    modifier: Modifier = Modifier,
) {
    val oscuro = esTemaOscuro()
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = modifier) {
        Surface(
            shape = CircleShape,
            color = when {
                disponible -> MaterialTheme.colorScheme.primaryContainer
                oscuro -> MaterialTheme.colorScheme.surfaceVariant
                else -> Color(0xFFEFEFEA)
            },
            shadowElevation = 2.dp,
        ) {
            // Foto recortada en círculo que llena todo el chip (respaldo: glifo del plato).
            FotoPlato(
                nombre = texto,
                alto = 58.dp,
                forma = CircleShape,
                agotado = !disponible,
                modifier = Modifier.size(58.dp),
            )
        }
        Spacer(Modifier.height(6.dp))
        Text(
            texto,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 11.sp,
            lineHeight = 13.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.fillMaxWidth(),
        )
        if (!disponible) {
            Text(
                "Agotado",
                color = MaterialTheme.colorScheme.error,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}
