package cl.duoc.casajojo.domain.usecase

/** Errores del paso 1 de "¿Olvidaste tu contraseña?" (pedir el código). */
data class RecuperarErrores(val correo: String? = null) {
    val hayErrores: Boolean get() = correo != null
}

/** Errores del paso 2 (código + nueva contraseña). */
data class RestablecerErrores(
    val codigo: String? = null,
    val password: String? = null,
    val confirmar: String? = null,
) {
    val hayErrores: Boolean get() = listOf(codigo, password, confirmar).any { it != null }
}

/** Errores del formulario "Mis datos" del Perfil. */
data class PerfilErrores(
    val nombre: String? = null,
    val telefono: String? = null,
) {
    val hayErrores: Boolean get() = nombre != null || telefono != null
}

/** Errores del formulario "Cambiar contraseña" del Perfil. */
data class CambioPasswordErrores(
    val actual: String? = null,
    val nueva: String? = null,
    val confirmar: String? = null,
) {
    val hayErrores: Boolean get() = listOf(actual, nueva, confirmar).any { it != null }
}

/**
 * EFT: reglas de recuperación de contraseña y edición del perfil.
 * Kotlin puro (sin Android) para probarlas con JUnit; reutiliza las reglas de
 * [ValidacionesAuth] para que registro, perfil y recuperación pidan lo mismo.
 */
object ValidacionesPerfil {

    const val LARGO_CODIGO = 6
    private val REGEX_CODIGO = Regex("^\\d{$LARGO_CODIGO}$")

    fun validarCodigo(codigo: String): String? = when {
        codigo.isBlank() -> "Ingresa el código que te enviamos"
        !REGEX_CODIGO.matches(codigo.trim()) -> "El código tiene $LARGO_CODIGO dígitos"
        else -> null
    }

    fun validarRecuperar(correo: String): RecuperarErrores =
        RecuperarErrores(correo = ValidacionesAuth.validarCorreo(correo))

    fun validarRestablecer(codigo: String, password: String, confirmar: String): RestablecerErrores =
        RestablecerErrores(
            codigo = validarCodigo(codigo),
            password = ValidacionesAuth.validarPassword(password),
            confirmar = ValidacionesAuth.validarConfirmacion(password, confirmar),
        )

    fun validarPerfil(nombre: String, telefono: String): PerfilErrores = PerfilErrores(
        nombre = ValidacionesAuth.validarNombre(nombre),
        telefono = ValidacionesAuth.validarTelefono(telefono),
    )

    fun validarCambioPassword(actual: String, nueva: String, confirmar: String): CambioPasswordErrores =
        CambioPasswordErrores(
            actual = if (actual.isEmpty()) "Ingresa tu contraseña actual" else null,
            nueva = ValidacionesAuth.validarPassword(nueva)
                ?: if (nueva == actual) "Debe ser distinta de la actual" else null,
            confirmar = ValidacionesAuth.validarConfirmacion(nueva, confirmar),
        )
}
