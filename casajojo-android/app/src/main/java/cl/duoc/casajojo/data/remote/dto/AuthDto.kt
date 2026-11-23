package cl.duoc.casajojo.data.remote.dto

/**
 * DTOs de autenticación. Los campos son nullable a propósito: Gson no ejecuta
 * los constructores de Kotlin, así que nunca asumimos que un campo llegó.
 */

data class LoginRequestDto(
    val correo: String,
    val password: String,
)

data class RegistroRequestDto(
    val nombre: String,
    val correo: String,
    /** Opcional en ms-auth; null si no se conoce (Gson no lo envía). */
    val telefono: String?,
    val password: String,
)

data class UsuarioResponseDto(
    val id: Long? = null,
    val nombre: String? = null,
    val correo: String? = null,
    val username: String? = null,
    val nombreRol: String? = null,
    val telefono: String? = null,
    val token: String? = null,
)

// ---------- EFT: recuperación de contraseña y perfil (/api/usuarios/me) ----------

data class RecuperarRequestDto(val correo: String)

data class RestablecerRequestDto(
    val correo: String,
    val codigo: String,
    val nuevaPassword: String,
)

data class ActualizarPerfilDto(
    val nombre: String,
    val telefono: String,
)

data class CambiarPasswordDto(
    val passwordActual: String,
    val nuevaPassword: String,
)

/** Respuesta simple {"mensaje": "..."} de ms-auth. */
data class MensajeDto(val mensaje: String? = null)
