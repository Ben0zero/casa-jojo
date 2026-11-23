package cl.duoc.casajojo.ui.viewmodels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import cl.duoc.casajojo.data.local.CasaJojoDatabase
import cl.duoc.casajojo.data.local.SesionPreferences
import cl.duoc.casajojo.data.repository.AuthRepository
import cl.duoc.casajojo.domain.error.CasaJojoException
import cl.duoc.casajojo.domain.error.ValidacionException
import cl.duoc.casajojo.domain.usecase.CambioPasswordErrores
import cl.duoc.casajojo.domain.usecase.PerfilErrores
import cl.duoc.casajojo.domain.usecase.ValidacionesPerfil
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * EFT: ficha de la pantalla Perfil. Tres formularios en un solo estado:
 * "Mis datos" (nombre y teléfono), "Cambiar contraseña" y "Eliminar cuenta".
 */
data class PerfilUiState(
    val correo: String = "",
    val nombre: String = "",
    val telefono: String = "",
    val erroresDatos: PerfilErrores = PerfilErrores(),
    val guardandoDatos: Boolean = false,

    val actual: String = "",
    val nueva: String = "",
    val confirmar: String = "",
    val erroresPassword: CambioPasswordErrores = CambioPasswordErrores(),
    val guardandoPassword: Boolean = false,

    /** true mientras se muestra el diálogo "¿Eliminar tu cuenta?". */
    val confirmandoEliminar: Boolean = false,
    val eliminando: Boolean = false,

    /** Aviso para el Snackbar; [mensajeEsError] decide el color/ícono. */
    val mensaje: String? = null,
    val mensajeEsError: Boolean = false,
)

/**
 * EFT: CRUD de la cuenta propia contra ms-auth, con la copia local en Room.
 * - READ: al abrir se muestran los datos de Room y se refrescan desde ms-auth.
 * - UPDATE: nombre/teléfono y contraseña (validación por campo en domain).
 * - DELETE: eliminar cuenta con diálogo de confirmación.
 */
class PerfilViewModel @JvmOverloads constructor(
    app: Application,
    private val repo: AuthRepository = CasaJojoDatabase.get(app).let { db ->
        AuthRepository(
            usuarios = db.usuarioDao(),
            sesion = SesionPreferences(app),
            pedidosDelUsuario = { id -> db.pedidoDao().borrarDelUsuario(id) },
        )
    },
) : AndroidViewModel(app) {

    private val _estado = MutableStateFlow(PerfilUiState())
    val estado: StateFlow<PerfilUiState> = _estado.asStateFlow()

    /** true cuando la persona ya escribió en "Mis datos": Room no le pisa lo escrito. */
    private var datosEditados = false

    init {
        // Room es la fuente inmediata (funciona sin red); ms-auth la actualiza después.
        viewModelScope.launch {
            repo.usuarioActual.collect { u ->
                if (u != null) {
                    _estado.update {
                        if (datosEditados) it.copy(correo = u.correo)
                        else it.copy(correo = u.correo, nombre = u.nombre, telefono = u.telefono)
                    }
                }
            }
        }
        refrescar()
    }

    /** READ remoto. Si falla (sin red), se queda con la copia de Room sin molestar. */
    fun refrescar() {
        viewModelScope.launch { repo.refrescarPerfil() }
    }

    // ---------------- Mis datos ----------------

    fun onNombre(v: String) {
        datosEditados = true
        _estado.update { it.copy(nombre = v, erroresDatos = it.erroresDatos.copy(nombre = null)) }
    }

    fun onTelefono(v: String) {
        datosEditados = true
        _estado.update { it.copy(telefono = v, erroresDatos = it.erroresDatos.copy(telefono = null)) }
    }

    fun guardarDatos() {
        val a = _estado.value
        if (a.guardandoDatos) return
        val errores = ValidacionesPerfil.validarPerfil(a.nombre, a.telefono)
        if (errores.hayErrores) {
            _estado.update { it.copy(erroresDatos = errores, mensaje = "Revisa los campos marcados", mensajeEsError = true) }
            return
        }
        viewModelScope.launch {
            _estado.update { it.copy(guardandoDatos = true) }
            repo.actualizarPerfil(a.nombre, a.telefono).fold(
                onSuccess = { u ->
                    datosEditados = false
                    _estado.update {
                        it.copy(guardandoDatos = false, nombre = u.nombre, telefono = u.telefono,
                            mensaje = "Tus datos se guardaron", mensajeEsError = false)
                    }
                },
                onFailure = { e -> _estado.update { it.copy(guardandoDatos = false).conError(e) } },
            )
        }
    }

    // ---------------- Cambiar contraseña ----------------

    fun onActual(v: String) =
        _estado.update { it.copy(actual = v, erroresPassword = it.erroresPassword.copy(actual = null)) }

    fun onNueva(v: String) =
        _estado.update { it.copy(nueva = v, erroresPassword = it.erroresPassword.copy(nueva = null)) }

    fun onConfirmar(v: String) =
        _estado.update { it.copy(confirmar = v, erroresPassword = it.erroresPassword.copy(confirmar = null)) }

    fun cambiarPassword() {
        val a = _estado.value
        if (a.guardandoPassword) return
        val errores = ValidacionesPerfil.validarCambioPassword(a.actual, a.nueva, a.confirmar)
        if (errores.hayErrores) {
            _estado.update { it.copy(erroresPassword = errores, mensaje = "Revisa los campos marcados", mensajeEsError = true) }
            return
        }
        viewModelScope.launch {
            _estado.update { it.copy(guardandoPassword = true) }
            repo.cambiarPassword(a.actual, a.nueva).fold(
                onSuccess = {
                    _estado.update {
                        it.copy(guardandoPassword = false, actual = "", nueva = "", confirmar = "",
                            erroresPassword = CambioPasswordErrores(),
                            mensaje = "Contraseña actualizada", mensajeEsError = false)
                    }
                },
                onFailure = { e ->
                    _estado.update {
                        val base = it.copy(guardandoPassword = false).conError(e)
                        // "La contraseña actual no es correcta" se marca bajo ese campo.
                        if ((e as? ValidacionException)?.campo == "passwordActual") {
                            base.copy(erroresPassword = base.erroresPassword.copy(actual = base.mensaje))
                        } else {
                            base
                        }
                    }
                },
            )
        }
    }

    // ---------------- Eliminar cuenta ----------------

    fun pedirEliminar() = _estado.update { it.copy(confirmandoEliminar = true) }

    fun cancelarEliminar() = _estado.update { it.copy(confirmandoEliminar = false) }

    /** Tras confirmar en el diálogo: borra en ms-auth y Room; [onEliminada] vuelve al login. */
    fun eliminarCuenta(onEliminada: () -> Unit) {
        if (_estado.value.eliminando) return
        viewModelScope.launch {
            _estado.update { it.copy(confirmandoEliminar = false, eliminando = true) }
            repo.eliminarCuenta().fold(
                onSuccess = {
                    _estado.value = PerfilUiState()
                    onEliminada()
                },
                onFailure = { e -> _estado.update { it.copy(eliminando = false).conError(e) } },
            )
        }
    }

    fun limpiarMensaje() = _estado.update { it.copy(mensaje = null) }

    private fun PerfilUiState.conError(e: Throwable) = copy(
        mensaje = (e as? CasaJojoException)?.mensajeUsuario ?: "Ocurrió un error. Intenta de nuevo.",
        mensajeEsError = true,
    )
}
