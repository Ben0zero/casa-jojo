package cl.duoc.casajojo.ui.screens.auth

import androidx.compose.foundation.background
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
import cl.duoc.casajojo.ui.components.Nota
import cl.duoc.casajojo.ui.components.Volver
import cl.duoc.casajojo.ui.layouts.Pantalla
import cl.duoc.casajojo.ui.theme.Ac
import cl.duoc.casajojo.ui.theme.VerdeOscuro
import cl.duoc.casajojo.ui.viewmodels.AuthViewModel

/**
 * Registro: mismos cinco campos, misma validación y mismo flujo.
 * Solo cambia el "wrapper" visual: cabecera roja y tarjeta del formulario.
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
                Spacer(Modifier.height(14.dp))
                Surface(
                    shape = RoundedCornerShape(24.dp),
                    color = Ac,
                    shadowElevation = 5.dp,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Box(Modifier.background(Brush.linearGradient(listOf(Ac, VerdeOscuro))).padding(18.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(shape = CircleShape, color = Color.White) {
                                Box(Modifier.size(44.dp), contentAlignment = Alignment.Center) {
                                    Text("CJ", color = Ac, fontWeight = FontWeight.Black, fontSize = 17.sp)
                                }
                            }
                            Column(Modifier.padding(start = 12.dp)) {
                                Text(
                                    "Crea tu cuenta",
                                    color = Color.White,
                                    fontSize = 21.sp,
                                    fontWeight = FontWeight.Black,
                                )
                                Text(
                                    "Así guardamos tus pedidos y te avisamos si hace falta.",
                                    color = Color(0xFFE3F3E8),
                                    fontSize = 12.sp,
                                )
                            }
                        }
                    }
                }

                Surface(
                    shape = RoundedCornerShape(22.dp),
                    color = MaterialTheme.colorScheme.surface,
                    shadowElevation = 3.dp,
                    modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
                ) {
                    Column(Modifier.padding(horizontal = 18.dp, vertical = 6.dp)) {
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

                        Spacer(Modifier.height(20.dp))
                        Boton(
                            texto = if (estado.guardando) "Creando cuenta…" else "Crear cuenta",
                            onClick = enviar,
                            estilo = "acento",
                            enabled = activo,
                        )
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
