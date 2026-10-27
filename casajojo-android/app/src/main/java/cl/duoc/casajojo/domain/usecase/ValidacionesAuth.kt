package cl.duoc.casajojo.domain.usecase

/**
 * Mensajes de error del formulario de registro (patrón de la guía formulario-registro).
 * Cada campo es String?: null = el campo está bien; un texto = el aviso a mostrar
 * debajo de ese campo.
 */
data class RegistroErrores(
    val nombre: String? = null,
    val correo: String? = null,
    val telefono: String? = null,
    val password: String? = null,
    val confirmar: String? = null,
) {
    val hayErrores: Boolean
        get() = listOf(nombre, correo, telefono, password, confirmar).any { it != null }
}

/** Mensajes de error del formulario de inicio de sesión. */
data class LoginErrores(
    val correo: String? = null,
    val password: String? = null,
) {
    val hayErrores: Boolean get() = correo != null || password != null
}

/**
 * Reglas de validación de login y registro.
 * Es Kotlin puro (sin Android), por eso se puede probar con tests unitarios
 * (ver ValidacionesAuthTest).
 */
object ValidacionesAuth {

    // Formato básico de correo: algo@algo.dominio
    // (No usamos android.util.Patterns porque no funciona en tests unitarios.)
    private val REGEX_CORREO = Regex("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")

    const val LARGO_MINIMO_PASSWORD = 6

    /** Guardamos los correos sin espacios y en minúsculas: "Ana@Mail.com " = "ana@mail.com". */
    fun normalizarCorreo(correo: String): String = correo.trim().lowercase()

    /**
     * Deja el celular solo con 9 dígitos.
     * Acepta "912345678", "9 1234 5678" o "+56 9 1234 5678" → "912345678".
     */
    fun normalizarTelefono(telefono: String): String {
        val digitos = telefono.filter { it.isDigit() }
        return if (digitos.length == 11 && digitos.startsWith("56")) digitos.drop(2) else digitos
    }

    fun validarNombre(nombre: String): String? = when {
        nombre.isBlank() -> "El nombre es obligatorio"
        nombre.trim().length < 2 -> "El nombre es muy corto"
        else -> null
    }

    fun validarCorreo(correo: String): String? = when {
        correo.isBlank() -> "El correo es obligatorio"
        !REGEX_CORREO.matches(normalizarCorreo(correo)) -> "Ingresa un correo válido (ej.: nombre@correo.cl)"
        else -> null
    }

    /** Celular chileno: 9 dígitos que empiezan con 9. */
    fun validarTelefono(telefono: String): String? {
        val normalizado = normalizarTelefono(telefono)
        return when {
            telefono.isBlank() -> "El celular es obligatorio"
            normalizado.length != 9 || !normalizado.startsWith("9") ->
                "Ingresa un celular de 9 dígitos que empiece con 9"
            else -> null
        }
    }

    fun validarPassword(password: String): String? = when {
        password.isEmpty() -> "La contraseña es obligatoria"
        password.length < LARGO_MINIMO_PASSWORD -> "Debe tener al menos $LARGO_MINIMO_PASSWORD caracteres"
        password.none { it.isLetter() } || password.none { it.isDigit() } -> "Debe tener letras y números"
        else -> null
    }

    fun validarConfirmacion(password: String, confirmar: String): String? = when {
        confirmar.isEmpty() -> "Repite la contraseña"
        confirmar != password -> "Las contraseñas no coinciden"
        else -> null
    }

    /** Revisa todo el formulario de registro de una vez. */
    fun validarRegistro(
        nombre: String,
        correo: String,
        telefono: String,
        password: String,
        confirmar: String,
    ): RegistroErrores = RegistroErrores(
        nombre = validarNombre(nombre),
        correo = validarCorreo(correo),
        telefono = validarTelefono(telefono),
        password = validarPassword(password),
        confirmar = validarConfirmacion(password, confirmar),
    )

    /**
     * En el login solo verificamos que vengan datos con forma correcta.
     * Las reglas de largo de la contraseña son del registro, no del login.
     */
    fun validarLogin(correo: String, password: String): LoginErrores = LoginErrores(
        correo = validarCorreo(correo),
        password = if (password.isEmpty()) "Ingresa tu contraseña" else null,
    )
}