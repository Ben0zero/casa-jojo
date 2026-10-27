package cl.duoc.casajojo.ui.screens.cliente

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cl.duoc.casajojo.data.local.entity.PedidoWithPlatos
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
                            TarjetaPedido(datos, esUltimo = i == 0)
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
}

@Composable
private fun TarjetaPedido(datos: PedidoWithPlatos, esUltimo: Boolean) {
    val p = datos.pedido
    val context = LocalContext.current
    var errorQr by remember { mutableStateOf<String?>(null) }
    Surface(
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(12.dp),
        // El pedido más reciente se destaca con el borde del color de acento.
        border = androidx.compose.foundation.BorderStroke(
            if (esUltimo) 1.5.dp else 1.dp,
            if (esUltimo) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
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
                (if (esUltimo) "Último pedido · " else "") + p.fecha.fechaHora(),
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

            if (p.formaPago == "Pagar en tienda" && p.estadoPago == "pendiente") {
                Spacer(Modifier.height(12.dp))
                Text(
                    "Pago pendiente: puedes pagar al retirar usando el QR de la tienda.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 14.sp,
                )
                Spacer(Modifier.height(8.dp))
                Boton(
                    "Escanear QR para pagar",
                    onClick = {
                        errorQr = null
                        GmsBarcodeScanning.getClient(context)
                            .startScan()
                            .addOnSuccessListener { resultado ->
                                val valor = resultado.rawValue.orEmpty()
                                val enlace = runCatching { Uri.parse(valor) }.getOrNull()
                                if (enlace?.scheme == "https" && !enlace.host.isNullOrBlank()) {
                                    context.startActivity(Intent(Intent.ACTION_VIEW, enlace))
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
