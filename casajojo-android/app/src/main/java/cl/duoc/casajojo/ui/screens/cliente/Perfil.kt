package cl.duoc.casajojo.ui.screens.cliente

import android.Manifest
import android.content.ActivityNotFoundException
import android.content.pm.PackageManager
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import cl.duoc.casajojo.BuildConfig
import cl.duoc.casajojo.data.local.PerfilPhotoStorage
import cl.duoc.casajojo.data.local.entity.Usuario
import cl.duoc.casajojo.ui.components.Aviso
import cl.duoc.casajojo.ui.components.AvatarUsuario
import cl.duoc.casajojo.ui.components.Boton
import cl.duoc.casajojo.ui.components.DialogoPermisoRechazado
import cl.duoc.casajojo.ui.components.FilaOpcion
import cl.duoc.casajojo.ui.components.SeccionOpciones
import cl.duoc.casajojo.ui.components.Titulo
import cl.duoc.casajojo.ui.components.inicialesDe
import cl.duoc.casajojo.ui.layouts.Pantalla
import cl.duoc.casajojo.ui.theme.Ac

/** Destinos que se abren desde el menú del Perfil (las rutas las decide App.kt). */
data class AccionesPerfil(
    val onConfiguracion: () -> Unit,
    val onHistorial: () -> Unit,
    val onMiPedido: () -> Unit,
    val onBilletera: () -> Unit,
    val onPrivacidad: () -> Unit,
    val onAccesibilidad: () -> Unit,
    val onQuienesSomos: () -> Unit,
)

/**
 * Perfil (estilo Uber).
 * - Arriba: avatar con la foto de la cuenta, nombre, correo, "Tomar foto" y "Cerrar sesión".
 * - Abajo: opciones agrupadas por sección (Cuenta, Pedidos, Pagos, Preferencias, Información).
 * - Al final: versión de la app (BuildConfig.VERSION_NAME).
 * Los formularios de datos y contraseña se movieron a Configuración de cuenta.
 */
@Composable
fun ViewPerfil(
    usuario: Usuario?,
    foto: PerfilPhotoStorage.Foto?,
    /** URI temporal (FileProvider) donde la cámara deja la foto a resolución completa. */
    uriCaptura: () -> Uri,
    /** Procesa y guarda la captura; avisa si se pudo guardar. */
    onFotoTomada: (onResultado: (Boolean) -> Unit) -> Unit,
    onCerrarSesion: () -> Unit,
    acciones: AccionesPerfil,
    barra: @Composable () -> Unit,
) {
    val context = LocalContext.current
    var mensajeFoto by remember { mutableStateOf<String?>(null) }
    var camaraRechazada by remember { mutableStateOf(false) }
    // rememberSaveable: el diálogo sigue abierto aunque se gire el teléfono.
    var confirmarSalida by rememberSaveable { mutableStateOf(false) }

    // TakePicture (no TakePicturePreview): la cámara escribe la foto completa en la URI,
    // así el avatar se ve nítido en vez de un thumbnail pixelado.
    val camara = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { tomada ->
        if (tomada) {
            onFotoTomada { ok ->
                mensajeFoto = if (ok) "Foto de perfil actualizada" else "No pudimos guardar la foto. Intenta de nuevo."
            }
        }
    }

    // Sin app de cámara (o sin cámara: uses-feature required="false") no se cae la app.
    fun lanzarCamara() {
        try {
            camara.launch(uriCaptura())
        } catch (e: ActivityNotFoundException) {
            mensajeFoto = "Este dispositivo no tiene una app de cámara disponible"
        }
    }

    val pedirCamara = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { concedido ->
        if (concedido) {
            lanzarCamara()
        } else {
            mensajeFoto = "Necesitamos permiso para usar la cámara"
            camaraRechazada = true
        }
    }

    // El permiso CAMERA se pide recién al tocar el botón (no al abrir la pantalla).
    fun abrirCamara() {
        val permiso = ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA)
        if (permiso == PackageManager.PERMISSION_GRANTED) lanzarCamara() else pedirCamara.launch(Manifest.permission.CAMERA)
    }

    if (camaraRechazada) {
        DialogoPermisoRechazado(
            titulo = "Permiso de cámara rechazado",
            mensaje = "Sin la cámara no podemos tomar tu foto de perfil; el resto de la app " +
                "funciona igual. Puedes activarla en Ajustes → Permisos → Cámara.",
            onCerrar = { camaraRechazada = false },
        )
    }

    if (confirmarSalida) {
        AlertDialog(
            onDismissRequest = { confirmarSalida = false },
            icon = { Text("👋", fontSize = 28.sp) },
            title = { Text("¿Cerrar sesión?", fontWeight = FontWeight.Black) },
            text = { Text("Tendrás que ingresar tu correo y contraseña para volver a pedir.") },
            confirmButton = {
                TextButton(onClick = { confirmarSalida = false; onCerrarSesion() }) {
                    Text("Cerrar sesión", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = { TextButton(onClick = { confirmarSalida = false }) { Text("Cancelar") } },
        )
    }

    Pantalla(bar = barra) {
        Column(Modifier.fillMaxWidth()) {
            Spacer(Modifier.height(10.dp))
            Titulo("Mi perfil", "Tu cuenta en La Casa JoJo")
            Spacer(Modifier.height(18.dp))

            // ---------- Cabecera: foto, nombre y correo ----------
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                Surface(
                    shape = CircleShape,
                    shadowElevation = 6.dp,
                    border = BorderStroke(3.dp, Brush.horizontalGradient(listOf(Ac, Color(0xFFFFC83D)))),
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        AvatarUsuario(
                            foto = foto,
                            iniciales = inicialesDe(usuario?.nombre),
                            tamano = 120.dp,
                            tamanoTexto = 40.sp,
                        )
                    }
                }
                Spacer(Modifier.height(12.dp))
                Text(
                    usuario?.nombre ?: "Mi cuenta",
                    fontSize = 21.sp,
                    fontWeight = FontWeight.Black,
                    textAlign = TextAlign.Center,
                )
                Text(
                    usuario?.correo.orEmpty(),
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
                Spacer(Modifier.height(16.dp))
                Boton("📸  Tomar foto con la cámara", onClick = ::abrirCamara, estilo = "fantasma")
                Aviso(mensajeFoto, esError = mensajeFoto?.startsWith("Foto") == false)
                Spacer(Modifier.height(10.dp))
                // Secundario en rojo del tema: acción destructiva pero reversible (se confirma).
                OutlinedButton(
                    onClick = { confirmarSalida = true },
                    shape = RoundedCornerShape(99.dp),
                    border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.error),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                    modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp),
                ) {
                    Text("↪️  Cerrar sesión", fontWeight = FontWeight.ExtraBold, fontSize = 15.sp)
                }
            }

            // ---------- Opciones (estilo Uber) ----------
            SeccionOpciones("Cuenta", listOf {
                FilaOpcion("⚙️", "Configuración de cuenta", "Nombre, celular, contraseña y eliminar cuenta", acciones.onConfiguracion)
            })
            SeccionOpciones("Pedidos", listOf(
                { FilaOpcion("📚", "Historial de pedidos", "Entregados y cancelados", acciones.onHistorial) },
                { FilaOpcion("🧾", "Ver mi pedido actual", "Estado, cancelar, editar y retiro con QR", acciones.onMiPedido) },
            ))
            SeccionOpciones("Pagos", listOf {
                FilaOpcion("💳", "Billetera", "Tus tarjetas guardadas (simulado)", acciones.onBilletera)
            })
            SeccionOpciones("Preferencias", listOf(
                { FilaOpcion("🔒", "Privacidad", "Tus datos, permisos y foto", acciones.onPrivacidad) },
                { FilaOpcion("👁️", "Accesibilidad", "Tema, tamaño de texto y animaciones", acciones.onAccesibilidad) },
            ))
            SeccionOpciones("Información", listOf {
                FilaOpcion("🏠", "Quiénes somos", "La Casa JoJo y el equipo", acciones.onQuienesSomos)
            })

            Text(
                "La Casa JoJo · versión ${BuildConfig.VERSION_NAME}",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().padding(top = 24.dp),
            )
        }
    }
}
