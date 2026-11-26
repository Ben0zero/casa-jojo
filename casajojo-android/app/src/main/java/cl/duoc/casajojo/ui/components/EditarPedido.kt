package cl.duoc.casajojo.ui.components

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cl.duoc.casajojo.domain.usecase.ReglasPedido
import cl.duoc.casajojo.ui.viewmodels.EdicionPedidoUiState
import java.time.LocalTime

/**
 * EFT / RF-12: diálogo para editar un pedido mientras sigue RECIBIDO: nombre del
 * retiro, hora, forma de pago e indicaciones. La validación y el guardado (ms-ventas
 * + Room) los hace ClienteViewModel; este componente solo dibuja la ficha.
 */
@Composable
fun EditarPedidoDialogo(
    estado: EdicionPedidoUiState,
    formasPago: List<String>,
    onNombre: (String) -> Unit,
    onHora: (String) -> Unit,
    onFormaPago: (String) -> Unit,
    onIndicaciones: (String) -> Unit,
    onGuardar: () -> Unit,
    onCerrar: () -> Unit,
    platos: List<String> = emptyList(),
) {
    val pedido = estado.pedido ?: return
    // Horas que aún no pasan; se conserva la hora actual del pedido aunque ya no esté en la lista.
    val horas = remember(pedido.id) {
        (ReglasPedido.horasDisponibles(LocalTime.now()) + pedido.horaRetiro).distinct().sorted()
    }

    AlertDialog(
        onDismissRequest = onCerrar,
        icon = { Text("✏️", fontSize = 26.sp) },
        title = { Text("Editar pedido #${pedido.numero}", fontWeight = FontWeight.Black) },
        text = {
            Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState())) {
                Text(
                    "Puedes cambiar estos datos mientras la cocina no empiece a prepararlo.",
                    fontSize = 13.sp,
                )
                if (platos.isNotEmpty()) {
                    Spacer(Modifier.height(12.dp))
                    // Solo lectura: recuerda qué platos lleva el pedido (no se editan aquí).
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                    ) {
                        platos.forEach { nombre ->
                            FotoPlato(
                                nombre,
                                alto = 52.dp,
                                redondeo = 12.dp,
                                modifier = Modifier.size(52.dp),
                            )
                        }
                    }
                }
                CampoTexto(
                    etiqueta = "Nombre para el retiro",
                    valor = estado.nombre,
                    onCambio = onNombre,
                    error = estado.errorNombre,
                    enabled = !estado.guardando,
                    icono = "👤",
                )
                Spacer(Modifier.height(14.dp))
                TituloBloque("⏰", "Hora de retiro")
                Spacer(Modifier.height(8.dp))
                ChipRow(horas, estado.horaRetiro, onHora)
                Spacer(Modifier.height(14.dp))
                TituloBloque("💳", "Forma de pago")
                Spacer(Modifier.height(8.dp))
                ChipRow(formasPago, estado.formaPago, onFormaPago)
                Spacer(Modifier.height(14.dp))
                TituloBloque("📝", "Indicaciones (opcional)")
                OutlinedTextField(
                    value = estado.indicaciones,
                    onValueChange = onIndicaciones,
                    enabled = !estado.guardando,
                    maxLines = 3,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onGuardar, enabled = !estado.guardando) {
                Text(if (estado.guardando) "Guardando…" else "Guardar cambios", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onCerrar, enabled = !estado.guardando) { Text("Cancelar") }
        },
    )
}
