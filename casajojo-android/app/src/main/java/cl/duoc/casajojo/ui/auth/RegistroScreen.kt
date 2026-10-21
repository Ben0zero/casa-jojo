package cl.duoc.casajojo.ui.auth

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import cl.duoc.casajojo.ui.cliente.Nota
import cl.duoc.casajojo.ui.cliente.Pantalla
import cl.duoc.casajojo.ui.components.Boton
import cl.duoc.casajojo.ui.components.CampoTexto
import cl.duoc.casajojo.ui.components.Titulo
import cl.duoc.casajojo.ui.components.Volver

/**
 * Pantalla de registro de cliente.
 * Al tocar "Crear cuenta", el ViewModel valida los 5 campos; si hay errores se
 * marcan en rojo bajo cada campo. Si todo está bien, crea la cuenta, inicia
 * sesión y onRegistrado lleva al inicio.
 */
@Composable
fun RegistroScreen(
    vm: AuthViewModel,
    onVolver: () -> Unit,
    onRegistrado: () -> Unit,
) {
    val estado by vm.registro.collectAsState()
    val snackbar = remember { SnackbarHostState() }
    val foco = LocalFocusManager.current

    LaunchedEffect(estado.mensaje) {
        estado.mensaje?.let {
            snackbar.showSnackbar(it)
            vm.limpiarMensajeRegistro()
        }
    }

    val enviar = {
        foco.clearFocus()
        vm.registrar(onExito = onRegistrado)
    }
    val activo = !estado.guardando

    Box(Modifier.fillMaxSize()) {
        Pantalla {
            Column {
                Volver(onVolver)
                Titulo("Crea tu cuenta", "Así guardamos tus pedidos y te avisamos si hace falta.")

                CampoTexto(
                    etiqueta = "Nombre",
                    valor = estado.nombre,
                    onCambio = vm::onRegistroNombre,
                    error = estado.errores.nombre,
                    placeholder = "Nombre y apellido",
                    enabled = activo,
                )
                CampoTexto(
                    etiqueta = "Correo",
                    valor = estado.correo,
                    onCambio = vm::onRegistroCorreo,
                    error = estado.errores.correo,
                    placeholder = "nombre@correo.cl",
                    tipoTeclado = KeyboardType.Email,
                    enabled = activo,
                )
                CampoTexto(
                    etiqueta = "Celular",
                    valor = estado.telefono,
                    onCambio = vm::onRegistroTelefono,
                    error = estado.errores.telefono,
                    placeholder = "9 1234 5678",
                    tipoTeclado = KeyboardType.Phone,
                    enabled = activo,
                )
                CampoTexto(
                    etiqueta = "Contraseña",
                    valor = estado.password,
                    onCambio = vm::onRegistroPassword,
                    error = estado.errores.password,
                    placeholder = "Mínimo 6, con letras y números",
                    esContrasena = true,
                    enabled = activo,
                )
                CampoTexto(
                    etiqueta = "Repite la contraseña",
                    valor = estado.confirmar,
                    onCambio = vm::onRegistroConfirmar,
                    error = estado.errores.confirmar,
                    esContrasena = true,
                    accionIme = ImeAction.Done,
                    enabled = activo,
                    onListo = enviar,
                )

                Nota("Tu contraseña se guarda cifrada en el teléfono; nadie puede leerla.")

                Spacer(Modifier.height(22.dp))
                Boton(
                    texto = if (estado.guardando) "Creando cuenta…" else "Crear cuenta",
                    onClick = enviar,
                    estilo = "acento",
                    enabled = activo,
                )
            }
        }
        SnackbarHost(
            hostState = snackbar,
            modifier = Modifier.align(Alignment.BottomCenter).padding(16.dp),
        )
    }
}