package cl.duoc.casajojo.ui.viewmodels

import android.app.Application
import cl.duoc.casajojo.data.repository.AuthRepository
import cl.duoc.casajojo.domain.error.RedException
import cl.duoc.casajojo.domain.error.ValidacionException
import io.kotest.matchers.booleans.shouldBeFalse
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.nulls.shouldBeNull
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

/** EFT: "¿Olvidaste tu contraseña?" (correo → código → nueva contraseña). */
@OptIn(ExperimentalCoroutinesApi::class)
class AuthViewModelRecuperarTest {

    private val app = mockk<Application>(relaxed = true)
    private val repo = mockk<AuthRepository>(relaxed = true)

    @Before
    fun preparar() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        every { repo.usuarioActual } returns flowOf(null)
        coEvery { repo.ultimoCorreo() } returns "camila@casajojo.cl"
    }

    @After
    fun limpiar() = Dispatchers.resetMain()

    private fun vm() = AuthViewModel(app, repo)

    @Test
    fun `abrir la recuperacion usa el correo del login`() {
        val vm = vm()
        vm.abrirRecuperacion()
        vm.recuperar.value.correo shouldBe "camila@casajojo.cl"
        vm.recuperar.value.paso shouldBe PasoRecuperacion.CORREO
    }

    @Test
    fun `correo invalido no pide el codigo`() {
        val vm = vm()
        vm.onRecuperarCorreo("camila")
        vm.enviarCodigo()

        vm.recuperar.value.erroresCorreo.correo shouldBe "Ingresa un correo válido (ej.: nombre@correo.cl)"
        coVerify(exactly = 0) { repo.solicitarRecuperacion(any()) }
        vm.onRecuperarCorreo("camila@casajojo.cl")
        vm.recuperar.value.erroresCorreo.correo.shouldBeNull()
    }

    @Test
    fun `enviar codigo pasa al paso 2 con el aviso del servidor`() {
        coEvery { repo.solicitarRecuperacion("camila@casajojo.cl") } returns Result.success("Te enviamos un código")
        val vm = vm()
        vm.abrirRecuperacion()
        vm.enviarCodigo()

        vm.recuperar.value.paso shouldBe PasoRecuperacion.CODIGO
        vm.recuperar.value.mensaje shouldBe "Te enviamos un código"
        vm.recuperar.value.cargando.shouldBeFalse()
        vm.limpiarMensajeRecuperar()
        vm.recuperar.value.mensaje.shouldBeNull()
    }

    @Test
    fun `sin red se queda en el paso 1 con el mensaje de error`() {
        coEvery { repo.solicitarRecuperacion(any()) } returns Result.failure(RedException())
        val vm = vm()
        vm.abrirRecuperacion()
        vm.enviarCodigo()

        vm.recuperar.value.paso shouldBe PasoRecuperacion.CORREO
        vm.recuperar.value.mensaje shouldBe RedException().mensajeUsuario
    }

    @Test
    fun `el codigo acepta solo 6 digitos`() {
        val vm = vm()
        vm.onRecuperarCodigo("12a34567")
        vm.recuperar.value.codigo shouldBe "123456"
    }

    @Test
    fun `restablecer valida los campos antes de llamar al servidor`() {
        val vm = vm()
        vm.onRecuperarCodigo("12")
        vm.onRecuperarPassword("abc")
        vm.onRecuperarConfirmar("xyz")
        vm.restablecerPassword {}

        vm.recuperar.value.errores.codigo shouldBe "El código tiene 6 dígitos"
        vm.recuperar.value.errores.password shouldBe "Debe tener al menos 6 caracteres"
        vm.recuperar.value.errores.confirmar shouldBe "Las contraseñas no coinciden"
        coVerify(exactly = 0) { repo.restablecerPassword(any(), any(), any()) }
        vm.onRecuperarPassword("Nueva123")
        vm.onRecuperarConfirmar("Nueva123")
        vm.recuperar.value.errores.password.shouldBeNull()
        vm.recuperar.value.errores.confirmar.shouldBeNull()
    }

    @Test
    fun `restablecer correcto vuelve al login con el correo y un aviso`() {
        coEvery { repo.restablecerPassword(any(), "123456", "Nueva123") } returns
            Result.success("Contraseña actualizada")
        val vm = vm()
        vm.abrirRecuperacion()
        vm.onRecuperarCorreo(" Camila@CasaJojo.cl ")
        vm.onRecuperarCodigo("123456")
        vm.onRecuperarPassword("Nueva123")
        vm.onRecuperarConfirmar("Nueva123")
        var volvio = false
        vm.restablecerPassword { volvio = true }

        volvio.shouldBeTrue()
        vm.login.value.correo shouldBe "camila@casajojo.cl"
        vm.login.value.mensaje shouldBe "Contraseña actualizada"
        vm.recuperar.value shouldBe RecuperarUiState()
    }

    @Test
    fun `un codigo rechazado se marca bajo el campo codigo`() {
        coEvery { repo.restablecerPassword(any(), any(), any()) } returns
            Result.failure(ValidacionException("El código no es válido o ya venció", campo = "codigo"))
        val vm = vm()
        vm.onRecuperarCorreo("camila@casajojo.cl")
        vm.onRecuperarCodigo("000000")
        vm.onRecuperarPassword("Nueva123")
        vm.onRecuperarConfirmar("Nueva123")
        var volvio = false
        vm.restablecerPassword { volvio = true }

        volvio.shouldBeFalse()
        vm.recuperar.value.errores.codigo shouldBe "El código no es válido o ya venció"
        vm.volverAPedirCodigo()
        vm.recuperar.value.paso shouldBe PasoRecuperacion.CORREO
        vm.recuperar.value.codigo shouldBe ""
    }

    @Test
    fun `otros errores del servidor solo muestran el aviso`() {
        coEvery { repo.restablecerPassword(any(), any(), any()) } returns Result.failure(RedException())
        val vm = vm()
        vm.onRecuperarCorreo("camila@casajojo.cl")
        vm.onRecuperarCodigo("123456")
        vm.onRecuperarPassword("Nueva123")
        vm.onRecuperarConfirmar("Nueva123")
        vm.restablecerPassword {}

        vm.recuperar.value.errores.codigo.shouldBeNull()
        vm.recuperar.value.mensaje shouldBe RedException().mensajeUsuario
    }
}
