package cl.duoc.casajojo.ui.screens.cliente

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import cl.duoc.casajojo.data.local.PerfilPhotoStorage
import cl.duoc.casajojo.ui.components.AvatarUsuario
import cl.duoc.casajojo.ui.components.inicialesDe
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
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
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import cl.duoc.casajojo.ui.components.Aviso
import cl.duoc.casajojo.ui.components.Badge
import cl.duoc.casajojo.ui.components.Boton
import cl.duoc.casajojo.ui.components.CampoTexto
import cl.duoc.casajojo.ui.components.Titulo
import cl.duoc.casajojo.ui.components.TituloBloque
import cl.duoc.casajojo.ui.components.Volver
import cl.duoc.casajojo.ui.layouts.Pantalla
import cl.duoc.casajojo.ui.theme.Ac
import cl.duoc.casajojo.ui.viewmodels.PerfilViewModel

/**
 * Perfil del cliente.
 * - Foto con la cámara (permiso CAMERA en tiempo de ejecución), como antes.
 * - EFT: CRUD de la cuenta contra ms-auth: ver y editar nombre/teléfono, cambiar la
 *   contraseña y eliminar la cuenta con diálogo de confirmación. El estado y las
 *   validaciones viven en PerfilViewModel (domain/ValidacionesPerfil).
 */
@Composable
fun ViewPerfil(
    vm: PerfilViewModel,
    foto: PerfilPhotoStorage.Foto?,
    /** URI temporal (FileProvider) donde la cámara deja la foto a resolución completa. */
    uriCaptura: () -> Uri,
    /** Procesa y guarda la captura; avisa si se pudo guardar. */
    onFotoTomada: (onResultado: (Boolean) -> Unit) -> Unit,
    onVolver: () -> Unit,
    onCuentaEliminada: () -> Unit,
) {
    val context = LocalContext.current
    val foco = LocalFocusManager.current
    val estado by vm.estado.collectAsState()
    var mensajeFoto by remember { mutableStateOf<String?>(null) }
    val snackbar = remember { SnackbarHostState() }

    LaunchedEffect(estado.mensaje) {
        estado.mensaje?.let {
            snackbar.showSnackbar(it)
            vm.limpiarMensaje()
        }
    }

    // TakePicture (no TakePicturePreview): la cámara escribe la foto completa en la URI,
    // así el avatar se ve nítido en vez de un thumbnail pixelado.
    val camara = rememberLauncherForActivityResult(
        ActivityResultContracts.TakePicture(),
    ) { tomada ->
        if (tomada) {
            onFotoTomada { ok ->
                mensajeFoto = if (ok) "Foto de perfil actualizada" else "No pudimos guardar la foto. Intenta de nuevo."
            }
        }
    }
    fun lanzarCamara() = camara.launch(uriCaptura())

    val pedirCamara = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { concedido ->
        if (concedido) {
            lanzarCamara()
        } else {
            mensajeFoto = "Necesitamos permiso para usar la cámara"
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

    Box(Modifier.fillMaxSize()) {
        Pantalla {
            Column(Modifier.fillMaxWidth()) {
                Volver(onVolver)
                Spacer(Modifier.height(18.dp))
                Titulo("Mi perfil", "Personaliza tu cuenta en La Casa JoJo")
                Spacer(Modifier.height(20.dp))

                // ---------- Foto (recurso nativo: cámara) ----------
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Surface(
                        shape = CircleShape,
                        shadowElevation = 6.dp,
                        border = androidx.compose.foundation.BorderStroke(
                            3.dp,
                            Brush.horizontalGradient(listOf(Ac, Color(0xFFFFC83D))),
                        ),
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            AvatarUsuario(
                                foto = foto,
                                iniciales = inicialesDe(estado.nombre),
                                tamano = 140.dp,
                                tamanoTexto = 44.sp,
                            )
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                    Badge(estado.correo.ifBlank { "Mi cuenta Casa JoJo" })
                    Spacer(Modifier.height(16.dp))
                    Boton("📸  Tomar foto con la cámara", onClick = ::abrirCamara, estilo = "fantasma")
                    Aviso(mensajeFoto, esError = mensajeFoto?.startsWith("Foto") == false)
                }

                // ---------- Mis datos (READ + UPDATE) ----------
                Tarjeta {
                    TituloBloque("👤", "Mis datos")
                    CampoTexto(
                        etiqueta = "Nombre",
                        valor = estado.nombre,
                        onCambio = vm::onNombre,
                        error = estado.erroresDatos.nombre,
                        placeholder = "Nombre y apellido",
                        enabled = !estado.guardandoDatos,
                        icono = "👤",
                    )
                    CampoTexto(
                        etiqueta = "Celular",
                        valor = estado.telefono,
                        onCambio = vm::onTelefono,
                        error = estado.erroresDatos.telefono,
                        placeholder = "9 1234 5678",
                        tipoTeclado = KeyboardType.Phone,
                        accionIme = ImeAction.Done,
                        enabled = !estado.guardandoDatos,
                        onListo = { foco.clearFocus(); vm.guardarDatos() },
                        icono = "📱",
                    )
                    Spacer(Modifier.height(16.dp))
                    Boton(
                        texto = if (estado.guardandoDatos) "Guardando…" else "💾  Guardar mis datos",
                        onClick = { foco.clearFocus(); vm.guardarDatos() },
                        estilo = "acento",
                        enabled = !estado.guardandoDatos,
                    )
                }

                // ---------- Cambiar contraseña (UPDATE) ----------
                Tarjeta {
                    TituloBloque("🔑", "Cambiar contraseña")
                    CampoTexto(
                        etiqueta = "Contraseña actual",
                        valor = estado.actual,
                        onCambio = vm::onActual,
                        error = estado.erroresPassword.actual,
                        esContrasena = true,
                        enabled = !estado.guardandoPassword,
                        icono = "🔒",
                    )
                    CampoTexto(
                        etiqueta = "Nueva contraseña",
                        valor = estado.nueva,
                        onCambio = vm::onNueva,
                        error = estado.erroresPassword.nueva,
                        placeholder = "Mínimo 6, con letras y números",
                        esContrasena = true,
                        enabled = !estado.guardandoPassword,
                        icono = "🔑",
                    )
                    CampoTexto(
                        etiqueta = "Repite la nueva contraseña",
                        valor = estado.confirmar,
                        onCambio = vm::onConfirmar,
                        error = estado.erroresPassword.confirmar,
                        esContrasena = true,
                        accionIme = ImeAction.Done,
                        enabled = !estado.guardandoPassword,
                        onListo = { foco.clearFocus(); vm.cambiarPassword() },
                        icono = "🔑",
                    )
                    Spacer(Modifier.height(16.dp))
                    Boton(
                        texto = if (estado.guardandoPassword) "Actualizando…" else "Actualizar contraseña",
                        onClick = { foco.clearFocus(); vm.cambiarPassword() },
                        enabled = !estado.guardandoPassword,
                    )
                }

                // ---------- Eliminar cuenta (DELETE) ----------
                Tarjeta {
                    TituloBloque("🗑️", "Eliminar cuenta")
                    Text(
                        "Se borrará tu cuenta del servidor y de este teléfono, junto con tu historial local.",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(12.dp))
                    Boton(
                        texto = if (estado.eliminando) "Eliminando…" else "Eliminar mi cuenta",
                        onClick = vm::pedirEliminar,
                        estilo = "fantasma",
                        enabled = !estado.eliminando,
                    )
                }
            }
        }
        SnackbarHost(snackbar, Modifier.align(Alignment.BottomCenter).padding(16.dp))
    }

    if (estado.confirmandoEliminar) {
        AlertDialog(
            onDismissRequest = vm::cancelarEliminar,
            icon = { Text("⚠️", fontSize = 28.sp) },
            title = { Text("¿Eliminar tu cuenta?") },
            text = {
                Text("Esta acción no se puede deshacer. Perderás tu sesión y tu historial en este teléfono.")
            },
            confirmButton = {
                TextButton(onClick = { vm.eliminarCuenta(onCuentaEliminada) }) {
                    Text("Sí, eliminar", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = vm::cancelarEliminar) { Text("Cancelar") }
            },
        )
    }
}

/** Tarjeta blanca con sombra para cada sección del Perfil. */
@Composable
private fun Tarjeta(contenido: @Composable () -> Unit) {
    Surface(
        shape = RoundedCornerShape(22.dp),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 3.dp,
        modifier = Modifier.fillMaxWidth().padding(top = 18.dp),
    ) {
        Column(Modifier.padding(horizontal = 18.dp, vertical = 16.dp)) { contenido() }
    }
}
