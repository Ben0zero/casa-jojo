package cl.duoc.casajojo.ui.screens.cliente

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LifecycleResumeEffect
import cl.duoc.casajojo.ui.components.Boton
import cl.duoc.casajojo.ui.components.Pill
import cl.duoc.casajojo.ui.components.TarjetaBloque
import cl.duoc.casajojo.ui.components.Titulo
import cl.duoc.casajojo.ui.components.TituloBloque
import cl.duoc.casajojo.ui.components.Volver
import cl.duoc.casajojo.ui.components.abrirAjustesDeLaApp
import cl.duoc.casajojo.ui.layouts.Pantalla

/** true si la app tiene concedida la cámara. */
private fun tieneCamara(context: Context): Boolean =
    ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED

/** Notificaciones: en Android 13+ es un permiso; antes basta con que estén activadas. */
private fun tieneNotificaciones(context: Context): Boolean =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
    } else {
        NotificationManagerCompat.from(context).areNotificationsEnabled()
    }

/**
 * Privacidad: qué datos se guardan y dónde, en lenguaje simple; estado de los permisos
 * con acceso a los ajustes de Android; eliminar la foto de perfil y acceso a eliminar cuenta.
 */
@Composable
fun ViewPrivacidad(
    tieneFoto: Boolean,
    onEliminarFoto: () -> Unit,
    onEliminarCuenta: () -> Unit,
    onVolver: () -> Unit,
    barra: @Composable () -> Unit,
) {
    val context = LocalContext.current
    // Se vuelve a leer al regresar de Ajustes (onResume), así el estado queda al día.
    var revision by remember { mutableIntStateOf(0) }
    LifecycleResumeEffect(Unit) {
        revision++
        onPauseOrDispose { }
    }
    val camara = remember(revision) { tieneCamara(context) }
    val notificaciones = remember(revision) { tieneNotificaciones(context) }
    var confirmarFoto by rememberSaveable { mutableStateOf(false) }

    if (confirmarFoto) {
        AlertDialog(
            onDismissRequest = { confirmarFoto = false },
            icon = { Text("🖼️", fontSize = 26.sp) },
            title = { Text("¿Eliminar tu foto de perfil?", fontWeight = FontWeight.Black) },
            text = { Text("Se borrará de este teléfono y verás tus iniciales en el avatar.") },
            confirmButton = {
                TextButton(onClick = { confirmarFoto = false; onEliminarFoto() }) {
                    Text("Eliminar", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = { TextButton(onClick = { confirmarFoto = false }) { Text("Cancelar") } },
        )
    }

    Pantalla(bar = barra) {
        Column(Modifier.fillMaxWidth()) {
            Volver(onVolver)
            Spacer(Modifier.height(18.dp))
            Titulo("Privacidad", "Qué guardamos, dónde y cómo controlarlo")

            TarjetaBloque {
                TituloBloque("📱", "En tu teléfono")
                Punto("Base de datos local (Room): tus pedidos y tus tarjetas, guardadas enmascaradas (solo marca, últimos 4 dígitos y vencimiento; nunca el número completo ni el CVV).")
                Punto("Preferencias (DataStore): tu sesión, tus opciones de accesibilidad y la ubicación de tu foto de perfil.")
                Punto("Tu foto de perfil se guarda en la memoria privada de la app: otras apps no pueden verla.")
                Punto("La app no hace copias de seguridad en la nube (allowBackup desactivado).")
            }

            TarjetaBloque {
                TituloBloque("☁️", "En el servidor de La Casa JoJo")
                Punto("Tu cuenta (nombre, correo y celular) y tus pedidos, para que la cocina pueda prepararlos.")
                Punto("Tu contraseña se guarda cifrada (hash BCrypt): nadie puede leerla, ni siquiera nosotros.")
                Punto("Las tarjetas de la Billetera no se envían al servidor: es una simulación local.")
            }

            TarjetaBloque {
                TituloBloque("🔐", "Permisos")
                FilaPermiso("📸 Cámara", "Para tu foto de perfil", camara)
                FilaPermiso("🔔 Notificaciones", "Para avisarte el estado del pedido", notificaciones)
                Spacer(Modifier.height(12.dp))
                Boton("Administrar permisos", onClick = { abrirAjustesDeLaApp(context) }, estilo = "fantasma")
            }

            TarjetaBloque {
                TituloBloque("🧹", "Tus datos")
                Spacer(Modifier.height(12.dp))
                Boton(
                    if (tieneFoto) "Eliminar foto de perfil" else "No tienes foto de perfil",
                    onClick = { confirmarFoto = true },
                    estilo = "fantasma",
                    enabled = tieneFoto,
                )
                Spacer(Modifier.height(10.dp))
                Boton("Eliminar cuenta…", onClick = onEliminarCuenta, estilo = "fantasma")
                Text(
                    "Te llevaremos a Configuración de cuenta para confirmarlo.",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 6.dp),
                )
            }
        }
    }
}

@Composable
private fun Punto(texto: String) {
    Row(Modifier.padding(top = 10.dp)) {
        Text("•", fontWeight = FontWeight.Bold, fontSize = 14.sp)
        Spacer(Modifier.width(8.dp))
        Text(texto, fontSize = 14.sp, lineHeight = 20.sp)
    }
}

@Composable
private fun FilaPermiso(nombre: String, uso: String, concedido: Boolean) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
    ) {
        Column(Modifier.weight(1f)) {
            Text(nombre, fontWeight = FontWeight.Bold, fontSize = 15.sp)
            Text(uso, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Pill(
            if (concedido) "Permitido" else "No permitido",
            if (concedido) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
        )
    }
}
