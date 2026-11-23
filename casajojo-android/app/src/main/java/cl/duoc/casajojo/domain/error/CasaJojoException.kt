package cl.duoc.casajojo.domain.error

/**
 * Jerarquía de excepciones propia de La Casa JoJo (POO: herencia + polimorfismo).
 *
 * Los Repository traducen cualquier falla (validación, red, HTTP) a una de estas
 * subclases. La UI solo muestra [mensajeUsuario] y, si existe, marca el [campo]
 * con error: nunca ve un "HTTP 409" ni un IOException.
 *
 *                     CasaJojoException (sealed)
 *            ┌──────────────┼──────────────────┐
 *   ValidacionException  RedException   ServidorException (open)
 *                                ┌──────────┬─────┴─────┬───────────┐
 *                         SesionExpirada  SinPermiso  NoEncontrado  Conflicto
 */
sealed class CasaJojoException(
    mensaje: String,
    causa: Throwable? = null,
) : Exception(mensaje, causa) {

    /** Texto listo para mostrar en un Snackbar o bajo un campo. */
    open val mensajeUsuario: String get() = message ?: "Ocurrió un error. Intenta de nuevo."

    /** true si conviene ofrecer "Reintentar" (falla temporal). Cada subclase decide. */
    abstract val reintentable: Boolean
}

/** Datos inválidos: se detecta en la app o el servidor responde 400. */
class ValidacionException(
    mensaje: String,
    /** Nombre del campo afectado ("codigo", "passwordActual"...) o null si es general. */
    val campo: String? = null,
    causa: Throwable? = null,
) : CasaJojoException(mensaje, causa) {
    override val reintentable: Boolean get() = false
}

/** Sin red, timeout o el microservicio no está levantado. */
class RedException(
    causa: Throwable? = null,
    mensaje: String = "No pudimos conectar con el servidor. Revisa tu conexión e intenta de nuevo.",
) : CasaJojoException(mensaje, causa) {
    override val reintentable: Boolean get() = true
}

/** El servidor respondió con un código de error HTTP. */
open class ServidorException(
    val codigo: Int,
    mensaje: String,
    causa: Throwable? = null,
) : CasaJojoException(mensaje, causa) {
    /** 5xx son fallas temporales del servidor; los 4xx no mejoran reintentando. */
    override val reintentable: Boolean get() = codigo >= 500
}

/** 401: el token venció o no hay sesión con el servidor. */
class SesionExpiradaException(causa: Throwable? = null) :
    ServidorException(401, "Tu sesión expiró. Vuelve a iniciar sesión con conexión.", causa)

/** 403: el recurso es de otro cliente. */
class SinPermisoException(mensaje: String = "No puedes modificar este recurso", causa: Throwable? = null) :
    ServidorException(403, mensaje, causa)

/** 404: el recurso no existe en el servidor. */
class NoEncontradoException(mensaje: String = "No encontramos lo que buscabas en el servidor", causa: Throwable? = null) :
    ServidorException(404, mensaje, causa)

/** 409: una regla de negocio lo impide (plazo vencido, estado que ya cambió...). */
class ConflictoException(mensaje: String, causa: Throwable? = null) :
    ServidorException(409, mensaje, causa)
