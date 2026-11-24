package cl.duoc.casajojo.ui.screens.cliente

import cl.duoc.casajojo.domain.model.FormaPago
import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cl.duoc.casajojo.data.local.entity.PedidoWithPlatos
import cl.duoc.casajojo.ui.layouts.BarraSuperior
import cl.duoc.casajojo.ui.components.Badge
import cl.duoc.casajojo.ui.components.Boton
import cl.duoc.casajojo.domain.usecase.QrRetiro
import cl.duoc.casajojo.domain.usecase.ReglasPedido
import cl.duoc.casajojo.ui.components.CancelarPedido
import cl.duoc.casajojo.ui.components.EditarPedidoDialogo
import cl.duoc.casajojo.ui.components.ErrorEcho
import cl.duoc.casajojo.ui.components.FotoPlato
import cl.duoc.casajojo.ui.components.MSG_FUERA_DE_PLAZO
import cl.duoc.casajojo.ui.components.MSG_PEDIDO_CANCELADO
import cl.duoc.casajojo.ui.components.LineaEstado
import cl.duoc.casajojo.ui.components.Pill
import cl.duoc.casajojo.ui.components.Titulo
import cl.duoc.casajojo.ui.components.Volver
import cl.duoc.casajojo.utils.colorEstadoPago
import cl.duoc.casajojo.utils.colorEstadoPedido
import cl.duoc.casajojo.utils.money
import cl.duoc.casajojo.utils.textoEstadoPago
import cl.duoc.casajojo.utils.textoEstadoPedido
import cl.duoc.casajojo.ui.layouts.Pantalla
import cl.duoc.casajojo.ui.theme.Ac
import cl.duoc.casajojo.ui.viewmodels.ClienteViewModel
import com.google.mlkit.vision.codescanner.GmsBarcodeScanning

/**
 * PASO 6 — Mi pedido (RF-19).
 * Igual que antes: suscripción a Room, sincronización al abrir y escaneo de QR.
 * Cambia la presentación: imagen del plato, línea de estado y tarjeta más viva.
 */
@Composable
fun ViewMiPedido(
    vm: ClienteViewModel,
    onVolver: () -> Unit,
    onCart: () -> Unit,
    onEmpezar: () -> Unit,
    onHistorial: () -> Unit,
) {
    val pedido by vm.ultimoPedido.collectAsState()
    LaunchedEffect(Unit) { vm.sincronizarUltimoPedido() }

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
                Spacer(Modifier.height(16.dp))
                Titulo("Mi pedido", "Sigue tu pedido en tiempo real")
                Spacer(Modifier.height(18.dp))

                val actual = pedido
                if (actual == null) {
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = MaterialTheme.colorScheme.secondaryContainer,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Column(Modifier.padding(20.dp)) {
                            Text("🧾", fontSize = 28.sp)
                            Spacer(Modifier.height(8.dp))
                            Text(
                                "Aún no tienes pedidos registrados en este dispositivo.",
                                color = MaterialTheme.colorScheme.onSecondaryContainer,
                                fontSize = 14.sp,
                            )
                        }
                    }
                    Spacer(Modifier.height(20.dp))
                    Boton("Hacer un pedido", onClick = onEmpezar, estilo = "acento")
                } else {
                    TarjetaPedido(actual, vm)
                    Spacer(Modifier.height(14.dp))

                    // RF-13: cancelación con cuenta regresiva de 15 s.
                    CancelarPedido(
                        pedido = actual.pedido,
                        cancelando = vm.cancelando,
                        onCancelar = {
                            vm.cancelarPedido(actual.pedido) { ok ->
                                if (ok) avisar(MSG_PEDIDO_CANCELADO) else avisar(MSG_FUERA_DE_PLAZO)
                            }
                        },
                        onFueraDePlazo = { avisar(MSG_FUERA_DE_PLAZO) },
                    )
                    // EFT / RF-12: editar los datos del retiro mientras siga RECIBIDO.
                    if (ReglasPedido.puedeEditar(actual.pedido.estadoPedido)) {
                        Spacer(Modifier.height(10.dp))
                        Boton(
                            "✏️  Editar pedido",
                            onClick = { vm.abrirEdicion(actual.pedido) },
                            estilo = "fantasma",
                        )
                    }
                    Spacer(Modifier.height(12.dp))

                    // M10: el cliente ya NO puede marcar su pedido como "listo": ese
                    // cambio le corresponde a la cocina. Aquí solo se muestra el estado,
                    // que se sincroniza desde ms-ventas al abrir la pantalla.
                    val estado = actual.pedido.estadoPedido

                    Text(
                        "Estado: ${textoEstadoPedido(estado)} · Pago: ${textoEstadoPago(actual.pedido.estadoPago)}. " +
                                "Puedes consultar esta pantalla para seguir tu pedido.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 12.sp,
                    )

                    Spacer(Modifier.height(12.dp))
                    Boton("Ver historial de pedidos", onClick = onHistorial, estilo = "fantasma")
                }
            }
        }
    }
        SnackbarHost(snackbar, Modifier.align(Alignment.BottomCenter))
    }

    // EFT: errores del retiro con QR o de la edición (mensajes de CasaJojoException).
    ErrorEcho(vm.error) { vm.limpiarError() }

    if (vm.edicion.abierta) {
        EditarPedidoDialogo(
            estado = vm.edicion,
            formasPago = FORMAS_PAGO,
            onNombre = vm::onEdicionNombre,
            onHora = vm::onEdicionHora,
            onFormaPago = vm::onEdicionFormaPago,
            onIndicaciones = vm::onEdicionIndicaciones,
            onGuardar = { vm.guardarEdicion(::avisar) },
            onCerrar = vm::cerrarEdicion,
        )
    }
}

@Composable
private fun TarjetaPedido(datos: PedidoWithPlatos, vm: ClienteViewModel) {
    val p = datos.pedido
    val context = LocalContext.current
    var errorQr by remember { mutableStateOf<String?>(null) }

    Surface(
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(22.dp),
        shadowElevation = 4.dp,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column {
            Box(Modifier.padding(9.dp)) {
                FotoPlato(
                    datos.platos.firstOrNull()?.nombreProteina ?: "Tu pedido",
                    alto = 132.dp,
                    redondeo = 16.dp,
                )
                Box(
                    Modifier.matchParentSize().padding(12.dp),
                    contentAlignment = Alignment.BottomStart,
                ) {
                    Badge("La Casa JoJo")
                }
            }

            Column(Modifier.padding(horizontal = 18.dp, vertical = 16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "#${p.numero}",
                        fontSize = 26.sp,
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.weight(1f),
                    )
                    AnimatedContent(targetState = p.estadoPedido, label = "estadoPedido") { estado ->
                        Pill(textoEstadoPedido(estado), colorEstadoPedido(estado))
                    }
                    AnimatedContent(targetState = p.estadoPago, label = "estadoPago") { estado ->
                        Pill(textoEstadoPago(estado), colorEstadoPago(estado))
                    }
                }
                Spacer(Modifier.height(10.dp))
                Text("A nombre de ${p.nombre}", fontSize = 15.sp, fontWeight = FontWeight.Bold)
                Text(
                    "Retiro a las ${p.horaRetiro} h · ${p.formaPago}",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                Spacer(Modifier.height(20.dp))
                LineaEstado(p.estadoPedido)

                // M12: el retiro con QR ya no depende de la forma de pago. Antes un pedido
                // con "Transferencia" nunca podía marcarse entregado desde la app.
                // EFT (POO): la forma de pago sabe si se paga al retirar.
                val pagaEnTienda = FormaPago.desde(p.formaPago).pagaAlRetirar() && p.estadoPago == "pendiente"
                if (ReglasPedido.puedeRetirarConQr(p.estadoPedido) || pagaEnTienda) {
                    Spacer(Modifier.height(4.dp))
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = MaterialTheme.colorScheme.secondaryContainer,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Column(Modifier.padding(14.dp)) {
                            Text(
                                if (ReglasPedido.puedeRetirarConQr(p.estadoPedido) && pagaEnTienda) {
                                    "Tu pedido está listo: escanea el QR de la tienda para pagar y retirar."
                                } else if (ReglasPedido.puedeRetirarConQr(p.estadoPedido)) {
                                    "Tu pedido está listo: escanea el QR de la tienda al retirarlo."
                                } else {
                                    "Pago pendiente: podrás escanear el QR de la tienda cuando tu pedido esté listo."
                                },
                                color = MaterialTheme.colorScheme.onSecondaryContainer,
                                fontSize = 13.sp,
                            )
                            // Fase 2: el retiro solo procede con el pedido LISTO (regla del servidor).
                            if (ReglasPedido.puedeRetirarConQr(p.estadoPedido)) {
                                Spacer(Modifier.height(10.dp))
                                Boton(
                                    if (pagaEnTienda) "Escanear QR para pagar" else "Escanear QR de retiro",
                                    onClick = {
                                        errorQr = null
                                        GmsBarcodeScanning.getClient(context)
                                            .startScan()
                                            .addOnSuccessListener { resultado ->
                                                val valor = resultado.rawValue
                                                // A5: ya NO se abre ninguna URL. Solo se acepta el QR propio
                                                // de la tienda (casajojo://retiro?codigo=...); el servidor
                                                // valida el código y deja el pedido pagado y entregado.
                                                if (QrRetiro.codigo(valor) != null) {
                                                    vm.registrarRetiro(p, valor)
                                                } else {
                                                    errorQr = "Este QR no es de La Casa JoJo"
                                                }
                                            }
                                            .addOnFailureListener {
                                                errorQr = "No se pudo leer el código QR"
                                            }
                                    },
                                    estilo = "fantasma",
                                )
                            }
                            errorQr?.let {
                                Text(it, color = MaterialTheme.colorScheme.error, fontSize = 12.sp)
                            }
                        }
                    }
                }

                Spacer(Modifier.height(18.dp))
                Text("Tu pedido", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Spacer(Modifier.height(6.dp))
                datos.platos.forEach { plato ->
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.background,
                        modifier = Modifier.fillMaxWidth().padding(top = 6.dp),
                    ) {
                        Text(
                            "• ${plato.nombreProteina}" +
                                    if (plato.solo) " (solo proteína)" else " — ${plato.acompanamientos.joinToString(" · ")}",
                            fontSize = 13.sp,
                            modifier = Modifier.padding(11.dp),
                        )
                    }
                }
                if (p.indicaciones.isNotBlank()) {
                    Spacer(Modifier.height(10.dp))
                    Text(
                        "Indicaciones: ${p.indicaciones}",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                Spacer(Modifier.height(16.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    Surface(
                        shape = RoundedCornerShape(99.dp),
                        color = Ac,
                    ) {
                        Text(
                            p.total.money(),
                            color = androidx.compose.ui.graphics.Color.White,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 16.sp,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        )
                    }
                }
            }
        }
    }
}
