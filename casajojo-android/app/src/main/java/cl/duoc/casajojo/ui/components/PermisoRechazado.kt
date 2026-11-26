package cl.duoc.casajojo.ui.components

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/**
 * Abre la ficha de la app en Ajustes del sistema (Apps → La Casa JoJo → Permisos).
 * Es la única forma de recuperar un permiso que el usuario rechazó dos veces: después
 * de eso Android ya no vuelve a mostrar el diálogo del sistema.
 */
fun abrirAjustesDeLaApp(context: Context) {
    val intent = Intent(
        Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
        Uri.fromParts("package", context.packageName, null),
    ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    // runCatching: algunos dispositivos sin la pantalla de detalles no deben botar la app.
    runCatching { context.startActivity(intent) }
}

/**
 * Diálogo común cuando el usuario rechaza un permiso peligroso (cámara, notificaciones,
 * red local). Explica qué deja de funcionar, que la app sigue andando, y ofrece ir a
 * Ajustes para activarlo. Nunca bloquea: "Ahora no" cierra y la app continúa.
 */
@Composable
fun DialogoPermisoRechazado(
    titulo: String,
    mensaje: String,
    onCerrar: () -> Unit,
    textoCerrar: String = "Ahora no",
) {
    val context = LocalContext.current
    AlertDialog(
        onDismissRequest = onCerrar,
        icon = { Text("🔒", fontSize = 26.sp) },
        title = { Text(titulo, fontWeight = FontWeight.Black) },
        text = { Text(mensaje) },
        confirmButton = {
            TextButton(onClick = {
                abrirAjustesDeLaApp(context)
                onCerrar()
            }) { Text("Ir a ajustes", fontWeight = FontWeight.Bold) }
        },
        dismissButton = {
            TextButton(onClick = onCerrar) { Text(textoCerrar) }
        },
    )
}
