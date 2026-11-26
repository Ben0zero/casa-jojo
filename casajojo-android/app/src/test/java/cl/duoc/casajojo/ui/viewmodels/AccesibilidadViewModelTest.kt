package cl.duoc.casajojo.ui.viewmodels

import android.app.Application
import cl.duoc.casajojo.data.local.RepositorioAccesibilidad
import cl.duoc.casajojo.domain.model.OpcionesAccesibilidad
import cl.duoc.casajojo.domain.model.TemaApp
import io.kotest.matchers.booleans.shouldBeFalse
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.shouldBe
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test

/** Accesibilidad: el ViewModel guarda cada opción y la expone al instante. */
@OptIn(ExperimentalCoroutinesApi::class)
class AccesibilidadViewModelTest {

    /** DataStore en memoria: guardar() actualiza el mismo flujo que se observa. */
    private class RepoEnMemoria : RepositorioAccesibilidad {
        override val opciones = MutableStateFlow(OpcionesAccesibilidad())
        var guardados = 0
        override suspend fun guardar(opciones: OpcionesAccesibilidad) {
            guardados++
            this.opciones.value = opciones
        }
    }

    private val app = mockk<Application>(relaxed = true)
    private val repo = RepoEnMemoria()

    @Before
    fun preparar() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @After
    fun limpiar() = Dispatchers.resetMain()

    @Test
    fun `parte con las opciones de fabrica`() {
        val o = AccesibilidadViewModel(app, repo).opciones.value
        o.tema shouldBe TemaApp.SISTEMA
        o.textoGrande.shouldBeFalse()
        o.reducirAnimaciones.shouldBeFalse()
        o.escalaTexto shouldBe 1f
    }

    @Test
    fun `cambiar el tema se guarda y se aplica al instante`() {
        val vm = AccesibilidadViewModel(app, repo)
        vm.cambiarTema(TemaApp.OSCURO)
        vm.opciones.value.tema shouldBe TemaApp.OSCURO
        repo.guardados shouldBe 1
    }

    @Test
    fun `texto grande escala la tipografia a 1,15 sin tocar lo demas`() {
        val vm = AccesibilidadViewModel(app, repo)
        vm.cambiarTema(TemaApp.CLARO)
        vm.cambiarTextoGrande(true)
        vm.opciones.value.textoGrande.shouldBeTrue()
        vm.opciones.value.escalaTexto shouldBe OpcionesAccesibilidad.ESCALA_GRANDE
        vm.opciones.value.tema shouldBe TemaApp.CLARO
    }

    @Test
    fun `reducir animaciones se activa y desactiva`() {
        val vm = AccesibilidadViewModel(app, repo)
        vm.cambiarReducirAnimaciones(true)
        vm.opciones.value.reducirAnimaciones.shouldBeTrue()
        vm.cambiarReducirAnimaciones(false)
        vm.opciones.value.reducirAnimaciones.shouldBeFalse()
    }

    @Test
    fun `si cambia la cuenta, el ViewModel muestra sus opciones`() {
        val vm = AccesibilidadViewModel(app, repo)
        repo.opciones.value = OpcionesAccesibilidad(tema = TemaApp.OSCURO, textoGrande = true)
        vm.opciones.value.tema shouldBe TemaApp.OSCURO
        vm.opciones.value.textoGrande.shouldBeTrue()
    }

    @Test
    fun `el tema guardado se lee por nombre y lo desconocido vuelve a Sistema`() {
        TemaApp.desde("OSCURO") shouldBe TemaApp.OSCURO
        TemaApp.desde("morado") shouldBe TemaApp.SISTEMA
        TemaApp.desde(null) shouldBe TemaApp.SISTEMA
    }
}
