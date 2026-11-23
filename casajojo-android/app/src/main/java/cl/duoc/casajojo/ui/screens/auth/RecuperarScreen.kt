package cl.duoc.casajojo.ui.screens.auth

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
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
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cl.duoc.casajojo.ui.components.Boton
import cl.duoc.casajojo.ui.components.CampoTexto
import cl.duoc.casajojo.ui.components.Nota
import cl.duoc.casajojo.ui.components.Titulo
import cl.duoc.casajojo.ui.components.Volver
import cl.duoc.casajojo.ui.layouts.Pantalla
import cl.duoc.casajojo.ui.theme.Ac
import cl.duoc.casajojo.ui.viewmodels.AuthViewModel
import cl.duoc.casajojo.ui.viewmodels.PasoRecuperacion

/**
 * EFT: "¿Olvidaste tu contraseña?" en dos pasos.
 *  1) Correo → ms-auth envía un código de 6 dígitos (vence en 15 min).
 *  2) Código + nueva contraseña → vuelve al login con el correo listo.
 * Toda la lógica (validación y llamadas) vive en AuthViewModel; aquí solo se dibuja.
 */
@Composable
fun RecuperarScreen(
    vm: AuthViewModel,
    onVolver: () -> Unit,
    onRestablecida: () -> Unit,
) {
    val estado by vm.recuperar.collectAsState()
    val snackbar = remember { SnackbarHostState() }
    val foco = LocalFocusManager.current

    LaunchedEffect(estado.mensaje) {
        estado.mensaje?.let {
            snackbar.showSnackbar(it)
            vm.limpiarMensajeRecuperar()
        }
    }
    val activo = !estado.cargando

    Box(Modifier.fillMaxSize()) {
        Pantalla {
            Column {
                Volver(onVolver)
                Spacer(Modifier.height(14.dp))
                Titulo("Recupera tu contraseña", "Te enviaremos un código a tu correo")

                Surface(
                    shape = RoundedCornerShape(22.dp),
                    color = MaterialTheme.colorScheme.surface,
                    shadowElevation = 3.dp,
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                ) {
                    AnimatedContent(targetState = estado.paso, label = "pasoRecuperacion") { paso ->
                        Column(Modifier.padding(horizontal = 18.dp, vertical = 8.dp)) {
                            when (paso) {
                                PasoRecuperacion.CORREO -> {
                                    Text("Paso 1 de 2", color = Ac, fontWeight = FontWeight.Bold, fontSize = 12.sp,
                                        modifier = Modifier.padding(top = 10.dp))
                                    CampoTexto(
                                        etiqueta = "Correo de tu cuenta",
                                        valor = estado.correo,
                                        onCambio = vm::onRecuperarCorreo,
                                        error = estado.erroresCorreo.correo,
                                        placeholder = "nombre@correo.cl",
                                        tipoTeclado = KeyboardType.Email,
                                        accionIme = ImeAction.Done,
                                        enabled = activo,
                                        onListo = { foco.clearFocus(); vm.enviarCodigo() },
                                        icono = "✉️",
                                    )
                                    Spacer(Modifier.height(20.dp))
                                    Boton(
                                        texto = if (estado.cargando) "Enviando código…" else "Enviar código",
                                        onClick = { foco.clearFocus(); vm.enviarCodigo() },
                                        estilo = "acento",
                                        enabled = activo,
                                    )
                                }
                                PasoRecuperacion.CODIGO -> {
                                    Text("Paso 2 de 2", color = Ac, fontWeight = FontWeight.Bold, fontSize = 12.sp,
                                        modifier = Modifier.padding(top = 10.dp))
                                    Text(
                                        "Revisa ${estado.correo.trim()}. El código vence en 15 minutos y sirve una sola vez.",
                                        fontSize = 13.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                    CampoTexto(
                                        etiqueta = "Código de 6 dígitos",
                                        valor = estado.codigo,
                                        onCambio = vm::onRecuperarCodigo,
                                        error = estado.errores.codigo,
                                        placeholder = "123456",
                                        tipoTeclado = KeyboardType.NumberPassword,
                                        enabled = activo,
                                        icono = "🔢",
                                    )
                                    CampoTexto(
                                        etiqueta = "Nueva contraseña",
                                        valor = estado.password,
                                        onCambio = vm::onRecuperarPassword,
                                        error = estado.errores.password,
                                        placeholder = "Mínimo 6, con letras y números",
                                        esContrasena = true,
                                        enabled = activo,
                                        icono = "🔒",
                                    )
                                    CampoTexto(
                                        etiqueta = "Repite la nueva contraseña",
                                        valor = estado.confirmar,
                                        onCambio = vm::onRecuperarConfirmar,
                                        error = estado.errores.confirmar,
                                        esContrasena = true,
                                        accionIme = ImeAction.Done,
                                        enabled = activo,
                                        onListo = { foco.clearFocus(); vm.restablecerPassword(onRestablecida) },
                                        icono = "🔒",
                                    )
                                    Spacer(Modifier.height(20.dp))
                                    Boton(
                                        texto = if (estado.cargando) "Guardando…" else "Cambiar contraseña",
                                        onClick = { foco.clearFocus(); vm.restablecerPassword(onRestablecida) },
                                        estilo = "acento",
                                        enabled = activo,
                                    )
                                    Spacer(Modifier.height(12.dp))
                                    Text(
                                        "¿No te llegó? Pedir otro código",
                                        color = Ac,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                        modifier = Modifier
                                            .align(Alignment.CenterHorizontally)
                                            .clickable(enabled = activo, onClick = vm::volverAPedirCodigo)
                                            .padding(8.dp),
                                    )
                                }
                            }
                            Spacer(Modifier.height(10.dp))
                        }
                    }
                }
                Nota("Por seguridad te respondemos lo mismo aunque el correo no tenga cuenta.")
            }
        }
        SnackbarHost(snackbar, Modifier.align(Alignment.BottomCenter).padding(16.dp))
    }
}
