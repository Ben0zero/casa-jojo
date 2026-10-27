package cl.duoc.casajojo.ui.screens.cliente

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import cl.duoc.casajojo.ui.components.Boton
import cl.duoc.casajojo.ui.components.Titulo
import cl.duoc.casajojo.ui.components.Volver
import cl.duoc.casajojo.ui.layouts.Pantalla

/**
 * Perfil del cliente.
 *
 * Recurso nativo: TakePicturePreview abre la cámara del dispositivo y devuelve
 * una imagen para mostrarla inmediatamente como foto de perfil. El permiso se
 * solicita justo al pulsar el botón, no al iniciar la aplicación.
 */
@Composable
fun ViewPerfil(
    foto: Bitmap?,
    onFotoCambiada: (Bitmap) -> Unit,
    onVolver: () -> Unit,
) {
    val context = LocalContext.current
    var mensaje by remember { mutableStateOf<String?>(null) }

    val camara = rememberLauncherForActivityResult(
        ActivityResultContracts.TakePicturePreview(),
    ) { imagen ->
        if (imagen != null) {
            onFotoCambiada(imagen)
            mensaje = "Foto de perfil actualizada"
        }
    }

    val pedirCamara = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { concedido ->
        if (concedido) {
            camara.launch(null)
        } else {
            mensaje = "Necesitamos permiso para usar la cámara"
        }
    }

    fun abrirCamara() {
        val permiso = ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA)
        if (permiso == PackageManager.PERMISSION_GRANTED) {
            camara.launch(null)
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
                    Text("Sin foto", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            } else {
                Image(
                    bitmap = foto.asImageBitmap(),
                    contentDescription = "Foto de perfil",
                    modifier = Modifier.size(150.dp).clip(CircleShape),
                )
            }

            Spacer(Modifier.height(24.dp))
            Boton("Tomar foto con la cámara", onClick = ::abrirCamara)
            if (mensaje != null) {
                Spacer(Modifier.height(12.dp))
                Text(mensaje!!, color = MaterialTheme.colorScheme.primary)
            }
        }
    }
}
