package cl.duoc.casajojo.ui.screens.cliente

import cl.duoc.casajojo.domain.model.EstadoPedido
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cl.duoc.casajojo.ui.components.Boton
import cl.duoc.casajojo.ui.components.CancelarPedido
import cl.duoc.casajojo.ui.components.Confeti
import cl.duoc.casajojo.ui.components.MSG_FUERA_DE_PLAZO
import cl.duoc.casajojo.ui.components.MSG_PEDIDO_CANCELADO
import cl.duoc.casajojo.ui.components.Pill
import cl.duoc.casajojo.utils.compartirTexto
import cl.duoc.casajojo.utils.colorEstadoPago
import cl.duoc.casajojo.utils.colorEstadoPedido
import cl.duoc.casajojo.utils.money
import cl.duoc.casajojo.utils.textoEstadoPago
import cl.duoc.casajojo.utils.textoEstadoPedido
import cl.duoc.casajojo.ui.layouts.Pantalla
import cl.duoc.casajojo.ui.theme.Ac
import cl.duoc.casajojo.ui.viewmodels.ClienteViewModel

/**
 * PASO 5 — Confirmación. Mismo resumen y mismas acciones de siempre,
 * con una "tarjeta de felicidades" y confeti para darle vida.
 */
@Composable
fun ViewConfirmado(
    vm: ClienteViewModel,
    onConsultar: () -> Unit,
    onOtroPedido: () -> Unit,
) {
    val ultimoPedido by vm.ultimoPedido.collectAsState()
    val pedido = ultimoPedido?.pedido
    val platos = ultimoPedido?.platos.orEmpty()

    // RF-13: avisos de cancelación y estado "cancelado" en la tarjeta.
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    fun avisar(mensaje: String) {
        scope.launch { snackbar.showSnackbar(mensaje) }
    }
    val cancelado = pedido?.estadoPedido == EstadoPedido.Cancelado.codigo

    val context = LocalContext.current
    val resumen = buildString {
        append("Pedido #${vm.ultimoNumero ?: pedido?.numero ?: "…"} en La Casa JoJo")
        pedido?.let { p ->
            append("\nRetiro a las ${p.horaRetiro} h · a nombre de ${p.nombre}")
            append("\n${platos.size} ${if (platos.size == 1) "plato" else "platos"} · ${p.total.money()}")
            append("\nPago: ${textoEstadoPago(p.estadoPago)}")
        }
    }

    Box(Modifier.fillMaxSize()) {
    Pantalla {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
        ) {
            Surface(
                shape = RoundedCornerShape(26.dp),
                color = MaterialTheme.colorScheme.primaryContainer,
                shadowElevation = 4.dp,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 26.dp),
                ) {
                    Confeti()
                    Spacer(Modifier.height(16.dp))
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(76.dp)
                            .clip(CircleShape)
                            .background(if (cancelado) MaterialTheme.colorScheme.error else Ac),
                    ) {
                        Text(
                            if (cancelado) "✕" else "✓",
                            color = Color.White,
                            fontSize = 42.sp,
                            lineHeight = 42.sp,
                        )
                    }
                    Spacer(Modifier.height(18.dp))
                    Text(
                        if (cancelado) "Pedido cancelado" else "¡Pedido confirmado!",
                        fontSize = 25.sp,
                        fontWeight = FontWeight.Black,
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        "Tu número de pedido es",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 14.sp,
                    )
                    Surface(
                        shape = RoundedCornerShape(18.dp),
                        color = Color.White,
                        shadowElevation = 3.dp,
                        modifier = Modifier.padding(top = 10.dp),
                    ) {
                        Text(
                            "#${vm.ultimoNumero ?: "…"}",
                            fontSize = 42.sp,
                            fontWeight = FontWeight.Black,
                            color = Ac,
                            modifier = Modifier.padding(horizontal = 26.dp, vertical = 6.dp),
                        )
                    }
                }
            }

            Spacer(Modifier.height(20.dp))
            Surface(
                color = MaterialTheme.colorScheme.surface,
                shape = RoundedCornerShape(20.dp),
                shadowElevation = 3.dp,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(Modifier.padding(18.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Pill(
                            pedido?.estadoPedido?.let { textoEstadoPedido(it) } ?: "…",
                            pedido?.estadoPedido?.let { colorEstadoPedido(it) } ?: Color(0xFF6D6D67),
                        )
                        Pill(
                            pedido?.estadoPago?.let { textoEstadoPago(it) } ?: "…",
                            pedido?.estadoPago?.let { colorEstadoPago(it) } ?: Color(0xFF6D6D67),
                        )
                    }
                    Spacer(Modifier.height(12.dp))
                    Text("Retiro a las ${pedido?.horaRetiro ?: "…"} h", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    if (platos.isNotEmpty()) {
                        Spacer(Modifier.height(4.dp))
                        Text(
                            "${platos.size} ${if (platos.size == 1) "plato" else "platos"} · ${pedido?.total?.money() ?: "…"}",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 14.sp,
                        )
                    }
                }
            }

            Spacer(Modifier.height(24.dp))
            Boton("Consultar estado de mi pedido", onClick = onConsultar, estilo = "acento")
            Spacer(Modifier.height(10.dp))
            Boton(
                "Compartir mi pedido",
                onClick = { compartirTexto(context, resumen) },
                estilo = "fantasma",
            )
            Spacer(Modifier.height(10.dp))
            Boton("Hacer otro pedido", onClick = onOtroPedido, estilo = "fantasma")

            // RF-13: el botón aparece solo durante los 15 s posteriores al pedido.
            ultimoPedido?.pedido?.let { p ->
                Spacer(Modifier.height(14.dp))
                CancelarPedido(
                    pedido = p,
                    cancelando = vm.cancelando,
                    onCancelar = {
                        vm.cancelarPedido(p) { ok ->
                            if (ok) avisar(MSG_PEDIDO_CANCELADO) else avisar(MSG_FUERA_DE_PLAZO)
                        }
                    },
                    onFueraDePlazo = { avisar(MSG_FUERA_DE_PLAZO) },
                )
            }
        }
    }
        SnackbarHost(snackbar, Modifier.align(Alignment.BottomCenter))
    }
}
