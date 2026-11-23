package cl.duoc.casajojo.ui.components

import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat

/**
 * Permiso de Android 17 (API 37) para conectarse a equipos de la red local.
 *
 * Se escribe como TEXTO a propósito: la constante Manifest.permission.ACCESS_LOCAL_NETWORK
 * solo existe al compilar con compileSdk 37, y así el código compila también con 36.
 */
const val PERMISO_RED_LOCAL = "android.permission.ACCESS_LOCAL_NETWORK"

/**
 * Primera versión de Android que exige [PERMISO_RED_LOCAL] (Android 17).
 * Número literal por la misma razón: no depende de Build.VERSION_CODES del compileSdk.
 */
const val API_RED_LOCAL = 37

/**
 * Pide el permiso de red local (Android 17 / API 37+).
 *
 * Los microservicios corren en la PC: el emulador los ve en 10.0.2.2 y un celular
 * físico en la IP de la PC. Ambas son direcciones de red LOCAL, y desde Android 17
 * una app que apunta a API 37 no puede conectarse a ellas sin este permiso (Internet,
 * como Open-Meteo, sigue funcionando). Sin él, login, menú y pedidos quedan en modo
 * sin conexión.
 *
 * Se pide al abrir la app; si se concede, [onConcedido] vuelve a sincronizar con
 * los microservicios. En versiones anteriores a Android 17 no hace nada.
 */
@Composable
fun PedirPermisoRedLocal(onConcedido: () -> Unit) {
    // Solo se pide si Build.VERSION.SDK_INT es 37 o superior; antes el permiso no existe.
    if (Build.VERSION.SDK_INT < API_RED_LOCAL) return

    val context = LocalContext.current
    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { concedido ->
        // Si lo rechaza, la app sigue funcionando con la caché (Room) sin conexión.
        if (concedido) onConcedido()
    }

    LaunchedEffect(Unit) {
        val concedido = ContextCompat.checkSelfPermission(context, PERMISO_RED_LOCAL) ==
            PackageManager.PERMISSION_GRANTED
        if (!concedido) launcher.launch(PERMISO_RED_LOCAL)
    }
}
