package cl.duoc.casajojo.ui.components

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat

/**
 * Pide el permiso de notificaciones (Android 13+).
 *
 * Se llama al entrar a la pantalla de Retiro, que es el momento en que la
 * notificación pasa a ser útil: justo antes de confirmar el pedido. En
 * versiones anteriores a Android 13 el permiso no existe, así que no hace nada.
 * Si lo rechaza: el pedido se registra igual; se muestra [DialogoPermisoRechazado]
 * explicando que no llegará el aviso, con la opción de activarlo en Ajustes.
 */
@Composable
fun PedirPermisoNotificaciones() {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return

    val context = LocalContext.current
    var rechazado by rememberSaveable { mutableStateOf(false) }
    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { concedido -> rechazado = !concedido }

    if (rechazado) {
        DialogoPermisoRechazado(
            titulo = "Notificaciones desactivadas",
            mensaje = "Tu pedido se registra igual, pero no te avisaremos con una notificación. " +
                "Puedes activarlas en Ajustes → Permisos → Notificaciones.",
            onCerrar = { rechazado = false },
        )
    }

    LaunchedEffect(Unit) {
        val concedido = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.POST_NOTIFICATIONS,
        ) == PackageManager.PERMISSION_GRANTED

        if (!concedido) launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
    }
}
