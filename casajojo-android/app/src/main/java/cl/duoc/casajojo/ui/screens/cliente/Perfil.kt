package cl.duoc.casajojo.ui.screens.cliente

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import cl.duoc.casajojo.ui.components.Badge
import cl.duoc.casajojo.ui.components.Boton
import cl.duoc.casajojo.ui.components.Titulo
import cl.duoc.casajojo.ui.components.Volver
import cl.duoc.casajojo.ui.layouts.Pantalla
import cl.duoc.casajojo.ui.theme.Ac

/**
 * Perfil del cliente: misma cámara y mismo permiso de siempre, con un avatar
 * más vistoso y un espacio reservado para la foto cuando se decida el asset.
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
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Volver(onVolver)
            Spacer(Modifier.height(18.dp))
            Titulo("Mi perfil", "Personaliza tu cuenta en La Casa JoJo")
            Spacer(Modifier.height(28.dp))

            Surface(
                shape = CircleShape,
                shadowElevation = 6.dp,
                border = androidx.compose.foundation.BorderStroke(
                    3.dp,
                    Brush.horizontalGradient(listOf(Ac, Color(0xFFFFC83D))),
                ),
            ) {
                if (foto == null) {
                    Box(
                        modifier = Modifier
                            .size(160.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.linearGradient(
                                    listOf(Color(0xFFE8F5EA), Color(0xFFD3EAD8)),
                                ),
                            ),
                        contentAlignment = Alignment.Center,
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("📷", fontSize = 34.sp)
                            Text(
                                "Espacio para tu foto",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF9E6250),
                            )
                        }
                    }
                } else {
                    Image(
                        bitmap = foto.asImageBitmap(),
                        contentDescription = "Foto de perfil",
                        modifier = Modifier.size(160.dp).clip(CircleShape),
                    )
                }
            }

            Spacer(Modifier.height(16.dp))
            Badge("Mi cuenta Casa JoJo")
            Spacer(Modifier.height(28.dp))

            Boton("📸  Tomar foto con la cámara", onClick = ::abrirCamara)
            if (mensaje != null) {
                Spacer(Modifier.height(12.dp))
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.secondaryContainer,
                ) {
                    Text(
                        mensaje!!,
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                        fontSize = 13.sp,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                    )
                }
            }
        }
    }
}
