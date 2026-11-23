package cl.duoc.casajojo.data.remote

import okhttp3.Interceptor
import okhttp3.Response

/**
 * A1: agrega "Authorization: Bearer <token>" a las llamadas a los microservicios
 * de Casa JoJo, para que ms-ventas sepa de quién es cada pedido.
 *
 * - Solo se agrega si el host es el de nuestros microservicios (API_HOST): el token
 *   NUNCA viaja a APIs externas como Open-Meteo, aunque compartan el cliente OkHttp.
 * - [proveedorToken] devuelve el token guardado en DataStore (o null si no hay sesión).
 * - Si la petición ya trae Authorization, se respeta.
 */
class AuthInterceptor(
    private val hostPropio: String,
    private val proveedorToken: () -> String?,
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val original = chain.request()
        if (!original.url.host.equals(hostPropio, ignoreCase = true) ||
            original.header(AUTHORIZATION) != null
        ) {
            return chain.proceed(original)
        }
        val token = proveedorToken()?.takeIf { it.isNotBlank() }
            ?: return chain.proceed(original)

        return chain.proceed(
            original.newBuilder().header(AUTHORIZATION, "Bearer $token").build()
        )
    }

    private companion object {
        const val AUTHORIZATION = "Authorization"
    }
}
