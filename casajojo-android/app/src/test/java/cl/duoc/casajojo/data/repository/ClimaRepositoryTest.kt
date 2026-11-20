package cl.duoc.casajojo.data.repository

import cl.duoc.casajojo.data.remote.ClimaApi
import cl.duoc.casajojo.data.remote.dto.ClimaActualDto
import cl.duoc.casajojo.data.remote.dto.ClimaDto
import io.kotest.matchers.result.shouldBeFailure
import io.kotest.matchers.string.shouldContain
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Test
import java.io.IOException

/**
 * IL3.2 / IE 3.1.4: la API externa se prueba mockeando Retrofit.
 * Ningún test llama a Open-Meteo de verdad: eso se demuestra en la app.
 */
class ClimaRepositoryTest {

    private val api = mockk<ClimaApi>()
    private val repo = ClimaRepository(api)

    @Test
    fun `clima de Viña se formatea para la interfaz`() = runTest {
        coEvery { api.climaActual(any(), any(), any(), any()) } returns ClimaDto(
            current = ClimaActualDto(temperatura = 24.3, codigo = 2, viento = 7.9),
        )

        val texto = repo.climaActual().getOrThrow()

        texto shouldContain "Parcialmente nublado"
        texto shouldContain "24°"
        texto shouldContain "8 km/h"
    }

    @Test
    fun `fallo de red no tumba el flujo de pedidos`() = runTest {
        coEvery { api.climaActual(any(), any(), any(), any()) } throws IOException("sin red")

        repo.climaActual().shouldBeFailure()
    }
}
