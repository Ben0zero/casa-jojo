package cl.duoc.casajojo.ui.screens.cliente

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cl.duoc.casajojo.ui.components.Boton
import cl.duoc.casajojo.ui.components.CampoTexto
import cl.duoc.casajojo.ui.components.TarjetaBloque
import cl.duoc.casajojo.ui.components.Titulo
import cl.duoc.casajojo.ui.components.TituloBloque
import cl.duoc.casajojo.ui.components.Volver
import cl.duoc.casajojo.ui.layouts.Pantalla
import cl.duoc.casajojo.ui.viewmodels.PerfilViewModel

/**
 * Configuración de cuenta (antes estaba dentro de Perfil). Misma lógica y mismas
 * validaciones de PerfilViewModel, contra ms-auth:
 * - "Mis datos" → PUT /api/usuarios/me (nombre y celular). El correo es solo lectura.
 * - "Cambiar contraseña" → PUT /api/usuarios/me/password.
 * - "Eliminar cuenta" → DELETE /api/usuarios/me (con diálogo de confirmación).
 */
@Composable
fun ViewConfiguracionCuenta(
    vm: PerfilViewModel,
    onVolver: () -> Unit,
    onCuentaEliminada: () -> Unit,
    barra: @Composable () -> Unit,
) {
    val foco = LocalFocusManager.current
    val estado by vm.estado.collectAsState()
    val snackbar = remember { SnackbarHostState() }

    LaunchedEffect(estado.mensaje) {
        estado.mensaje?.let {
            snackbar.showSnackbar(it)
            vm.limpiarMensaje()
        }
    }

    Box(Modifier.fillMaxSize()) {
        Pantalla(bar = barra) {
            Column(Modifier.fillMaxWidth()) {
                Volver(onVolver)
                Spacer(Modifier.height(18.dp))
                Titulo("Configuración de cuenta", "Tus datos, tu contraseña y tu cuenta")

                // ---------- Correo (solo lectura: identifica la cuenta en ms-auth) ----------
                TarjetaBloque {
                    TituloBloque("✉️", "Correo")
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier.fillMaxWidth().padding(top = 10.dp),
                    ) {
                        Text(
                            estado.correo.ifBlank { "—" },
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 14.dp),
                        )
                    }
                    Text(
                        "El correo no se puede cambiar: es tu usuario para ingresar.",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 6.dp),
                    )
                }

                // ---------- Mis datos (READ + UPDATE) ----------
                TarjetaBloque {
                    TituloBloque("👤", "Mis datos")
                    CampoTexto(
                        etiqueta = "Nombre",
                        valor = estado.nombre,
                        onCambio = vm::onNombre,
                        error = estado.erroresDatos.nombre,
                        placeholder = "Nombre y apellido",
                        enabled = !estado.guardandoDatos,
                        icono = "👤",
                    )
                    CampoTexto(
                        etiqueta = "Celular",
                        valor = estado.telefono,
                        onCambio = vm::onTelefono,
                        error = estado.erroresDatos.telefono,
                        placeholder = "9 1234 5678",
                        tipoTeclado = KeyboardType.Phone,
                        accionIme = ImeAction.Done,
                        enabled = !estado.guardandoDatos,
                        onListo = { foco.clearFocus(); vm.guardarDatos() },
                        icono = "📱",
                    )
                    Spacer(Modifier.height(16.dp))
                    Boton(
                        texto = if (estado.guardandoDatos) "Guardando…" else "💾  Guardar mis datos",
                        onClick = { foco.clearFocus(); vm.guardarDatos() },
                        estilo = "acento",
                        enabled = !estado.guardandoDatos,
                    )
                }

                // ---------- Cambiar contraseña (UPDATE) ----------
                TarjetaBloque {
                    TituloBloque("🔑", "Cambiar contraseña")
                    CampoTexto(
                        etiqueta = "Contraseña actual",
                        valor = estado.actual,
                        onCambio = vm::onActual,
                        error = estado.erroresPassword.actual,
                        esContrasena = true,
                        enabled = !estado.guardandoPassword,
                        icono = "🔒",
                    )
                    CampoTexto(
                        etiqueta = "Nueva contraseña",
                        valor = estado.nueva,
                        onCambio = vm::onNueva,
                        error = estado.erroresPassword.nueva,
                        placeholder = "Mínimo 6, con letras y números",
                        esContrasena = true,
                        enabled = !estado.guardandoPassword,
                        icono = "🔑",
                    )
                    CampoTexto(
                        etiqueta = "Repite la nueva contraseña",
                        valor = estado.confirmar,
                        onCambio = vm::onConfirmar,
                        error = estado.erroresPassword.confirmar,
                        esContrasena = true,
                        accionIme = ImeAction.Done,
                        enabled = !estado.guardandoPassword,
                        onListo = { foco.clearFocus(); vm.cambiarPassword() },
                        icono = "🔑",
                    )
                    Spacer(Modifier.height(16.dp))
                    Boton(
                        texto = if (estado.guardandoPassword) "Actualizando…" else "Actualizar contraseña",
                        onClick = { foco.clearFocus(); vm.cambiarPassword() },
                        enabled = !estado.guardandoPassword,
                    )
                }

                // ---------- Eliminar cuenta (DELETE) ----------
                TarjetaBloque {
                    TituloBloque("🗑️", "Eliminar cuenta")
                    Text(
                        "Se borrará tu cuenta del servidor y de este teléfono, junto con tu historial local y tus tarjetas guardadas.",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(12.dp))
                    Boton(
                        texto = if (estado.eliminando) "Eliminando…" else "Eliminar mi cuenta",
                        onClick = vm::pedirEliminar,
                        estilo = "fantasma",
                        enabled = !estado.eliminando,
                    )
                }
            }
        }
        SnackbarHost(snackbar, Modifier.align(Alignment.BottomCenter).padding(16.dp))
    }

    if (estado.confirmandoEliminar) {
        AlertDialog(
            onDismissRequest = vm::cancelarEliminar,
            icon = { Text("⚠️", fontSize = 28.sp) },
            title = { Text("¿Eliminar tu cuenta?") },
            text = {
                Text("Esta acción no se puede deshacer. Perderás tu sesión, tu historial y tus tarjetas en este teléfono.")
            },
            confirmButton = {
                TextButton(onClick = { vm.eliminarCuenta(onCuentaEliminada) }) {
                    Text("Sí, eliminar", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = vm::cancelarEliminar) { Text("Cancelar") }
            },
        )
    }
}
