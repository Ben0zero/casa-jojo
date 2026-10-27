package cl.duoc.casajojo.ui.screens.auth

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
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
import cl.duoc.casajojo.ui.layouts.Pantalla
import cl.duoc.casajojo.ui.viewmodels.AuthViewModel
import cl.duoc.casajojo.ui.components.Regla

/**
 * Pantalla de inicio de sesión. Es la primera que se ve si no hay sesión guardada.
 *
 * La pantalla solo DIBUJA la ficha (LoginUiState) que le entrega el ViewModel
 * y le AVISA lo que hace la persona (escribir, tocar Ingresar). No valida ni
 * guarda nada por su cuenta.
 */
@Composable
fun LoginScreen(
    vm: AuthViewModel,
    onIngresar: () -> Unit,
    onIrRegistro: () -> Unit,
) {
    val estado by vm.login.collectAsState()
    val snackbar = remember { SnackbarHostState() }
    val foco = LocalFocusManager.current

    // Cuando el ViewModel publica un mensaje, se muestra en el Snackbar y se limpia
    // (si no se limpiara, se volvería a mostrar al redibujar).
    LaunchedEffect(estado.mensaje) {
        estado.mensaje?.let {
            snackbar.showSnackbar(it)
            vm.limpiarMensajeLogin()
        }
    }

    val enviar = {
        foco.clearFocus() // cierra el teclado
        vm.iniciarSesion(onExito = onIngresar)
    }

    Box(Modifier.fillMaxSize()) {
        Pantalla {
            Column(Modifier.padding(top = 32.dp)) {
                Row {
                    Text("La Casa ", fontSize = 32.sp, fontWeight = FontWeight.Bold)
                    Text(
                        "JoJo",
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
                Regla()
                Text(
                    "Inicia sesión para armar tu menú y seguir tus pedidos.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 17.sp,
                )

                CampoTexto(
                    etiqueta = "Correo",
                    valor = estado.correo,
                    onCambio = vm::onLoginCorreo,
                    error = estado.errores.correo,
                    placeholder = "nombre@correo.cl",
                    tipoTeclado = KeyboardType.Email,
                    enabled = !estado.cargando,
                )
                CampoTexto(
                    etiqueta = "Contraseña",
                    valor = estado.password,
                    onCambio = vm::onLoginPassword,
                    error = estado.errores.password,
                    esContrasena = true,
                    accionIme = ImeAction.Done,
                    enabled = !estado.cargando,
                    onListo = enviar,
                )

                Spacer(Modifier.height(24.dp))
                // Mientras se verifica, el botón cambia de texto y queda desactivado.
                Boton(
                    texto = if (estado.cargando) "Ingresando…" else "Ingresar",
                    onClick = enviar,
                    estilo = "acento",
                    enabled = !estado.cargando,
                )

                Spacer(Modifier.height(18.dp))
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = androidx.compose.foundation.layout.Arrangement.Center,
                ) {
                    Text("¿No tienes cuenta? ", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(
                        "Crear cuenta",
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.clickable(enabled = !estado.cargando, onClick = onIrRegistro),
                    )
                }
            }
        }
        // SnackbarHost es el "hueco" donde aparece el aviso; sin él, el mensaje no se ve.
        SnackbarHost(
            hostState = snackbar,
            modifier = Modifier.align(Alignment.BottomCenter).padding(16.dp),
        )
    }
}