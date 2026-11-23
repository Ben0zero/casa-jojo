package cl.duoc.casajojo.domain.error

import cl.duoc.casajojo.data.remote.ErroresRemotos
import io.kotest.matchers.booleans.shouldBeFalse
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Test
import retrofit2.HttpException
import retrofit2.Response
import java.io.IOException

/** EFT: jerarquía de excepciones propia y traducción de errores remotos. */
class CasaJojoExceptionTest {

    private fun http(codigo: Int, cuerpo: String = "") =
        HttpException(Response.error<Any>(codigo, cuerpo.toResponseBody()))

    @Test
    fun `las subclases heredan de CasaJojoException con mensaje claro`() {
        val errores: List<CasaJojoException> = listOf(
            ValidacionException("Dato malo", campo = "codigo"),
            RedException(),
            ServidorException(500, "Falla"),
            SesionExpiradaException(),
            SinPermisoException(),
            NoEncontradoException(),
            ConflictoException("Ya no se puede"),
        )
        errores.all { it.mensajeUsuario.isNotBlank() }.shouldBeTrue()
        (errores[3] as ServidorException).codigo shouldBe 401
        errores[4].shouldBeInstanceOf<ServidorException>()
    }

    @Test
    fun `reintentable es polimorfico segun el tipo de falla`() {
        RedException().reintentable.shouldBeTrue()
        ServidorException(503, "caído").reintentable.shouldBeTrue()
        ConflictoException("no").reintentable.shouldBeFalse()
        ValidacionException("no").reintentable.shouldBeFalse()
    }

    @Test
    fun `traduce cada codigo HTTP a su subclase`() {
        ErroresRemotos.traducir(http(400)).shouldBeInstanceOf<ValidacionException>()
        ErroresRemotos.traducir(http(401)).shouldBeInstanceOf<SesionExpiradaException>()
        ErroresRemotos.traducir(http(403)).shouldBeInstanceOf<SinPermisoException>()
        ErroresRemotos.traducir(http(404)).shouldBeInstanceOf<NoEncontradoException>()
        ErroresRemotos.traducir(http(409)).shouldBeInstanceOf<ConflictoException>()
        (ErroresRemotos.traducir(http(500)) as ServidorException).codigo shouldBe 500
        ErroresRemotos.traducir(IOException("sin red")).shouldBeInstanceOf<RedException>()
        ErroresRemotos.traducir(IllegalArgumentException("x")).shouldBeInstanceOf<ServidorException>()
        val propia = ConflictoException("igual")
        ErroresRemotos.traducir(propia) shouldBe propia
    }

    @Test
    fun `usa el mensaje del servidor (ms-auth y ms-ventas) o el indicado`() {
        ErroresRemotos.traducir(http(400, """{"mensaje":"El código no es válido"}""")).message shouldBe
            "El código no es válido"
        ErroresRemotos.traducir(http(409, """{"message":"Plazo vencido"}""")).message shouldBe "Plazo vencido"
        val conCampo = ErroresRemotos.traducir(http(400), en400 = "Revisa", campo400 = "codigo")
        (conCampo as ValidacionException).campo shouldBe "codigo"
        conCampo.message shouldBe "Revisa"
        ErroresRemotos.traducir(http(409, "no-json"), en409 = "Conflicto").message shouldBe "Conflicto"
    }
}
