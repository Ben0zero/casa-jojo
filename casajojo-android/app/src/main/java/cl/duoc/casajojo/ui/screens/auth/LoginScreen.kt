package cl.duoc.casajojo.ui.screens.auth

import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cl.duoc.casajojo.ui.components.Boton
import cl.duoc.casajojo.ui.components.CampoTexto
import cl.duoc.casajojo.ui.layouts.Pantalla
import cl.duoc.casajojo.ui.theme.Ac
import cl.duoc.casajojo.ui.theme.Gold
import cl.duoc.casajojo.ui.theme.VerdeOscuro
import cl.duoc.casajojo.ui.viewmodels.AuthViewModel

/**
 * Login. Misma validación y mismo ViewModel; ahora con una cabecera roja con
 * sello "CJ" y el formulario sobre una tarjeta crema.
 */
@Composable
fun LoginScreen(
    vm: AuthViewModel,
    onIngresar: () -> Unit,
    onIrRegistro: () -> Unit,
    onOlvide: () -> Unit,
) {
    val estado by vm.login.collectAsState()
    val snackbar = remember { SnackbarHostState() }
    val foco = LocalFocusManager.current

    LaunchedEffect(estado.mensaje) {
        estado.mensaje?.let {
            snackbar.showSnackbar(it)
            vm.limpiarMensajeLogin()
        }
    }

    val enviar = {
        foco.clearFocus()
        vm.iniciarSesion(onExito = onIngresar)
    }

    Box(Modifier.fillMaxSize()) {
        Pantalla {
            Column(Modifier.padding(top = 8.dp)) {
                Surface(
                    shape = RoundedCornerShape(26.dp),
                    color = Ac,
                    shadowElevation = 6.dp,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Box(
                        Modifier
                            .background(Brush.linearGradient(listOf(Ac, VerdeOscuro)))
                            .padding(22.dp),
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(shape = CircleShape, color = Color.White) {
                                Box(Modifier.size(52.dp), contentAlignment = Alignment.Center) {
                                    Text("CJ", color = Ac, fontWeight = FontWeight.Black, fontSize = 20.sp)
                                }
                            }
                            Column(Modifier.padding(start = 12.dp)) {
                                Text(
                                    "LA CASA",
                                    color = Gold,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                )
                                Text(
                                    "JOJO",
                                    color = Color.White,
                                    fontSize = 24.sp,
                                    fontWeight = FontWeight.Black,
                                )
                            }
                        }
                    }
                }

                Spacer(Modifier.height(20.dp))
                Text(
                    "¡Qué rico verte de nuevo!",
                    fontSize = 26.sp,
                    fontWeight = FontWeight.Black,
                )
                Text(
                    "Inicia sesión para armar tu menú y seguir tus pedidos.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 14.sp,
                    modifier = Modifier.padding(top = 4.dp),
                )

                Surface(
                    shape = RoundedCornerShape(22.dp),
                    color = MaterialTheme.colorScheme.surface,
                    shadowElevation = 3.dp,
                    modifier = Modifier.fillMaxWidth().padding(top = 20.dp),
                ) {
                    Column(Modifier.padding(horizontal = 18.dp, vertical = 8.dp)) {
                        CampoTexto(
                            etiqueta = "Correo",
                            valor = estado.correo,
                            onCambio = vm::onLoginCorreo,
                            error = estado.errores.correo,
                            placeholder = "nombre@correo.cl",
                            tipoTeclado = KeyboardType.Email,
                            enabled = !estado.cargando,
                            icono = "✉️",
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
                            icono = "🔒",
                        )
                        // EFT: recuperación de contraseña (correo → código → nueva contraseña).
                        Text(
                            "¿Olvidaste tu contraseña?",
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            modifier = Modifier
                                .align(Alignment.End)
                                .clip(RoundedCornerShape(8.dp))
                                .clickable(enabled = !estado.cargando, onClick = onOlvide)
                                .minimumInteractiveComponentSize()
                                .padding(horizontal = 6.dp),
                        )
                        Spacer(Modifier.height(12.dp))
                        Boton(
                            texto = if (estado.cargando) "Ingresando…" else "Ingresar",
                            onClick = enviar,
                            estilo = "acento",
                            enabled = !estado.cargando,
                        )
                        Spacer(Modifier.height(14.dp))
                        Row(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = androidx.compose.foundation.layout.Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                "¿No tienes cuenta? ",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 13.sp,
                            )
                            Text(
                                "Crear cuenta",
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable(enabled = !estado.cargando, onClick = onIrRegistro)
                                    .minimumInteractiveComponentSize()
                                    .padding(horizontal = 6.dp),
                            )
                        }
                    }
                }
            }
        }
        SnackbarHost(
            hostState = snackbar,
            modifier = Modifier.align(Alignment.BottomCenter).padding(16.dp),
        )
    }
}
