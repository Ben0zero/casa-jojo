package cl.duoc.casajojo.domain.usecase

import java.net.URI
import java.net.URLDecoder

/**
 * A5: formato propio del QR de retiro de La Casa JoJo.
 *
 *     casajojo://retiro?codigo=<CODIGO-DE-LA-TIENDA>
 *
 * La app ya NO abre URLs: solo acepta este formato, extrae el código y lo envía a
 * ms-ventas (PUT /api/pedidos/{id}/retirar), que lo compara con RETIRO_CODIGO.
 * Kotlin puro (java.net.URI) para poder probarlo con tests unitarios.
 */
object QrRetiro {

    const val ESQUEMA = "casajojo"
    const val ACCION = "retiro"
    private val CODIGO_VALIDO = Regex("^[A-Za-z0-9_-]{4,64}$")

    /** Devuelve el código de la tienda, o null si el QR no es de Casa JoJo. */
    fun codigo(contenido: String?): String? {
        val texto = contenido?.trim().orEmpty()
        if (texto.isEmpty()) return null
        val uri = runCatching { URI(texto) }.getOrNull() ?: return null
        if (!uri.scheme.equals(ESQUEMA, ignoreCase = true)) return null
        if (!uri.host.equals(ACCION, ignoreCase = true)) return null

        val codigo = uri.rawQuery.orEmpty()
            .split("&")
            .mapNotNull { par ->
                val partes = par.split("=", limit = 2)
                if (partes.size == 2 && partes[0] == "codigo") partes[1] else null
            }
            .firstOrNull()
            ?.let { runCatching { URLDecoder.decode(it, "UTF-8") }.getOrNull() }
            ?.trim()
            ?: return null

        return codigo.takeIf { CODIGO_VALIDO.matches(it) }
    }
}
