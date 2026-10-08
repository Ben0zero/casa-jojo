package cl.duoc.casajojo.ui.cliente

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cl.duoc.casajojo.ui.components.Boton
import cl.duoc.casajojo.ui.components.ChipRow
import cl.duoc.casajojo.ui.components.ErrorEcho
import cl.duoc.casajojo.ui.components.PasoBar
import cl.duoc.casajojo.ui.components.Titulo
import cl.duoc.casajojo.ui.components.Volver
import cl.duoc.casajojo.ui.formato.money

private val HORAS_RETIRO = listOf("12:00", "12:30", "13:00", "13:30", "14:00", "14:30", "15:00")
private val FORMAS_PAGO = listOf("Transferencia", "Pagar al retirar")

@Composable
fun ViewRetiro(
    vm: ClienteViewModel,
    onVolver: () -> Unit,
    onConfirmar: () -> Unit,
) {
    ErrorEcho(vm.error) { vm.limpiarError() }
    Pantalla {
        Column {
            Volver(onVolver)
            PasoBar(3)
            Titulo("Retiro y pago")

            Text("Tu nombre", fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(top = 18.dp, bottom = 6.dp))
            OutlinedTextField(
                value = vm.nombre,
                onValueChange = { vm.nombre = it },
                placeholder = { Text("Nombre para el retiro") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )

            Text("Hora de retiro", fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(top = 18.dp, bottom = 8.dp))
            ChipRow(HORAS_RETIRO, vm.horaRetiro, { vm.horaRetiro = it })

            Text("Forma de pago", fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(top = 18.dp, bottom = 8.dp))
            ChipRow(FORMAS_PAGO, vm.formaPago, { vm.formaPago = it })

            Text("Indicaciones (opcional)", fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(top = 18.dp, bottom = 6.dp))
            OutlinedTextField(
                value = vm.indicaciones,
                onValueChange = { vm.indicaciones = it },
                placeholder = { Text("Ej.: sin ají, bien cocido") },
                modifier = Modifier.fillMaxWidth(),
                maxLines = 3,
            )

            Nota(
                "Es una anotación: recuerda llevar el medio de pago que elijas al retirar tu pedido."
            )

            Spacer(Modifier.height(22.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = androidx.compose.foundation.layout.Arrangement.SpaceBetween) {
                Text("Total a pagar", fontWeight = FontWeight.Bold, fontSize = 20.sp)
                Text(vm.total.money(), fontWeight = FontWeight.Bold, fontSize = 20.sp)
            }
            Spacer(Modifier.height(14.dp))
            Boton(
                texto = if (vm.enRegistro) "Registrando…" else "Confirmar pedido",
                onClick = onConfirmar,
                estilo = "acento",
                enabled = !vm.enRegistro,
            )
        }
    }
}