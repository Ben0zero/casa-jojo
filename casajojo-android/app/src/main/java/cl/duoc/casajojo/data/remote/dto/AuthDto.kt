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
    val telefono: String,
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
