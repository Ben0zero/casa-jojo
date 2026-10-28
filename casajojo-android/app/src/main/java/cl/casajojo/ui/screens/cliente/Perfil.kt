package cl.duoc.casajojo.ui.screens.cliente

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import cl.duoc.casajojo.data.local.PerfilPhotoStorage
import cl.duoc.casajojo.data.local.entity.Usuario
import cl.duoc.casajojo.ui.components.Boton
import cl.duoc.casajojo.ui.components.EstiloBoton
import cl.duoc.casajojo.ui.components.Titulo
import cl.duoc.casajojo.ui.components.Volver
import cl.duoc.casajojo.ui.layouts.Pantalla
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Perfil del cliente.
 *
 * Recurso nativo: la cámara del dispositivo. El permiso se solicita justo al
 * pulsar el botón, no al iniciar la aplicación.
 *
 * CAMBIO (P3): TakePicture (foto completa en un archivo compartido con
 * FileProvider) en vez de TakePicturePreview (miniatura pixelada). Además,
 * "Cerrar sesión" vive aquí y no en la portada.
 */
@Composable
fun ViewPerfil(
    usuario: Usuario?,
    foto: Bitmap?,
    onFotoCambiada: (Bitmap) -> Unit,
    onVolver: () -> Unit,
    onCerrarSesion: () -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var mensaje by remember { mutableStateOf<String?>(null) }

    val camara = rememberLauncherForActivityResult(
        ActivityResultContracts.TakePicture(),
    ) { guardada ->
        if (!guardada) return@rememberLauncherForActivityResult
        // Leer y reducir la foto es lento: se hace en Dispatchers.IO.
        scope.launch {
            val imagen = withContext(Dispatchers.IO) { PerfilPhotoStorage.leerCaptura(context) }
            if (imagen != null) {
                onFotoCambiada(imagen)
                mensaje = "Foto de perfil actualizada"
            } else {
                mensaje = "No pudimos leer la foto. Intenta de nuevo."
            }
        }
    }

    fun lanzarCamara() = camara.launch(PerfilPhotoStorage.uriCaptura(context))

    val pedirCamara = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { concedido ->
        if (concedido) {
            lanzarCamara()
        } else {
            mensaje = "Necesitamos permiso para usar la cámara"
        }
    }

    fun abrirCamara() {
        val permiso = ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA)
        if (permiso == PackageManager.PERMISSION_GRANTED) {
            lanzarCamara()
        } else {
            pedirCamara.launch(Manifest.permission.CAMERA)
        }
    }

    Pantalla {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Volver(onVolver)
            Titulo("Mi perfil", "Personaliza tu cuenta en La Casa JoJo")
            Spacer(Modifier.height(28.dp))

            if (foto == null) {
                Box(
                    modifier = Modifier
                        .size(150.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surface),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        Icons.Filled.Person,
                        contentDescription = "Sin foto",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(72.dp),
                    )
                }
            } else {
                Image(
                    bitmap = foto.asImageBitmap(),
                    contentDescription = "Foto de perfil",
                    // Crop: la foto ya no es cuadrada; se recorta al círculo sin deformarla.
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.size(150.dp).clip(CircleShape),
                )
            }

            if (usuario != null) {
                Spacer(Modifier.height(16.dp))
                Text(usuario.nombre, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                Text(usuario.correo, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }

            Spacer(Modifier.height(24.dp))
            Boton("Tomar foto con la cámara", onClick = ::abrirCamara, icono = Icons.Filled.PhotoCamera)
            if (mensaje != null) {
                Spacer(Modifier.height(12.dp))
                Text(mensaje!!, color = MaterialTheme.colorScheme.primary)
            }

            Spacer(Modifier.height(32.dp))
            Boton(
                "Cerrar sesión",
                onClick = onCerrarSesion,
                estilo = EstiloBoton.Fantasma,
                icono = Icons.AutoMirrored.Filled.Logout,
            )
        }
    }
}