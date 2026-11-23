package cl.duoc.casajojo.ui.screens.cliente

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
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
import cl.duoc.casajojo.ui.components.FotoPlaceholder
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
 * con una portada tipo app de comida rápida y espacio reservado para foto.
 */
@Composable
fun ViewHome(
    vm: ClienteViewModel,
    onComenzar: () -> Unit,
    onMiPedido: () -> Unit,
    onHistorial: () -> Unit,
    onPerfil: () -> Unit,
    onCerrarSesion: () -> Unit,
) {
    val usuario by vm.usuario.collectAsState()
    val ultimoPedido by vm.ultimoPedido.collectAsState()
    val proteinas by vm.proteinas.collectAsState()

    // API externa (Open-Meteo): estado separado del ViewModel del pedido.
    val climaVm: ClimaViewModel = viewModel()
    val clima by climaVm.estado.collectAsState()
    val oscuro = isSystemInDarkTheme()

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
                Surface(
                    onClick = onPerfil,
                    shape = CircleShape,
                    color = Ac,
                    shadowElevation = 4.dp,
                ) {
                    Box(Modifier.size(46.dp), contentAlignment = Alignment.Center) {
                        Text("CJ", color = Color.White, fontWeight = FontWeight.Black, fontSize = 16.sp)
                    }
                }
            }

            Surface(
                shape = RoundedCornerShape(26.dp),
                color = Ink,
                shadowElevation = 6.dp,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Box {
                    // Círculo dorado decorativo: guiño visual de la maqueta,
                    // dibujado con Compose, sin assets de otras marcas.
                    Box(
                        Modifier
                            .align(Alignment.TopEnd)
                            .offset(x = 28.dp, y = (-28).dp)
                            .size(150.dp)
                            .clip(CircleShape)
                            .background(Gold),
                    )
                    Column {
                        Box(Modifier.padding(9.dp)) {
                            FotoPlaceholder(alto = 150.dp, etiqueta = "Foto de tu plato", redondeo = 18.dp)
                            Box(
                                Modifier.fillMaxSize().padding(16.dp),
                                contentAlignment = Alignment.TopStart,
                            ) {
                                Badge("🔥 Listo en 10 min", color = Ac)
                            }
                            Box(
                                Modifier.fillMaxSize().padding(16.dp),
                                contentAlignment = Alignment.BottomStart,
                            ) {
                                Badge("Retiro express", color = Gold, colorTexto = Ink)
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
                        val detalleClima = when {
                            clima.cargando -> "Consultando clima…"
                            clima.texto != null -> clima.texto ?: "Clima disponible"
                            clima.fallo -> "Sin conexión con el clima"
                            else -> "Disponible al abrir la app"
                        }
                        Text(
                            detalleClima,
                            color = if (oscuro) Color.White else VerdeOscuro,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.ExtraBold,
                        )
                    }
                }
            }

            Spacer(Modifier.height(22.dp))
            Text("¿Qué te antoja?", fontWeight = FontWeight.Black, fontSize = 18.sp)
            Spacer(Modifier.height(10.dp))
            Column(modifier = Modifier.fillMaxWidth()) {
                antojos.chunked(3).forEachIndexed { fila, items ->
                    if (fila > 0) Spacer(Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                    ) {
                        items.forEach { (nombre, disponible) ->
                            CategoriaChip(iconoPara(nombre), nombre, disponible)
                        }
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

            Spacer(Modifier.height(22.dp))
            Text("Accesos rápidos", fontWeight = FontWeight.Black, fontSize = 18.sp)
            Spacer(Modifier.height(10.dp))
            FilaAcceso(glifo = "👤", texto = "Mi perfil", onClick = onPerfil)
            if (ultimoPedido != null) {
                FilaAcceso(glifo = "🧾", texto = "Ver mi pedido", onClick = onMiPedido)
            }
            FilaAcceso(glifo = "📚", texto = "Historial de pedidos", onClick = onHistorial)
            FilaAcceso(glifo = "↪️", texto = "Cerrar sesión", onClick = onCerrarSesion)

            Nota("Pide antes de las 12 y retira sin esperar: tu almuerzo sale en menos de 10 minutos.")
        }
    }
}

@Composable
private fun FilaAcceso(glifo: String, texto: String, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 2.dp,
        modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 13.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                Modifier
                    .size(34.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center,
            ) {
                Text(glifo, fontSize = 16.sp)
            }
            Spacer(Modifier.width(12.dp))
            Text(texto, fontWeight = FontWeight.Bold, fontSize = 14.sp, modifier = Modifier.weight(1f))
            Text("→", color = Ac, fontWeight = FontWeight.Bold, fontSize = 18.sp)
        }
    }
}

/** Glifo según el plato real de Casa JoJo. */
private fun iconoPara(nombre: String): String = when {
    nombre.contains("pollo", ignoreCase = true) -> "🍗"
    nombre.contains("carne", ignoreCase = true) -> "🥩"
    nombre.contains("guatita", ignoreCase = true) -> "🍲"
    nombre.contains("cerdo", ignoreCase = true) -> "🥓"
    nombre.contains("poroto", ignoreCase = true) -> "🫘"
    else -> "🍽️"
}

/** Chips circulares del menú real: decorativos, no crean rutas nuevas. */
@Composable
private fun CategoriaChip(
    glifo: String,
    texto: String,
    disponible: Boolean = true,
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Surface(
            shape = CircleShape,
            color = if (disponible) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                Color(0xFFEFEFEA)
            },
            shadowElevation = 2.dp,
        ) {
            Box(Modifier.size(58.dp), contentAlignment = Alignment.Center) {
                Text(glifo, fontSize = 24.sp)
            }
        }
        Spacer(Modifier.height(6.dp))
        Text(
            texto,
            color = if (disponible) MaterialTheme.colorScheme.onSurfaceVariant else Mut,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
        )
        if (!disponible) {
            Text("Agotado", color = Mut, fontSize = 9.sp, fontWeight = FontWeight.Bold)
        }
    }
}
