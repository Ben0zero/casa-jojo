package cl.duoc.casajojo.data.repository

import cl.duoc.casajojo.data.remote.OpenFoodFactsApi
import cl.duoc.casajojo.data.remote.dto.NutrimentosOff
import cl.duoc.casajojo.data.remote.dto.ProductoOff
import cl.duoc.casajojo.data.remote.dto.RespuestaProductoOff
import cl.duoc.casajojo.domain.model.PlatoDraft
import io.kotest.matchers.result.shouldBeFailure
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Test
import retrofit2.Response
import java.io.IOException

class NutricionRepositoryTest {

    private val api = mockk<OpenFoodFactsApi>()
    private val repo = NutricionRepository(api)
    private val plato = PlatoDraft("Pollo asado", 4800, false, listOf("Arroz"))

    private fun respuesta(
        kcal: Double = 100.0,
        proteinas: Double = 10.0,
        carbohidratos: Double = 20.0,
        grasas: Double = 5.0,
    ) = RespuestaProductoOff(
        product = ProductoOff(
            nombre = "Producto de prueba",
            nutriments = NutrimentosOff(kcal, proteinas, carbohidratos, grasas),
        ),
    )

    @Test
    fun `suma la nutricion de dos ingredientes`() = runTest {
        coEvery { api.producto(any(), any()) } returns Response.success(respuesta())

        val resultado = repo.nutricionDe(plato).getOrThrow()

        resultado.items.size shouldBe 2
        resultado.total.kcal shouldBe 200.0
        resultado.total.proteinas shouldBe 20.0
        resultado.total.carbohidratos shouldBe 40.0
        resultado.total.grasas shouldBe 10.0
    }

    @Test
    fun `sin producto devuelve failure sin total parcial`() = runTest {
        coEvery { api.producto(any(), any()) } returns Response.success(RespuestaProductoOff())

        repo.nutricionDe(plato).shouldBeFailure()
    }

    @Test
    fun `error de red devuelve failure`() = runTest {
        coEvery { api.producto(any(), any()) } throws IOException("sin red")

        repo.nutricionDe(plato).shouldBeFailure()
    }

    @Test
    fun `respuesta 403 devuelve failure`() = runTest {
        coEvery { api.producto(any(), any()) } returns Response.error(
            403,
            mockk(relaxed = true),
        )

        repo.nutricionDe(plato).shouldBeFailure()
    }

    @Test
    fun `reintenta un 503 y usa la respuesta correcta`() = runTest {
        val respuestas = mutableListOf<Response<RespuestaProductoOff>>(
            Response.error(503, mockk(relaxed = true)),
            Response.success(respuesta()),
            Response.success(respuesta()),
        )
        coEvery { api.producto(any(), any()) } answers { respuestas.removeAt(0) }

        val resultado = repo.nutricionDe(plato).getOrThrow()

        resultado.items.size shouldBe 2
        coVerify(exactly = 3) { api.producto(any(), any()) }
    }

    @Test
    fun `segunda consulta usa la cache`() = runTest {
        coEvery { api.producto(any(), any()) } returns Response.success(respuesta())

        repo.nutricionDe(plato)
        repo.nutricionDe(plato)

        coVerify(exactly = 2) { api.producto(any(), any()) }
    }
}
