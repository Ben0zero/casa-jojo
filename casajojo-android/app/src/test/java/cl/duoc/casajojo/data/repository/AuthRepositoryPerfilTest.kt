package cl.duoc.casajojo.data.repository

import cl.duoc.casajojo.data.local.SesionPreferences
import cl.duoc.casajojo.data.local.dao.UsuarioDao
import cl.duoc.casajojo.data.local.entity.Usuario
import cl.duoc.casajojo.data.remote.AuthApi
import cl.duoc.casajojo.data.remote.dto.ActualizarPerfilDto
import cl.duoc.casajojo.data.remote.dto.CambiarPasswordDto
import cl.duoc.casajojo.data.remote.dto.MensajeDto
import cl.duoc.casajojo.data.remote.dto.RecuperarRequestDto
import cl.duoc.casajojo.data.remote.dto.RestablecerRequestDto
import cl.duoc.casajojo.data.remote.dto.UsuarioResponseDto
import cl.duoc.casajojo.domain.error.RedException
import cl.duoc.casajojo.domain.error.SesionExpiradaException
import cl.duoc.casajojo.domain.error.ValidacionException
import cl.duoc.casajojo.utils.Contrasenas
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Before
import org.junit.Test
import retrofit2.HttpException
import retrofit2.Response
import java.io.IOException

/** EFT: recuperación de contraseña y CRUD del perfil, con Room/DataStore/Retrofit mockeados. */
class AuthRepositoryPerfilTest {

    private val usuarios = mockk<UsuarioDao>(relaxed = true)
    private val sesion = mockk<SesionPreferences>(relaxed = true)
    private val api = mockk<AuthApi>(relaxed = true)
    private val borrados = mutableListOf<Long>()
    private val repo = AuthRepository(usuarios, sesion, api, pedidosDelUsuario = { borrados += it })

    private val salt = Contrasenas.generarSalt()
    private val local = Usuario(
        id = 11L,
        nombre = "Camila",
        correo = "camila@casajojo.cl",
        telefono = "912345678",
        passwordHash = Contrasenas.hash("Vieja123", salt),
        salt = salt,
    )

    private fun http(codigo: Int, cuerpo: String = "") =
        HttpException(Response.error<Any>(codigo, cuerpo.toResponseBody()))

    @Before
    fun sesionActiva() {
        every { sesion.usuarioId } returns flowOf(11L)
        coEvery { sesion.token() } returns "jwt"
        coEvery { usuarios.buscarPorId(11L) } returns local
    }

    // ---------- recuperación ----------

    @Test
    fun `solicitar recuperacion normaliza el correo y devuelve el mensaje del servidor`() = runTest {
        coEvery { api.recuperar(any()) } returns MensajeDto("Te enviamos un código")

        repo.solicitarRecuperacion("  Camila@CasaJojo.cl ").getOrThrow() shouldBe "Te enviamos un código"
        coVerify { api.recuperar(RecuperarRequestDto("camila@casajojo.cl")) }
    }

    @Test
    fun `solicitar recuperacion sin red falla con RedException`() = runTest {
        coEvery { api.recuperar(any()) } throws IOException("sin red")

        repo.solicitarRecuperacion("camila@casajojo.cl").exceptionOrNull().shouldBeInstanceOf<RedException>()
    }

    @Test
    fun `restablecer actualiza el hash de la copia local en Room`() = runTest {
        coEvery { api.restablecer(any()) } returns MensajeDto(null)
        coEvery { usuarios.porCorreo("camila@casajojo.cl") } returns local

        repo.restablecerPassword("camila@casajojo.cl", " 482913 ", "Nueva123").getOrThrow() shouldBe
            "Contraseña actualizada. Ya puedes iniciar sesión"

        coVerify { api.restablecer(RestablecerRequestDto("camila@casajojo.cl", "482913", "Nueva123")) }
        val guardado = slot<Usuario>()
        coVerify { usuarios.actualizar(capture(guardado)) }
        Contrasenas.verificar("Nueva123", guardado.captured.salt, guardado.captured.passwordHash).shouldBeTrue()
    }

    @Test
    fun `restablecer con codigo invalido marca el campo codigo`() = runTest {
        coEvery { api.restablecer(any()) } throws http(400)

        val error = repo.restablecerPassword("camila@casajojo.cl", "000000", "Nueva123").exceptionOrNull()
        error.shouldBeInstanceOf<ValidacionException>()
        error.campo shouldBe "codigo"
        error.message shouldBe "El código no es válido o ya venció"
    }

    // ---------- perfil ----------

    @Test
    fun `refrescar perfil trae los datos de ms-auth y actualiza Room`() = runTest {
        coEvery { api.perfil() } returns UsuarioResponseDto(nombre = "Camila Rojas", telefono = "+56987654321")

        val u = repo.refrescarPerfil().getOrThrow()
        u.nombre shouldBe "Camila Rojas"
        u.telefono shouldBe "987654321"
        coVerify { usuarios.actualizar(u) }
    }

    @Test
    fun `refrescar perfil sin cambios no escribe en Room`() = runTest {
        coEvery { api.perfil() } returns UsuarioResponseDto(nombre = "Camila", telefono = "912345678")

        repo.refrescarPerfil().getOrThrow() shouldBe local
        coVerify(exactly = 0) { usuarios.actualizar(any()) }
    }

    @Test
    fun `actualizar perfil envia nombre y telefono normalizados y sincroniza Room`() = runTest {
        coEvery { api.actualizarPerfil(any()) } returns UsuarioResponseDto(nombre = "Cami", telefono = "987654321")

        val u = repo.actualizarPerfil(" Cami ", "9 8765 4321").getOrThrow()
        coVerify { api.actualizarPerfil(ActualizarPerfilDto("Cami", "987654321")) }
        u.nombre shouldBe "Cami"
        coVerify { usuarios.actualizar(u) }
    }

    @Test
    fun `sin token del servidor el perfil pide volver a iniciar sesion`() = runTest {
        coEvery { sesion.token() } returns ""

        repo.actualizarPerfil("Cami", "987654321").exceptionOrNull().shouldBeInstanceOf<SesionExpiradaException>()
        coVerify(exactly = 0) { api.actualizarPerfil(any()) }
    }

    @Test
    fun `sin sesion iniciada tambien falla con SesionExpirada`() = runTest {
        every { sesion.usuarioId } returns flowOf(null)

        repo.refrescarPerfil().exceptionOrNull().shouldBeInstanceOf<SesionExpiradaException>()
    }

    @Test
    fun `cambiar contraseña guarda el nuevo hash local`() = runTest {
        coEvery { api.cambiarPassword(any()) } returns MensajeDto("ok")

        repo.cambiarPassword("Vieja123", "Nueva123").isSuccess.shouldBeTrue()
        coVerify { api.cambiarPassword(CambiarPasswordDto("Vieja123", "Nueva123")) }
        val guardado = slot<Usuario>()
        coVerify { usuarios.actualizar(capture(guardado)) }
        Contrasenas.verificar("Nueva123", guardado.captured.salt, guardado.captured.passwordHash).shouldBeTrue()
    }

    @Test
    fun `cambiar contraseña con la actual incorrecta usa el mensaje del servidor`() = runTest {
        coEvery { api.cambiarPassword(any()) } throws
            http(400, """{"mensaje":"La contraseña actual no es correcta"}""")

        val error = repo.cambiarPassword("Mala123", "Nueva123").exceptionOrNull()
        error.shouldBeInstanceOf<ValidacionException>()
        error.message shouldBe "La contraseña actual no es correcta"
        error.campo shouldBe "passwordActual"
        coVerify(exactly = 0) { usuarios.actualizar(any()) }
    }

    @Test
    fun `eliminar cuenta borra en ms-auth, Room y cierra la sesion`() = runTest {
        coEvery { api.eliminarCuenta() } returns Response.success(204, Unit)

        repo.eliminarCuenta().isSuccess.shouldBeTrue()
        borrados shouldBe listOf(11L)
        coVerify { usuarios.eliminar(local) }
        coVerify { sesion.cerrar() }
        coVerify { sesion.guardarToken("") }
    }

    @Test
    fun `si ms-auth rechaza la eliminacion no se borra nada local`() = runTest {
        coEvery { api.eliminarCuenta() } returns Response.error(500, "".toResponseBody())

        repo.eliminarCuenta().isFailure.shouldBeTrue()
        borrados shouldBe emptyList()
        coVerify(exactly = 0) { usuarios.eliminar(any()) }
    }
}
