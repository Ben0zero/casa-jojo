package cl.duoc.casajojo.ui.screens.cliente

import cl.duoc.casajojo.domain.model.FormaPago
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import cl.duoc.casajojo.ui.components.Boton
import cl.duoc.casajojo.ui.components.CampoTexto
import cl.duoc.casajojo.ui.components.ChipRow
import cl.duoc.casajojo.ui.components.ErrorEcho
import cl.duoc.casajojo.ui.components.Nota
import cl.duoc.casajojo.ui.components.PanelTotal
import cl.duoc.casajojo.ui.components.PasoBar
import cl.duoc.casajojo.ui.components.PedirPermisoNotificaciones
import cl.duoc.casajojo.ui.components.Titulo
import cl.duoc.casajojo.ui.components.TituloBloque
import cl.duoc.casajojo.ui.components.Volver
import cl.duoc.casajojo.ui.layouts.Pantalla
import cl.duoc.casajojo.ui.viewmodels.ClienteViewModel
import cl.duoc.casajojo.domain.usecase.ReglasPedido
import java.time.LocalTime

/** EFT (POO): las opciones salen de la jerarquía FormaPago. */
internal val FORMAS_PAGO: List<String> get() = FormaPago.etiquetas

/**
 * PASO 4 — Retiro y pago.
 * Toda la lógica (permiso, prellenado, chips, total y confirmación) se conserva;
 * cambia solo la presentación: bloques con encabezado y panel de total rojo.
 */
@Composable
fun ViewRetiro(
    vm: ClienteViewModel,
    onVolver: () -> Unit,
    onConfirmar: () -> Unit,
) {
    ErrorEcho(vm.error) { vm.limpiarError() }

    PedirPermisoNotificaciones()

    val usuario by vm.usuario.collectAsState()
    LaunchedEffect(usuario) { vm.prellenarNombre(usuario?.nombre) }

    // B7: solo horas que todavía no pasan; si la elegida ya pasó, se toma la primera libre.
    val horas = remember { ReglasPedido.horasDisponibles(LocalTime.now()) }
    LaunchedEffect(horas) {
        if (horas.isNotEmpty() && vm.retiro.horaRetiro !in horas) vm.onHoraRetiro(horas.first())
    }

    Pantalla {
        Column {
            Volver(onVolver)
            Spacer(Modifier.height(14.dp))
            PasoBar(3)
            Titulo("Retiro y pago", "Casi listo: dinos cómo y cuándo retiras.")
            Spacer(Modifier.height(20.dp))

            // Portado desde android: error bajo el campo y ✓ al validar.
            CampoTexto(
                etiqueta = "Nombre para el retiro",
                valor = vm.retiro.nombre,
                onCambio = vm::onNombre,
                error = vm.retiro.errores.nombre,
                placeholder = "Nombre para el retiro",
                enabled = !vm.enRegistro,
            )

            Spacer(Modifier.height(20.dp))
            TituloBloque("⏰", "Hora de retiro")
            Spacer(Modifier.height(10.dp))
            if (horas.isEmpty()) {
                Nota("Por hoy ya no quedan horarios de retiro (último retiro: ${ReglasPedido.HORAS_RETIRO.last()}).")
            } else {
                ChipRow(horas, vm.retiro.horaRetiro, vm::onHoraRetiro)
            }

            Spacer(Modifier.height(20.dp))
            TituloBloque("💳", "Forma de pago")
            Spacer(Modifier.height(10.dp))
            ChipRow(FORMAS_PAGO, vm.retiro.formaPago, vm::onFormaPago)

            Spacer(Modifier.height(20.dp))
            TituloBloque("📝", "Indicaciones (opcional)")
            OutlinedTextField(
                value = vm.retiro.indicaciones,
                onValueChange = vm::onIndicaciones,
                placeholder = { Text("Ej.: sin ají, bien cocido") },
                modifier = Modifier.fillMaxWidth(),
                maxLines = 3,
            )

            Nota(
                "El método de pago se usará al retirar el pedido. Si elegiste pagar en tienda, podrás escanear el QR desde Mi pedido cuando esté listo."
            )

            Spacer(Modifier.height(24.dp))
            PanelTotal(valor = vm.total, etiqueta = "Total a pagar")
            Spacer(Modifier.height(16.dp))
            Boton(
                texto = if (vm.enRegistro) "Registrando…" else "Confirmar pedido",
                onClick = onConfirmar,
                estilo = "acento",
                enabled = !vm.enRegistro,
            )
        }
    }
}
