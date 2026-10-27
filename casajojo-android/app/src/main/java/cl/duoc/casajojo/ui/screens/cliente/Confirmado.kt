package cl.duoc.casajojo.ui.screens.cliente

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Share
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
import cl.duoc.casajojo.ui.components.CancelarPedido
import cl.duoc.casajojo.ui.components.ErrorEcho
import cl.duoc.casajojo.ui.components.MSG_FUERA_DE_PLAZO
import cl.duoc.casajojo.ui.components.MSG_PEDIDO_CANCELADO
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cl.duoc.casajojo.ui.components.Boton
import cl.duoc.casajojo.ui.components.EstiloBoton
import cl.duoc.casajojo.ui.components.Pill
import cl.duoc.casajojo.utils.compartirTexto
import cl.duoc.casajojo.utils.colorEstadoPago
import cl.duoc.casajojo.utils.colorEstadoPedido
import cl.duoc.casajojo.utils.money
import cl.duoc.casajojo.utils.textoEstadoPago
import cl.duoc.casajojo.utils.textoEstadoPedido
import cl.duoc.casajojo.ui.layouts.Pantalla
import cl.duoc.casajojo.ui.viewmodels.ClienteViewModel

/**
 * PASO 5 — Confirmación.
 * Se llega aquí después de que el ViewModel guardó el pedido en Room.
 * Muestra el número asignado y un resumen leído desde la base de datos.
 */
@Composable
fun ViewConfirmado(
    vm: ClienteViewModel,
    onConsultar: () -> Unit,
    onOtroPedido: () -> Unit,
) {
    // CORRECCIÓN: igual que en Home, nos suscribimos al Flow con collectAsState()
    // para que Room entregue el pedido recién guardado. Antes se mostraba "…".
    val ultimoPedido by vm.ultimoPedido.collectAsState()
    // CORRECCIÓN: el StateFlow recuerda el último valor que tuvo, así que al
    // llegar aquí puede entregar por un instante el pedido ANTERIOR (hasta que
    // Room emite el nuevo). Se muestra solo si su número es el recién confirmado;
    // si no, la tarjeta muestra "…" y "Compartir" no mezcla datos de dos pedidos.
    // (Si Android cerró la app y se perdió ultimoNumero, se usa el último pedido.)
    val confirmado = ultimoPedido?.takeIf { vm.ultimoNumero == null || it.pedido.numero == vm.ultimoNumero }
    val pedido = confirmado?.pedido
    val platos = confirmado?.platos.orEmpty()

    val context = LocalContext.current
    // RECURSO NATIVO: texto que se envía al "share sheet" del sistema.
    val resumen = buildString {
        append("Pedido #${vm.ultimoNumero ?: pedido?.numero ?: "…"} en La Casa JoJo")
        pedido?.let { p ->
            append("\nRetiro a las ${p.horaRetiro} h · a nombre de ${p.nombre}")
            append("\n${platos.size} ${if (platos.size == 1) "plato" else "platos"} · ${p.total.money()}")
            append("\nPago: ${textoEstadoPago(p.estadoPago)}")
        }
    }

    // NUEVO (RF-13): avisos de la cancelación ("Pedido cancelado" / fuera de plazo).
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    fun avisar(mensaje: String) {
        scope.launch { snackbar.showSnackbar(mensaje) }
    }
    val cancelado = pedido?.estadoPedido == "cancelado"
    ErrorEcho(vm.error) { vm.limpiarError() }

    Box(Modifier.fillMaxSize()) {
        Pantalla {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth().padding(top = 28.dp),
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(72.dp)
                        .clip(CircleShape)
                        // Si se canceló, el círculo pasa a rojo con una ✕.
                        .background(if (cancelado) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary),
                ) {
                    Text(if (cancelado) "✕" else "✓", color = Color.White, fontSize = 40.sp, lineHeight = 40.sp)
                }
                Spacer(Modifier.height(18.dp))
                Text(if (cancelado) "Pedido cancelado" else "Pedido confirmado", fontSize = 26.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(6.dp))
                Text(
                    "Tu número de pedido es",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 15.sp,
                )
                // ultimoNumero lo deja el ViewModel apenas termina de guardar, así que
                // el número aparece de inmediato aunque Room aún no haya emitido.
                Text(
                    "#${vm.ultimoNumero ?: pedido?.numero ?: "…"}",
                    fontSize = 44.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(top = 4.dp),
                )

                Surface(
                    color = MaterialTheme.colorScheme.surface,
                    shape = RoundedCornerShape(12.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                    modifier = Modifier.fillMaxWidth().padding(top = 20.dp),
                ) {
                    Column(Modifier.padding(16.dp)) {
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
                        Spacer(Modifier.height(10.dp))
                        Text(
                            "Retiro a las ${pedido?.horaRetiro ?: "…"} h",
                            fontSize = 16.sp,
                        )
                        if (platos.isNotEmpty()) {
                            Spacer(Modifier.height(4.dp))
                            Text(
                                "${platos.size} ${if (platos.size == 1) "plato" else "platos"} · ${pedido?.total?.money() ?: "…"}",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        // NUEVO (RF-13): cuenta regresiva y botón "Cancelar pedido".
                        if (confirmado != null) {
                            CancelarPedido(
                                pedido = confirmado.pedido,
                                cancelando = vm.cancelando,
                                onCancelar = {
                                    vm.cancelarPedido(confirmado.pedido) { ok ->
                                        avisar(if (ok) MSG_PEDIDO_CANCELADO else MSG_FUERA_DE_PLAZO)
                                    }
                                },
                                onFueraDePlazo = { avisar(MSG_FUERA_DE_PLAZO) },
                            )
                        }
                    }
                }

                Spacer(Modifier.height(24.dp))
                Boton("Consultar estado de mi pedido", onClick = onConsultar, estilo = EstiloBoton.Acento)
                Spacer(Modifier.height(10.dp))
                // RECURSO NATIVO: abre la hoja de compartir del sistema
                // (WhatsApp, correo, notas…). No requiere permisos.
                Boton(
                    "Compartir mi pedido",
                    onClick = { compartirTexto(context, resumen) },
                    estilo = EstiloBoton.Fantasma,
                    icono = Icons.Filled.Share,
                )
                Spacer(Modifier.height(10.dp))
                Boton("Hacer otro pedido", onClick = onOtroPedido, estilo = EstiloBoton.Fantasma)
            }
        }
        SnackbarHost(snackbar, Modifier.align(Alignment.BottomCenter))
    }
}