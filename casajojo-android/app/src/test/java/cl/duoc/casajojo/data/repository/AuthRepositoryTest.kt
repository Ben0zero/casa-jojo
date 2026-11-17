package cl.duoc.casajojo.data.repository

import cl.duoc.casajojo.data.local.SesionPreferences
import cl.duoc.casajojo.data.local.dao.UsuarioDao
import cl.duoc.casajojo.data.local.entity.Usuario
import cl.duoc.casajojo.data.remote.AuthApi
import cl.duoc.casajojo.data.remote.dto.LoginRequestDto
import cl.duoc.casajojo.data.remote.dto.RegistroRequestDto
import cl.duoc.casajojo.data.remote.dto.UsuarioResponseDto
import cl.duoc.casajojo.utils.Contrasenas
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.test.runTest
import org.junit.Test
import java.io.IOException

/**
 * EA3 / IL3.2: pruebas del repositorio de autenticación.
 * Se mockean Room, DataStore y Retrofit: ninguna prueba necesita un teléfono
 * ni un microservicio levantado.
 */
class AuthRepositoryTest {

    private val usuarios = mockk<UsuarioDao>(relaxed = true)
    private val sesion = mockk<SesionPreferences>(relaxed = true)
    private val api = mockk<AuthApi>(relaxed = true)
    private val repo = AuthRepository(usuarios, sesion, api)

    private fun usuarioLocal(password: String = "Cliente123"): Usuario {
        val salt = Contrasenas.generarSalt()
        return Usuario(
            id = 11L,
            nombre = "Camila González",
            correo = "camila@casajojo.cl",
            telefono = "912345678",
            passwordHash = Contrasenas.hash(password, salt),
            salt = salt,
        )
    }

    @Test
    fun `login remoto guarda el token y devuelve el usuario local`() = runTest {
        val local = usuarioLocal()
        coEvery { usuarios.porCorreo("camila@casajojo.cl") } returns local
        coEvery { api.login(any()) } returns UsuarioResponseDto(
            id = 55L,
            nombre = "Camila",
            correo = "camila@casajojo.cl",
            token = "jwt-de-prueba",
        )

        val resultado = repo.iniciarSesion("  Camila@Casajojo.cl ", "Cliente123")

        val exito = resultado.shouldBeInstanceOf<ResultadoAuth.Exito>()
        exito.usuario.id shouldBe 11L
        coVerify { api.login(LoginRequestDto("camila@casajojo.cl", "Cliente123")) }
        coVerify { sesion.iniciar(11L, "camila@casajojo.cl", "jwt-de-prueba") }
    }

    @Test
    fun `login remoto sin token cierra sesion con la contrasena guardada en cache`() = runTest {
        val local = usuarioLocal()
        coEvery { usuarios.porCorreo(any()) } returns local
        coEvery { api.login(any()) } returns UsuarioResponseDto(token = null)

        val resultado = repo.iniciarSesion("camila@casajojo.cl", "Cliente123")

        resultado.shouldBeInstanceOf<ResultadoAuth.Exito>()
        coVerify { sesion.iniciar(11L, "camila@casajojo.cl", null) }
    }

    @Test
    fun `login sin red rechaza una contrasena incorrecta usando la cache`() = runTest {
        val local = usuarioLocal(password = "LaCorrecta123")
        coEvery { usuarios.porCorreo(any()) } returns local
        coEvery { api.login(any()) } throws IOException("sin conexión")

        val resultado = repo.iniciarSesion("camila@casajojo.cl", "Incorrecta123")

        val error = resultado.shouldBeInstanceOf<ResultadoAuth.Error>()
        error.mensaje shouldBe "Correo o contraseña incorrectos"
        coVerify(exactly = 0) { sesion.iniciar(any(), any(), any()) }
    }

    @Test
    fun `registro normaliza correo y telefono y guarda el token de la API`() = runTest {
        coEvery { usuarios.porCorreo(any()) } returns null
        coEvery { usuarios.insertar(any()) } returns 7L
        coEvery { api.registrar(any()) } returns UsuarioResponseDto(
            nombre = "Camila González",
            correo = "camila@casajojo.cl",
            telefono = "912345678",
            token = "token-nuevo",
        )

        val resultado = repo.registrar(
            nombre = " Camila González ",
            correo = " Camila@Casajojo.cl ",
            telefono = "+56 9 1234 5678",
            password = "Cliente123",
        )

        resultado.shouldBeInstanceOf<ResultadoAuth.Exito>()
        val enviado = slot<RegistroRequestDto>()
        coVerify { api.registrar(capture(enviado)) }
        enviado.captured.correo shouldBe "camila@casajojo.cl"
        enviado.captured.telefono shouldBe "912345678"

        val guardado = slot<Usuario>()
        coVerify { usuarios.insertar(capture(guardado)) }
        guardado.captured.passwordHash shouldBe Contrasenas.hash("Cliente123", guardado.captured.salt)
        coVerify { sesion.iniciar(7L, "camila@casajojo.cl", "token-nuevo") }
    }

    @Test
    fun `registro con correo ya guardado localmente no llama a la API`() = runTest {
        coEvery { usuarios.porCorreo(any()) } returns usuarioLocal()

        val resultado = repo.registrar(
            nombre = "Otra persona",
            correo = "camila@casajojo.cl",
            telefono = "912345678",
            password = "Cliente123",
        )

        val error = resultado.shouldBeInstanceOf<ResultadoAuth.Error>()
        error.campoCorreo shouldBe true
        coVerify(exactly = 0) { api.registrar(any()) }
    }
}
