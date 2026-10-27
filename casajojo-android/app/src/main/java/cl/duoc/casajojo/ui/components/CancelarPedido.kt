package cl.duoc.casajojo.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cl.duoc.casajojo.data.local.entity.Pedido
import cl.duoc.casajojo.domain.usecase.ReglasPedido
import kotlinx.coroutines.delay

/** Mensajes del Snackbar al terminar (los usan Confirmado y Mi pedido). */
const val MSG_PEDIDO_CANCELADO = "Pedido cancelado"
const val MSG_FUERA_DE_PLAZO = "Ya no es posible cancelar este pedido"

/**
 * NUEVO (RF-13): botón "Cancelar pedido" con cuenta regresiva de 15 s.
 *
 * El tiempo se calcula con Pedido.fecha (guardada en Room) y la hora actual,
 * no con un contador en memoria: si el usuario sale, vuelve o gira el teléfono,
 * el tiempo restante sigue siendo el correcto.
 *
 * onCancelar se llama solo después de confirmar en el diálogo y de volver a
 * revisar el plazo; si ya pasó, se llama onFueraDePlazo.
 */
@Composable
fun CancelarPedido(
    pedido: Pedido,
    cancelando: Boolean,
    onCancelar: () -> Unit,
    onFueraDePlazo: () -> Unit,
) {
    // "ahora" se refresca cada 100 ms mientras dure el plazo (barra fluida).
    var ahora by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(pedido.id, pedido.fecha, pedido.estadoPedido) {
        while (true) {
            ahora = System.currentTimeMillis()
            if (!ReglasPedido.puedeCancelar(pedido.fecha, ahora, pedido.estadoPedido)) break
            delay(100)
        }
    }

    val puede = ReglasPedido.puedeCancelar(pedido.fecha, ahora, pedido.estadoPedido)
    val restanteMs = ReglasPedido.msParaCancelar(pedido.fecha, ahora)
    // Redondeo hacia arriba: se ve "15 s" al inicio y "1 s" en el último segundo.
    val segundos = ((restanteMs + 999) / 1000).toInt()

    // Se recuerda (también al girar) si esta tarjeta mostró el botón, para avisar
    // "ya está en preparación" solo cuando el plazo se acaba frente al usuario,
    // y no en pedidos antiguos.
    var mostroBoton by rememberSaveable(pedido.id) { mutableStateOf(false) }
    LaunchedEffect(puede) { if (puede) mostroBoton = true }

    var preguntar by rememberSaveable(pedido.id) { mutableStateOf(false) }

    Column(Modifier.fillMaxWidth()) {
        // ANIMACIÓN: al acabarse el plazo, el bloque se pliega y desaparece.
        AnimatedVisibility(
            visible = puede,
            enter = fadeIn(tween(200)) + expandVertically(tween(250)),
            exit = fadeOut(tween(250)) + shrinkVertically(tween(300)),
        ) {
            Column(Modifier.padding(top = 12.dp)) {
                Text(
                    "Puedes cancelar en $segundos s",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(6.dp))
                // La barra se vacía a medida que pasa el tiempo.
                LinearProgressIndicator(
                    progress = { restanteMs.toFloat() / ReglasPedido.PLAZO_CANCELACION_MS },
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.error,
                )
                Spacer(Modifier.height(10.dp))
                Boton(
                    texto = if (cancelando) "Cancelando…" else "Cancelar pedido",
                    onClick = { preguntar = true },
                    estilo = EstiloBoton.Fantasma,
                    enabled = !cancelando,
                    icono = Icons.Filled.Close,
                )
            }
        }
        AnimatedVisibility(
            visible = !puede && mostroBoton && pedido.estadoPedido == "recibido",
            enter = fadeIn(tween(300)) + expandVertically(tween(300)),
        ) {
            Text(
                "Tu pedido ya está en preparación",
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(top = 12.dp),
            )
        }
    }

    if (preguntar) {
        AlertDialog(
            onDismissRequest = { preguntar = false },
            title = { Text("¿Cancelar el pedido #${pedido.numero}?") },
            text = { Text("El pedido no se preparará. Esta acción no se puede deshacer.") },
            confirmButton = {
                TextButton(onClick = {
                    preguntar = false
                    // Se vuelve a revisar el plazo: el diálogo pudo quedar abierto
                    // más allá de los 15 s.
                    val ahoraConfirmar = System.currentTimeMillis()
                    if (ReglasPedido.puedeCancelar(pedido.fecha, ahoraConfirmar, pedido.estadoPedido)) {
                        onCancelar()
                    } else {
                        onFueraDePlazo()
                    }
                }) {
                    Text("Sí, cancelar", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { preguntar = false }) { Text("No") }
            },
        )
    }
}
