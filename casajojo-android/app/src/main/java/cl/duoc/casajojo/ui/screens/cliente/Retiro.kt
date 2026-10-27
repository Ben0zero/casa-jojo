package cl.duoc.casajojo.ui.screens.cliente

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cl.duoc.casajojo.ui.components.Boton
import cl.duoc.casajojo.ui.components.CampoTexto
import cl.duoc.casajojo.ui.components.ChipRow
import cl.duoc.casajojo.data.local.entity.Usuario
import cl.duoc.casajojo.ui.components.ErrorEcho
import cl.duoc.casajojo.ui.components.EstiloBoton
import cl.duoc.casajojo.ui.components.PasoBar
import cl.duoc.casajojo.ui.components.PedirPermisoNotificaciones
import cl.duoc.casajojo.ui.components.Titulo
import cl.duoc.casajojo.ui.components.TotalAnimado
import cl.duoc.casajojo.ui.components.Volver
import cl.duoc.casajojo.ui.layouts.Pantalla
import cl.duoc.casajojo.ui.components.Nota
import cl.duoc.casajojo.ui.viewmodels.ClienteViewModel
import com.google.mlkit.vision.codescanner.GmsBarcodeScanning

private val HORAS_RETIRO = listOf("12:00", "12:30", "13:00", "13:30", "14:00", "14:30", "15:00")
private val FORMAS_PAGO = listOf("Transferencia", "Pagar al retirar")

/**
 * PASO 4 — Retiro y pago.
 * El cliente indica a nombre de quién, la hora de retiro, cómo pagará
 * (solo como anotación) e indicaciones opcionales.
 */
@Composable
fun ViewRetiro(
    vm: ClienteViewModel,
    usuario: Usuario?,
    onVolver: () -> Unit,
    onConfirmar: () -> Unit,
) {
    ErrorEcho(vm.error) { vm.limpiarError() }

    // RECURSO NATIVO: se pide el permiso de notificaciones aquí (y no al abrir
    // la app) porque es el momento en que el aviso tiene sentido para el usuario.
    PedirPermisoNotificaciones()
    val context = androidx.compose.ui.platform.LocalContext.current
    // El QR leído vive en el ViewModel (vm.retiro.codigoQr); el error de lectura
    // es solo un aviso de esta pantalla (rememberSaveable: sobrevive al girar).
    var errorQr by rememberSaveable { mutableStateOf<String?>(null) }

    // NUEVO (fase 1): al entrar, el nombre se rellena con el de la cuenta.
    // LaunchedEffect(usuario) corre cuando llega el usuario desde DataStore/Room.
    LaunchedEffect(usuario) { vm.prellenarNombre(usuario?.nombre) }

    val retiro = vm.retiro

    Pantalla {
        Column {
            Volver(onVolver)
            PasoBar(3)
            Titulo("Retiro y pago")

            // CAMBIO (P1): mismo campo que en el registro: ícono, error bajo el
            // campo y ✓ cuando ya se validó y está correcto.
            CampoTexto(
                etiqueta = "Nombre para el retiro",
                valor = retiro.nombre,
                onCambio = vm::onNombre,
                error = retiro.errores.nombre,
                placeholder = "Nombre para el retiro",
                icono = Icons.Filled.Person,
                validado = retiro.intentado,
                enabled = !vm.enRegistro,
            )

            Text("Hora de retiro", fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(top = 18.dp, bottom = 8.dp))
            ChipRow(HORAS_RETIRO, retiro.horaRetiro, vm::onHoraRetiro)

            Text("Forma de pago", fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(top = 18.dp, bottom = 8.dp))
            ChipRow(FORMAS_PAGO, retiro.formaPago, vm::onFormaPago)

            if (retiro.formaPago == "Transferencia") {
                Spacer(Modifier.height(12.dp))
                Boton(
                    "Escanear QR de transferencia",
                    onClick = {
                        errorQr = null
                        GmsBarcodeScanning.getClient(context)
                            .startScan()
                            .addOnSuccessListener { resultado ->
                                vm.onCodigoQr(resultado.rawValue ?: "QR leído sin texto")
                            }
                            .addOnFailureListener {
                                errorQr = "No se pudo leer el código QR"
                            }
                    },
                    estilo = EstiloBoton.Fantasma,
                )
                retiro.codigoQr?.let {
                    Text("QR leído: $it", color = androidx.compose.material3.MaterialTheme.colorScheme.primary, modifier = Modifier.padding(top = 8.dp))
                }
                errorQr?.let {
                    Text(it, color = androidx.compose.material3.MaterialTheme.colorScheme.error, modifier = Modifier.padding(top = 8.dp))
                }
            }

            Text("Indicaciones (opcional)", fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(top = 18.dp, bottom = 6.dp))
            OutlinedTextField(
                value = retiro.indicaciones,
                onValueChange = vm::onIndicaciones,
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
                // ANIMACIÓN: mismo contador animado que en el carrito.
                TotalAnimado(valor = vm.total)
            }
            Spacer(Modifier.height(14.dp))
            Boton(
                texto = if (vm.enRegistro) "Registrando…" else "Confirmar pedido",
                onClick = onConfirmar,
                estilo = EstiloBoton.Acento,
                enabled = !vm.enRegistro,
            )
        }
    }
}
