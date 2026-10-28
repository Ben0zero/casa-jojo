package cl.duoc.casajojo.ui.screens.cliente

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.History
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cl.duoc.casajojo.data.local.entity.PedidoWithPlatos
import cl.duoc.casajojo.ui.components.ChipRow
import cl.duoc.casajojo.ui.components.Pill
import cl.duoc.casajojo.ui.components.Titulo
import cl.duoc.casajojo.ui.components.Volver
import cl.duoc.casajojo.ui.layouts.BarraSuperior
import cl.duoc.casajojo.ui.layouts.Pantalla
import cl.duoc.casajojo.ui.viewmodels.ClienteViewModel
import cl.duoc.casajojo.ui.viewmodels.FiltroHistorial
import cl.duoc.casajojo.ui.viewmodels.HistorialUiState
import cl.duoc.casajojo.utils.colorEstadoPago
import cl.duoc.casajojo.utils.colorEstadoPedido
import cl.duoc.casajojo.utils.fechaHora
import cl.duoc.casajojo.utils.money
import cl.duoc.casajojo.utils.textoEstadoPago
import cl.duoc.casajojo.utils.textoEstadoPedido

/**
 * NUEVO (RF-14) — Historial de pedidos finalizados del usuario.
 * Chips "Entregados" (por defecto) y "Cancelados". Los datos vienen de Room
 * como Flow, así que al retirar o cancelar un pedido la lista se actualiza sola.
 */
@Composable
fun ViewHistorial(
    vm: ClienteViewModel,
    onVolver: () -> Unit,
    onCart: () -> Unit,
) {
    val filtro by vm.filtroHistorial.collectAsState()
    val historial by vm.historial.collectAsState()

    Column(Modifier.fillMaxWidth()) {
        BarraSuperior(cartCount = vm.cart.size, onCartClick = onCart)
        Pantalla {
            Column {
                Volver(onVolver)
                Titulo("Historial", "Tus pedidos finalizados, el más reciente primero")
                Spacer(Modifier.height(14.dp))
                ChipRow(
                    opciones = FiltroHistorial.entries.map { it.etiqueta },
                    seleccion = filtro.etiqueta,
                    onSeleccionar = { etiqueta ->
                        FiltroHistorial.entries.firstOrNull { it.etiqueta == etiqueta }
                            ?.let(vm::onFiltroHistorial)
                    },
                )
                Spacer(Modifier.height(6.dp))
                // ANIMACIÓN: al cambiar de filtro, la lista hace crossfade.
                // contentKey = filtro: solo anima al cambiar de chip, no cada vez
                // que Room entrega la misma lista actualizada.
                AnimatedContent(
                    targetState = historial,
                    contentKey = { it.filtro },
                    transitionSpec = { fadeIn(tween(250)) togetherWith fadeOut(tween(150)) },
                    label = "historial",
                ) { estado ->
                    ListaHistorial(estado)
                }
            }
        }
    }
}

@Composable
private fun ListaHistorial(estado: HistorialUiState) {
    if (estado.pedidos.isEmpty()) {
        HistorialVacio(estado.filtro)
        return
    }
    Column {
        // forEach y no LazyColumn: Pantalla ya tiene scroll (ver MiPedido).
        estado.pedidos.forEachIndexed { i, datos ->
            key(datos.pedido.id) {
                // ANIMACIÓN: cada tarjeta entra subiendo y apareciendo, un poco
                // después de la anterior (efecto cascada, máximo 6 escalones).
                val visible = remember { MutableTransitionState(false).apply { targetState = true } }
                AnimatedVisibility(
                    visibleState = visible,
                    enter = fadeIn(tween(300, delayMillis = 50 * i.coerceAtMost(6))) +
                            slideInVertically(tween(300, delayMillis = 50 * i.coerceAtMost(6))) { it / 4 },
                ) {
                    TarjetaHistorial(datos)
                }
            }
        }
    }
}

@Composable
private fun HistorialVacio(filtro: FiltroHistorial) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxWidth().padding(top = 48.dp),
    ) {
        Icon(
            if (filtro == FiltroHistorial.Cancelados) Icons.Filled.Block else Icons.Filled.History,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(56.dp),
        )
        Spacer(Modifier.height(12.dp))
        Text(
            if (filtro == FiltroHistorial.Cancelados) "No tienes pedidos cancelados"
            else "Aún no tienes pedidos entregados",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 16.sp,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun TarjetaHistorial(datos: PedidoWithPlatos) {
    val p = datos.pedido
    Surface(
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(12.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        modifier = Modifier.fillMaxWidth().padding(top = 10.dp),
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("#${p.numero}", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.weight(1f))
                Pill(textoEstadoPedido(p.estadoPedido), colorEstadoPedido(p.estadoPedido))
                Pill(textoEstadoPago(p.estadoPago), colorEstadoPago(p.estadoPago))
            }
            Text(
                p.fecha.fechaHora(),
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(8.dp))
            datos.platos.forEach { plato ->
                Text(
                    "• ${plato.nombreProteina}" +
                            if (plato.solo) " (solo proteína)" else " — ${plato.acompanamientos.joinToString(" · ")}",
                    fontSize = 14.sp,
                )
            }
            Spacer(Modifier.height(10.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                Text(p.total.money(), fontWeight = FontWeight.Bold, fontSize = 17.sp)
            }
        }
    }
}
