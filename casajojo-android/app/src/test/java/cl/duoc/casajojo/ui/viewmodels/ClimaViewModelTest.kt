package cl.duoc.casajojo.ui.viewmodels

import cl.duoc.casajojo.data.repository.ClimaRepository
import cl.duoc.casajojo.domain.error.RedException
import io.kotest.matchers.booleans.shouldBeFalse
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test

/** EFT (Fase D): API externa Open-Meteo con estados de carga, error y reintento. */
@OptIn(ExperimentalCoroutinesApi::class)
class ClimaViewModelTest {

    private val repo = mockk<ClimaRepository>()

    @Before
    fun preparar() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @After
    fun limpiar() = Dispatchers.resetMain()

    @Test
    fun `al abrir muestra el clima si Open-Meteo responde`() {
        coEvery { repo.climaActual() } returns Result.success("Despejado · 20° · viento 5 km/h")

        val vm = ClimaViewModel(repo)

        vm.estado.value shouldBe ClimaUiState(texto = "Despejado · 20° · viento 5 km/h")
    }

    @Test
    fun `si falla muestra el error y reintentar vuelve a consultar`() {
        coEvery { repo.climaActual() } returnsMany listOf(
            Result.failure(RedException()),
            Result.success("Nublado · 15° · viento 10 km/h"),
        )

        val vm = ClimaViewModel(repo)
        vm.estado.value.fallo.shouldBeTrue()
        vm.estado.value.error shouldBe RedException().mensajeUsuario

        vm.cargar()
        vm.estado.value.fallo.shouldBeFalse()
        vm.estado.value.texto shouldBe "Nublado · 15° · viento 10 km/h"
        coVerify(exactly = 2) { repo.climaActual() }
    }

    @Test
    fun `mientras carga muestra el indicador y no duplica la consulta`() {
        val respuesta = CompletableDeferred<Result<String>>()
        coEvery { repo.climaActual() } coAnswers { respuesta.await() }

        val vm = ClimaViewModel(repo)
        vm.estado.value.cargando.shouldBeTrue()
        vm.cargar() // toque repetido mientras carga: se ignora
        respuesta.complete(Result.failure(IllegalStateException("x")))

        vm.estado.value.error shouldBe "Sin conexión con el clima"
        coVerify(exactly = 1) { repo.climaActual() }
    }
}
