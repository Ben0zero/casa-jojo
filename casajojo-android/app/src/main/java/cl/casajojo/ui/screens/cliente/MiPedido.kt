package cl.duoc.casajojo.ui.screens.cliente

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cl.duoc.casajojo.data.local.entity.Pedido
import cl.duoc.casajojo.data.local.entity.PedidoWithPlatos
import cl.duoc.casajojo.domain.usecase.ReglasPedido
import cl.duoc.casajojo.ui.components.CancelarPedido
import cl.duoc.casajojo.ui.components.ErrorEcho
import cl.duoc.casajojo.ui.components.MSG_FUERA_DE_PLAZO
import cl.duoc.casajojo.ui.components.MSG_PEDIDO_CANCELADO
import cl.duoc.casajojo.ui.layouts.BarraSuperior
import cl.duoc.casajojo.ui.components.Boton
import cl.duoc.casajojo.ui.components.EstiloBoton
import cl.duoc.casajojo.ui.components.Pill
import cl.duoc.casajojo.ui.components.Titulo
import cl.duoc.casajojo.ui.components.Volver
import cl.duoc.casajojo.utils.colorEstadoPago
import cl.duoc.casajojo.utils.colorEstadoPedido
import cl.duoc.casajojo.utils.fechaHora
import cl.duoc.casajojo.utils.money
import cl.duoc.casajojo.utils.textoEstadoPago
import cl.duoc.casajojo.utils.textoEstadoPedido
import cl.duoc.casajojo.ui.layouts.Pantalla
import cl.duoc.casajojo.ui.viewmodels.ClienteViewModel
import com.google.mlkit.vision.codescanner.GmsBarcodeScanning
import kotlinx.coroutines.launch

/**
 * PASO 6 — Mis pedidos (RF-19: consultar estado).
 * CAMBIO: muestra TODOS los pedidos del usuario, el más reciente primero.
 * Antes solo se pedía el último a Room (LIMIT 1), así que al hacer un pedido
 * nuevo parecía que el anterior se borraba (seguía guardado, pero no se mostraba).
 * Como está conectado a Room mediante un Flow, si un estado cambia en la base
 * de datos la pantalla se actualiza sola.
 */
@Composable
fun ViewMiPedido(
    vm: ClienteViewModel,
    onVolver: () -> Unit,
    onCart: () -> Unit,
    onEmpezar: () -> Unit,
) {
    // collectAsState(): suscripción real al Flow de Room.
    val pedidos by vm.misPedidos.collectAsState()
    ErrorEcho(vm.error) { vm.limpiarError() }

    // NUEVO (RF-13): avisos de la cancelación ("Pedido cancelado" / fuera de plazo).
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    fun avisar(mensaje: String) {
        scope.launch { snackbar.showSnackbar(mensaje) }
    }

    Box(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxWidth()) {
            BarraSuperior(cartCount = vm.cart.size, onCartClick = onCart)
            Pantalla {
                Column {
                    Volver(onVolver)
                    Titulo(
                        "Mis pedidos",
                        if (pedidos.isEmpty()) null
                        else "${pedidos.size} ${if (pedidos.size == 1) "pedido" else "pedidos"} · el más reciente primero",
                    )
                    if (pedidos.isEmpty()) {
                        Spacer(Modifier.height(12.dp))
                        Text(
                            "Aún no tienes pedidos registrados en este dispositivo.",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 16.sp,
                        )
                        Spacer(Modifier.height(20.dp))
                        Boton("Hacer un pedido", onClick = onEmpezar, estilo = EstiloBoton.Acento)
                    } else {
                        // forEach y no LazyColumn: Pantalla ya tiene scroll vertical, y
                        // una LazyColumn dentro de otro scroll vertical hace fallar la app.
                        // key: Compose identifica cada tarjeta por su id, así las
                        // animaciones de estado no se mezclan entre pedidos.
                        pedidos.forEachIndexed { i, datos ->
                            key(datos.pedido.id) {
                                TarjetaPedido(
                                    datos,
                                    esUltimo = i == 0,
                                    cancelando = vm.cancelando,
                                    onCancelar = {
                                        vm.cancelarPedido(datos.pedido) { ok ->
                                            avisar(if (ok) MSG_PEDIDO_CANCELADO else MSG_FUERA_DE_PLAZO)
                                        }
                                    },
                                    onFueraDePlazo = { avisar(MSG_FUERA_DE_PLAZO) },
                                    onRetirado = vm::registrarRetiro,
                                )
                            }
                        }
                        Spacer(Modifier.height(12.dp))
                        Text(
                            "Puedes volver a esta pantalla para seguir el estado de tus pedidos.",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 13.sp,
                        )
                    }
                }
            }
        }
        SnackbarHost(snackbar, Modifier.align(Alignment.BottomCenter))
    }
}

@Composable
private fun TarjetaPedido(
    datos: PedidoWithPlatos,
    esUltimo: Boolean,
    cancelando: Boolean,
    onCancelar: () -> Unit,
    onFueraDePlazo: () -> Unit,
    onRetirado: (Pedido) -> Unit,
) {
    val p = datos.pedido
    // NUEVO (RF-13): un pedido cancelado no se destaca como activo.
    val cancelado = p.estadoPedido == "cancelado"
    val destacado = esUltimo && !cancelado
    val context = LocalContext.current
    var errorQr by remember { mutableStateOf<String?>(null) }
    Surface(
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(12.dp),
        // El pedido más reciente se destaca con el borde del color de acento.
        border = androidx.compose.foundation.BorderStroke(
            if (destacado) 1.5.dp else 1.dp,
            if (destacado) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
        ),
        modifier = Modifier.fillMaxWidth().padding(top = 10.dp),
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("#${p.numero}", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.weight(1f))
                // ANIMACIÓN: si el estado cambia (por ejemplo al pasar de
                // "En preparación" a "Listo"), la etiqueta hace crossfade.
                AnimatedContent(targetState = p.estadoPedido, label = "estadoPedido") { estado ->
                    Pill(textoEstadoPedido(estado), colorEstadoPedido(estado))
                }
                AnimatedContent(targetState = p.estadoPago, label = "estadoPago") { estado ->
                    Pill(textoEstadoPago(estado), colorEstadoPago(estado))
                }
            }
            Text(
                (if (destacado) "Último pedido · " else "") + p.fecha.fechaHora(),
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(8.dp))
            Text("A nombre de ${p.nombre}", fontSize = 15.sp)
            Text(
                "Retiro a las ${p.horaRetiro} h · ${p.formaPago}",
                fontSize = 15.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            // NUEVO (RF-13): cuenta regresiva y botón "Cancelar pedido" (15 s).
            CancelarPedido(
                pedido = p,
                cancelando = cancelando,
                onCancelar = onCancelar,
                onFueraDePlazo = onFueraDePlazo,
            )
            if (cancelado) {
                Text(
                    "Este pedido fue cancelado y no se preparará.",
                    color = colorEstadoPedido("cancelado"),
                    fontSize = 14.sp,
                    modifier = Modifier.padding(top = 10.dp),
                )
            }

            // Retiro con QR. CAMBIO (RF-14): al leer bien el QR el pedido queda
            // "entregado" y "pagado" (antes solo se abría el enlace de pago).
            // Un pedido cancelado o ya entregado no muestra el botón.
            if (ReglasPedido.puedeRetirarConQr(p.estadoPedido)) {
                val pagaEnTienda = p.formaPago == "Pagar en tienda" && p.estadoPago == "pendiente"
                Spacer(Modifier.height(12.dp))
                Text(
                    if (pagaEnTienda) "Pago pendiente: puedes pagar al retirar usando el QR de la tienda."
                    else "Al retirar, escanea el QR de la tienda para confirmar la entrega.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 14.sp,
                )
                Spacer(Modifier.height(8.dp))
                Boton(
                    if (pagaEnTienda) "Escanear QR para pagar" else "Escanear QR de retiro",
                    onClick = {
                        errorQr = null
                        GmsBarcodeScanning.getClient(context)
                            .startScan()
                            .addOnSuccessListener { resultado ->
                                val valor = resultado.rawValue.orEmpty()
                                val enlace = runCatching { Uri.parse(valor) }.getOrNull()
                                if (enlace?.scheme == "https" && !enlace.host.isNullOrBlank()) {
                                    onRetirado(p)
                                    // Igual que antes: si paga en tienda, se abre la plataforma de pago.
                                    if (pagaEnTienda) {
                                        context.startActivity(Intent(Intent.ACTION_VIEW, enlace))
                                    }
                                } else {
                                    errorQr = "El QR no contiene un enlace web de pago"
                                }
                            }
                            .addOnFailureListener {
                                errorQr = "No se pudo leer el código QR"
                            }
                    },
                    estilo = EstiloBoton.Fantasma,
                )
                errorQr?.let {
                    Text(it, color = MaterialTheme.colorScheme.error, fontSize = 13.sp)
                }
            }

            Spacer(Modifier.height(10.dp))
            datos.platos.forEach { plato ->
                Text(
                    "• ${plato.nombreProteina}" +
                            if (plato.solo) " (solo proteína)" else " — ${plato.acompanamientos.joinToString(" · ")}",
                    fontSize = 14.sp,
                )
            }
            if (p.indicaciones.isNotBlank()) {
                Spacer(Modifier.height(8.dp))
                Text(
                    "Indicaciones: ${p.indicaciones}",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            Spacer(Modifier.height(10.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = androidx.compose.foundation.layout.Arrangement.End) {
                Text(p.total.money(), fontWeight = FontWeight.Bold, fontSize = 17.sp)
            }
        }
    }
}
