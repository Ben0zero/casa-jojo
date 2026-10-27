package cl.duoc.casajojo.ui.viewmodels

import cl.duoc.casajojo.data.local.entity.Usuario
import cl.duoc.casajojo.domain.usecase.LoginErrores
import cl.duoc.casajojo.domain.usecase.RegistroErrores

/**
 * "Fichas" de estado de cada formulario (patrón de la guía formulario-registro):
 * todos los datos y errores juntos en un solo objeto. Cuando algo cambia, el
 * ViewModel publica una ficha nueva y la pantalla se redibuja sola.
 */

/** Ficha del formulario de inicio de sesión. */
data class LoginUiState(
    val correo: String = "",
    val password: String = "",
    val errores: LoginErrores = LoginErrores(),
    /** true mientras se verifica la contraseña: desactiva el botón y muestra "Ingresando…". */
    val cargando: Boolean = false,
    /** Aviso para el Snackbar. Se muestra una vez y la pantalla lo limpia. */
    val mensaje: String? = null,
)

/** Ficha del formulario de registro. */
data class RegistroUiState(
    val nombre: String = "",
    val correo: String = "",
    val telefono: String = "",
    val password: String = "",
    val confirmar: String = "",
    val errores: RegistroErrores = RegistroErrores(),
    /** true mientras se crea la cuenta (guía datastore: "guardando"). */
    val guardando: Boolean = false,
    val mensaje: String? = null,
)

/**
 * Estado de la sesión al abrir la app.
 * Cargando: aún no se lee el archivo de DataStore (se muestra un loader).
 * Activa: hay usuario → se entra directo al inicio.
 * Inactiva: no hay usuario → se muestra el login.
 */
sealed class EstadoSesion {
    data object Cargando : EstadoSesion()
    data class Activa(val usuario: Usuario) : EstadoSesion()
    data object Inactiva : EstadoSesion()
}