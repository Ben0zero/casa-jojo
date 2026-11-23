package cl.duoc.casajojo.data.remote

import io.kotest.matchers.shouldBe
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Test

/**
 * A1: el interceptor agrega el token solo hacia nuestros microservicios.
 * No hay red: un interceptor final captura la petición y responde 200 al tiro.
 */
class AuthInterceptorTest {

    private var capturada: Request? = null

    private fun cliente(token: String?): OkHttpClient = OkHttpClient.Builder()
        .addInterceptor(AuthInterceptor("10.0.2.2") { token })
        .addInterceptor(Interceptor { chain ->
            capturada = chain.request()
            Response.Builder()
                .request(chain.request())
                .protocol(Protocol.HTTP_1_1)
                .code(200)
                .message("OK")
                .body("{}".toResponseBody())
                .build()
        })
        .build()

    private fun llamar(token: String?, url: String, authorization: String? = null) {
        val request = Request.Builder().url(url).apply {
            if (authorization != null) header("Authorization", authorization)
        }.build()
        cliente(token).newCall(request).execute().close()
    }

    @Test
    fun `agrega Bearer hacia los microservicios cuando hay token`() {
        llamar("jwt-123", "http://10.0.2.2:8086/api/pedidos/mis-pedidos?correo=a@a.cl")
        capturada?.header("Authorization") shouldBe "Bearer jwt-123"
    }

    @Test
    fun `no envia el token a APIs externas como Open-Meteo`() {
        llamar("jwt-123", "https://api.open-meteo.com/v1/forecast")
        capturada?.header("Authorization") shouldBe null
    }

    @Test
    fun `sin sesion no agrega cabecera`() {
        llamar(null, "http://10.0.2.2:8086/api/pedidos")
        capturada?.header("Authorization") shouldBe null
        llamar("  ", "http://10.0.2.2:8086/api/pedidos")
        capturada?.header("Authorization") shouldBe null
    }

    @Test
    fun `respeta una cabecera Authorization que ya venia`() {
        llamar("jwt-123", "http://10.0.2.2:8081/api/auth/login", authorization = "Bearer otro")
        capturada?.header("Authorization") shouldBe "Bearer otro"
    }
}
