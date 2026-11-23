package cl.duoc.casajojo.data.remote

import cl.duoc.casajojo.domain.error.CasaJojoException
import cl.duoc.casajojo.domain.error.ConflictoException
import cl.duoc.casajojo.domain.error.NoEncontradoException
import cl.duoc.casajojo.domain.error.RedException
import cl.duoc.casajojo.domain.error.ServidorException
import cl.duoc.casajojo.domain.error.SesionExpiradaException
import cl.duoc.casajojo.domain.error.SinPermisoException
import cl.duoc.casajojo.domain.error.ValidacionException
import com.google.gson.JsonParser
import retrofit2.HttpException
import java.io.IOException

/**
 * Traduce las excepciones de Retrofit/OkHttp a la jerarquía [CasaJojoException].
 *
 * ms-auth responde {"mensaje": "..."} y ms-ventas {"message": "..."}; si el cuerpo
 * trae alguno de los dos, se usa ese texto (ya viene en español). Si no, se usa el
 * mensaje por defecto de cada código.
 */
object ErroresRemotos {

    fun traducir(
        e: Throwable,
        en400: String? = null,
        en404: String? = null,
        en409: String? = null,
        campo400: String? = null,
    ): CasaJojoException = when (e) {
        is CasaJojoException -> e
        is HttpException -> {
            val delServidor = mensajeDelCuerpo(e)
            when (e.code()) {
                400 -> ValidacionException(en400 ?: delServidor ?: "Revisa los datos ingresados", campo400, e)
                401 -> SesionExpiradaException(e)
                403 -> SinPermisoException(causa = e)
                404 -> NoEncontradoException(en404 ?: delServidor ?: "No encontramos lo que buscabas en el servidor", e)
                409 -> ConflictoException(en409 ?: delServidor ?: "La operación ya no es posible", e)
                else -> ServidorException(e.code(), "El servidor no respondió bien. Intenta de nuevo.", e)
            }
        }
        is IOException -> RedException(e)
        else -> ServidorException(-1, "Ocurrió un error inesperado. Intenta de nuevo.", e)
    }

    /** Lee "mensaje" (ms-auth) o "message" (ms-ventas) del cuerpo de error, si existe. */
    internal fun mensajeDelCuerpo(e: HttpException): String? = runCatching {
        val texto = e.response()?.errorBody()?.string().orEmpty()
        if (texto.isBlank()) return@runCatching null
        val json = JsonParser.parseString(texto).asJsonObject
        listOf("mensaje", "message")
            .firstNotNullOfOrNull { clave -> json.get(clave)?.takeIf { it.isJsonPrimitive }?.asString }
            ?.takeIf { it.isNotBlank() }
    }.getOrNull()
}
