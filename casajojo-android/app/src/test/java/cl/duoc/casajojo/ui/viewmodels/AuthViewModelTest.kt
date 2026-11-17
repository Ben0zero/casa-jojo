package cl.duoc.casajojo.ui.viewmodels

import android.app.Application
import cl.duoc.casajojo.data.local.entity.Usuario
import cl.duoc.casajojo.data.repository.AuthRepository
import cl.duoc.casajojo.data.repository.ResultadoAuth
import io.kotest.matchers.booleans.shouldBeFalse
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test

/**
 * EA3 / IL3.2: el ViewModel se prueba con el repositorio inyectado ( patrón
 * MVVM de la guía). viewModelScope usa Dispatchers.Main, así que se sustituye
 * por un dispatcher de prueba antes de crear el ViewModel.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class AuthViewModelTest {

    private val app = mockk<Application>(relaxed = true)
    private val repo = mockk<AuthRepository>(relaxed = true)

    @Before
    fun preparar() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        every { repo.usuarioActual } returns flowOf(null)
        coEvery { repo.ultimoCorreo() } returns ""
    }

    @After
    fun limpiar() {
        Dispatchers.resetMain()
    }

    private fun vm(): AuthViewModel = AuthViewModel(app, repo)

    private fun usuario() = Usuario(
        id = 3L,
        nombre = "Camila",
        correo = "camila@casajojo.cl",
        telefono = "912345678",
        passwordHash = "hash",
        salt = "salt",
    )

    @Test
    fun `login con campos invalidos marca errores y no consulta al repositorio`() {
        val vm = vm()
        vm.onLoginCorreo("correo-invalido")
        vm.onLoginPassword("")

        var navego = false
        vm.iniciarSesion { navego = true }

        val estado = vm.login.value
        estado.errores.correo.shouldNotBeNull()
        estado.errores.password.shouldNotBeNull()
        estado.mensaje shouldBe "Revisa los campos marcados"
        navego.shouldBeFalse()
        coVerify(exactly = 0) { repo.iniciarSesion(any(), any()) }
    }

    @Test
    fun `login correcto avisa a la navegacion y borra la contrasena del formulario`() {
        coEvery { repo.iniciarSesion("camila@casajojo.cl", "Cliente123") } returns
            ResultadoAuth.Exito(usuario())
        val vm = vm()
        vm.onLoginCorreo("camila@casajojo.cl")
        vm.onLoginPassword("Cliente123")

        var navego = false
        vm.iniciarSesion { navego = true }

        navego.shouldBeTrue()
        vm.login.value.password shouldBe ""
        vm.login.value.correo shouldBe "camila@casajojo.cl"
        vm.login.value.cargando.shouldBeFalse()
    }

    @Test
    fun `login rechazado muestra el mensaje y permite reintentar`() {
        coEvery { repo.iniciarSesion(any(), any()) } returns
            ResultadoAuth.Error("Correo o contraseña incorrectos")
        val vm = vm()
        vm.onLoginCorreo("camila@casajojo.cl")
        vm.onLoginPassword("Mala123")
        var navego = false

        vm.iniciarSesion { navego = true }

        val estado = vm.login.value
        estado.mensaje shouldBe "Correo o contraseña incorrectos"
        estado.password shouldBe ""
        estado.cargando.shouldBeFalse()
        navego.shouldBeFalse()
    }

    @Test
    fun `registro con correo repetido marca el error bajo el campo correo`() {
        coEvery { repo.registrar(any(), any(), any(), any()) } returns
            ResultadoAuth.Error("Ya existe una cuenta con ese correo", campoCorreo = true)
        val vm = vm()
        vm.onRegistroNombre("Camila González")
        vm.onRegistroCorreo("camila@casajojo.cl")
        vm.onRegistroTelefono("912345678")
        vm.onRegistroPassword("Cliente123")
        vm.onRegistroConfirmar("Cliente123")
        var navego = false

        vm.registrar { navego = true }

        val estado = vm.registro.value
        estado.errores.correo shouldBe "Ya existe una cuenta con ese correo"
        estado.guardando.shouldBeFalse()
        navego.shouldBeFalse()
    }

    @Test
    fun `registro correcto limpia el formulario y navega`() {
        coEvery { repo.registrar(any(), any(), any(), any()) } returns
            ResultadoAuth.Exito(usuario())
        val vm = vm()
        vm.onRegistroNombre("Camila González")
        vm.onRegistroCorreo("camila@casajojo.cl")
        vm.onRegistroTelefono("912345678")
        vm.onRegistroPassword("Cliente123")
        vm.onRegistroConfirmar("Cliente123")

        var navego = false
        vm.registrar { navego = true }

        navego.shouldBeTrue()
        vm.registro.value.nombre shouldBe ""
        vm.login.value.correo shouldBe "camila@casajojo.cl"
    }

    @Test
    fun `cerrar sesion pasa por el repositorio y ejecuta el callback`() {
        val vm = vm()
        var volvioAlLogin = false

        vm.cerrarSesion { volvioAlLogin = true }

        coVerify { repo.cerrarSesion() }
        volvioAlLogin.shouldBeTrue()
    }
}
