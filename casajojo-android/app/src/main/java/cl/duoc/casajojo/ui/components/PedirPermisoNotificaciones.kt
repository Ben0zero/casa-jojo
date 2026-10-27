package cl.duoc.casajojo.ui.components

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat

/**
 * Pide el permiso de notificaciones (Android 13+).
 *
 * Se llama al entrar a la pantalla de Retiro, que es el momento en que la
 * notificación pasa a ser útil: justo antes de confirmar el pedido. En
 * versiones anteriores a Android 13 el permiso no existe, así que no hace nada.
 */
@Composable
fun PedirPermisoNotificaciones() {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return

    val context = LocalContext.current
    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { /* Si lo rechaza, el pedido se guarda igual: solo no aparece la notificación. */ }

    LaunchedEffect(Unit) {
        val concedido = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.POST_NOTIFICATIONS,
        ) == PackageManager.PERMISSION_GRANTED

        if (!concedido) launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
    }
}
