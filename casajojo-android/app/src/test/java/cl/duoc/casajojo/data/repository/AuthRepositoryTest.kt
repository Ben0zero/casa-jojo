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
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Test
import retrofit2.HttpException
import retrofit2.Response
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

    @Test
    fun `B5 login de una cuenta que solo existe en el servidor guarda su nombre y telefono`() = runTest {
        coEvery { usuarios.porCorreo(any()) } returns null
        coEvery { usuarios.insertar(any()) } returns 30L
        coEvery { api.login(any()) } returns UsuarioResponseDto(
            nombre = "Camila González",
            correo = "camila@casajojo.cl",
            telefono = "+56912345678",
            token = "jwt",
        )

        repo.iniciarSesion("camila@casajojo.cl", "Cliente123").shouldBeInstanceOf<ResultadoAuth.Exito>()

        val copia = slot<Usuario>()
        coVerify { usuarios.insertar(capture(copia)) }
        copia.captured.nombre shouldBe "Camila González"
        copia.captured.telefono shouldBe "912345678"
    }

    // ---------- M7 / M8: rechazo del servidor vs. falta de servicio ----------

    private fun http(codigo: Int) = HttpException(Response.error<Any>(codigo, "{}".toResponseBody()))

    @Test
    fun `M7 login rechazado por el servidor (401) no cae a la cache local`() = runTest {
        // La contraseña "vieja" sigue en Room, pero el servidor ya la rechazó y el
        // correo SÍ existe allá (registrar → 409): no se entra con la clave vieja.
        coEvery { usuarios.porCorreo(any()) } returns usuarioLocal(password = "ClaveVieja1")
        coEvery { api.login(any()) } throws http(401)
        coEvery { api.registrar(any()) } throws http(409)

        val resultado = repo.iniciarSesion("camila@casajojo.cl", "ClaveVieja1")

        val error = resultado.shouldBeInstanceOf<ResultadoAuth.Error>()
        error.mensaje shouldBe "Correo o contraseña incorrectos"
        coVerify(exactly = 0) { sesion.iniciar(any(), any(), any()) }
    }

    @Test
    fun `cuenta registrada sin conexion se sube a ms-auth en el siguiente login`() = runTest {
        // Solo existe en Room (se creó sin red): el servidor responde 401 al login.
        coEvery { usuarios.porCorreo("camila@casajojo.cl") } returns usuarioLocal(password = "Cliente123")
        coEvery { api.login(any()) } throws http(401)
        coEvery { api.registrar(any()) } returns UsuarioResponseDto(correo = "camila@casajojo.cl", token = "jwt-nuevo")

        val resultado = repo.iniciarSesion("camila@casajojo.cl", "Cliente123")

        resultado.shouldBeInstanceOf<ResultadoAuth.Exito>()
        val enviado = slot<RegistroRequestDto>()
        coVerify { api.registrar(capture(enviado)) }
        enviado.captured.nombre shouldBe "Camila González"
        enviado.captured.telefono shouldBe "912345678"
        coVerify { sesion.iniciar(11L, "camila@casajojo.cl", "jwt-nuevo") }
    }

    @Test
    fun `con 401 y una contrasena que no coincide con la local no se intenta registrar`() = runTest {
        coEvery { usuarios.porCorreo(any()) } returns usuarioLocal(password = "Cliente123")
        coEvery { api.login(any()) } throws http(401)

        repo.iniciarSesion("camila@casajojo.cl", "OtraClave9").shouldBeInstanceOf<ResultadoAuth.Error>()

        coVerify(exactly = 0) { api.registrar(any()) }
    }

    @Test
    fun `M7 login sin red usa la cache local`() = runTest {
        coEvery { usuarios.porCorreo(any()) } returns usuarioLocal(password = "Cliente123")
        coEvery { api.login(any()) } throws IOException("sin conexión")

        val resultado = repo.iniciarSesion("camila@casajojo.cl", "Cliente123")

        resultado.shouldBeInstanceOf<ResultadoAuth.Exito>()
        coVerify { sesion.iniciar(11L, "camila@casajojo.cl", null) }
    }

    @Test
    fun `M7 login con error 500 del servidor usa la cache local`() = runTest {
        coEvery { usuarios.porCorreo(any()) } returns usuarioLocal(password = "Cliente123")
        coEvery { api.login(any()) } throws http(500)

        val resultado = repo.iniciarSesion("camila@casajojo.cl", "Cliente123")

        resultado.shouldBeInstanceOf<ResultadoAuth.Exito>()
    }

    @Test
    fun `M8 registro con correo ya existente en el servidor (409) no crea cuenta local`() = runTest {
        coEvery { usuarios.porCorreo(any()) } returns null
        coEvery { api.registrar(any()) } throws http(409)

        val resultado = repo.registrar("Camila", "camila@casajojo.cl", "912345678", "Cliente123")

        val error = resultado.shouldBeInstanceOf<ResultadoAuth.Error>()
        error.mensaje shouldBe "Ya existe una cuenta con ese correo"
        error.campoCorreo shouldBe true
        coVerify(exactly = 0) { usuarios.insertar(any()) }
        coVerify(exactly = 0) { sesion.iniciar(any(), any(), any()) }
    }

    @Test
    fun `M8 registro rechazado por datos invalidos (400) no crea cuenta local`() = runTest {
        coEvery { usuarios.porCorreo(any()) } returns null
        coEvery { api.registrar(any()) } throws http(400)

        val resultado = repo.registrar("Camila", "camila@casajojo.cl", "912345678", "Cliente123")

        val error = resultado.shouldBeInstanceOf<ResultadoAuth.Error>()
        error.campoCorreo shouldBe false
        coVerify(exactly = 0) { usuarios.insertar(any()) }
    }

    @Test
    fun `M8 registro sin red sigue creando la cuenta local`() = runTest {
        coEvery { usuarios.porCorreo(any()) } returns null
        coEvery { usuarios.insertar(any()) } returns 9L
        coEvery { api.registrar(any()) } throws IOException("sin red")

        val resultado = repo.registrar("Camila", "camila@casajojo.cl", "912345678", "Cliente123")

        resultado.shouldBeInstanceOf<ResultadoAuth.Exito>()
        coVerify { sesion.iniciar(9L, "camila@casajojo.cl", null) }
    }
}
