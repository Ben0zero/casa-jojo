package cl.duoc.casajojo.ui.viewmodels

import android.app.Application
import cl.duoc.casajojo.data.local.entity.Usuario
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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test

/** EFT: Perfil (leer, editar, cambiar contraseña y eliminar cuenta). */
@OptIn(ExperimentalCoroutinesApi::class)
class PerfilViewModelTest {

    private val app = mockk<Application>(relaxed = true)
    private val repo = mockk<AuthRepository>(relaxed = true)
    private val usuario = Usuario(11L, "Camila", "camila@casajojo.cl", "912345678", "h", "s")
    private val actual = MutableStateFlow<Usuario?>(usuario)

    @Before
    fun preparar() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        every { repo.usuarioActual } returns actual
        coEvery { repo.refrescarPerfil() } returns Result.success(usuario)
    }

    @After
    fun limpiar() = Dispatchers.resetMain()

    private fun vm() = PerfilViewModel(app, repo)

    @Test
    fun `al abrir muestra los datos de Room y refresca desde ms-auth`() {
        val vm = vm()
        vm.estado.value.nombre shouldBe "Camila"
        vm.estado.value.telefono shouldBe "912345678"
        vm.estado.value.correo shouldBe "camila@casajojo.cl"
        coVerify { repo.refrescarPerfil() }
    }

    @Test
    fun `lo que la persona escribe no se pisa cuando Room cambia`() {
        val vm = vm()
        vm.onNombre("Cami")
        actual.value = usuario.copy(nombre = "Otro", correo = "nuevo@casajojo.cl")
        vm.estado.value.nombre shouldBe "Cami"
        vm.estado.value.correo shouldBe "nuevo@casajojo.cl"
    }

    @Test
    fun `datos invalidos marcan errores por campo sin llamar al repositorio`() {
        val vm = vm()
        vm.onNombre("")
        vm.onTelefono("123")
        vm.guardarDatos()

        vm.estado.value.erroresDatos.nombre shouldBe "El nombre es obligatorio"
        vm.estado.value.erroresDatos.telefono shouldBe "Ingresa un celular de 9 dígitos que empiece con 9"
        vm.estado.value.mensajeEsError.shouldBeTrue()
        coVerify(exactly = 0) { repo.actualizarPerfil(any(), any()) }

        vm.onNombre("Ana")
        vm.estado.value.erroresDatos.nombre.shouldBeNull()
    }

    @Test
    fun `guardar datos validos actualiza y avisa`() {
        coEvery { repo.actualizarPerfil("Cami", "987654321") } returns
            Result.success(usuario.copy(nombre = "Cami", telefono = "987654321"))
        val vm = vm()
        vm.onNombre("Cami")
        vm.onTelefono("987654321")
        vm.guardarDatos()

        vm.estado.value.guardandoDatos.shouldBeFalse()
        vm.estado.value.mensaje shouldBe "Tus datos se guardaron"
        vm.estado.value.mensajeEsError.shouldBeFalse()
        vm.limpiarMensaje()
        vm.estado.value.mensaje.shouldBeNull()
    }

    @Test
    fun `si ms-auth falla al guardar se muestra el mensaje de la excepcion`() {
        coEvery { repo.actualizarPerfil(any(), any()) } returns Result.failure(RedException())
        val vm = vm()
        vm.guardarDatos()

        vm.estado.value.mensaje shouldBe RedException().mensajeUsuario
        vm.estado.value.mensajeEsError.shouldBeTrue()
    }

    @Test
    fun `cambiar contraseña valida y limpia el formulario al terminar`() {
        coEvery { repo.cambiarPassword("Vieja123", "Nueva123") } returns Result.success(Unit)
        val vm = vm()
        vm.cambiarPassword()
        vm.estado.value.erroresPassword.actual shouldBe "Ingresa tu contraseña actual"

        vm.onActual("Vieja123")
        vm.onNueva("Nueva123")
        vm.onConfirmar("Nueva123")
        vm.cambiarPassword()

        vm.estado.value.actual shouldBe ""
        vm.estado.value.nueva shouldBe ""
        vm.estado.value.mensaje shouldBe "Contraseña actualizada"
    }

    @Test
    fun `contraseña actual incorrecta se marca bajo su campo`() {
        coEvery { repo.cambiarPassword(any(), any()) } returns Result.failure(
            ValidacionException("La contraseña actual no es correcta", campo = "passwordActual")
        )
        val vm = vm()
        vm.onActual("Mala123")
        vm.onNueva("Nueva123")
        vm.onConfirmar("Nueva123")
        vm.cambiarPassword()

        vm.estado.value.erroresPassword.actual shouldBe "La contraseña actual no es correcta"
        vm.estado.value.guardandoPassword.shouldBeFalse()
    }

    @Test
    fun `eliminar cuenta pide confirmacion y al confirmar vuelve al login`() {
        coEvery { repo.eliminarCuenta() } returns Result.success(Unit)
        val vm = vm()
        vm.pedirEliminar()
        vm.estado.value.confirmandoEliminar.shouldBeTrue()
        vm.cancelarEliminar()
        vm.estado.value.confirmandoEliminar.shouldBeFalse()

        var volvio = false
        vm.pedirEliminar()
        vm.eliminarCuenta { volvio = true }

        volvio.shouldBeTrue()
        coVerify { repo.eliminarCuenta() }
    }

    @Test
    fun `si no se pudo eliminar la cuenta se queda en el perfil con el error`() {
        coEvery { repo.eliminarCuenta() } returns Result.failure(RedException())
        val vm = vm()
        var volvio = false
        vm.eliminarCuenta { volvio = true }

        volvio.shouldBeFalse()
        vm.estado.value.eliminando.shouldBeFalse()
        vm.estado.value.mensajeEsError.shouldBeTrue()
    }
}
