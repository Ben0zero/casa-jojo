package cl.duoc.casajojo.ui.viewmodels

import cl.duoc.casajojo.data.local.entity.Usuario
import cl.duoc.casajojo.data.repository.CuentasRepository
import cl.duoc.casajojo.data.repository.ResultadoAuth
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Pruebas del AuthViewModel sin emulador: se le pasa un repositorio falso en
 * memoria en vez de Room + DataStore.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class AuthViewModelTest {

    /** Repositorio falso: guarda las cuentas en una lista. La contraseña correcta es "clave123". */
    private class CuentasFalsas(private val ultimo: String = "") : CuentasRepository {
        val usuarios = mutableListOf<Usuario>()
        var llamadasRegistrar = 0

        override val usuarioActual = MutableStateFlow<Usuario?>(null)

        override suspend fun ultimoCorreo(): String = ultimo

        override suspend fun registrar(
            nombre: String,
            correo: String,
            telefono: String,
            password: String,
        ): ResultadoAuth {
            llamadasRegistrar++
            if (usuarios.any { it.correo == correo }) {
                return ResultadoAuth.Error("Ya existe una cuenta con ese correo", campoCorreo = true)
            }
            val nuevo = Usuario(
                id = usuarios.size + 1L,
                nombre = nombre,
                correo = correo,
                telefono = telefono,
                passwordHash = "hash",
                salt = "salt",
            )
            usuarios += nuevo
            usuarioActual.value = nuevo
            return ResultadoAuth.Exito(nuevo)
        }

        override suspend fun iniciarSesion(correo: String, password: String): ResultadoAuth {
            val usuario = usuarios.find { it.correo == correo && password == "clave123" }
                ?: return ResultadoAuth.Error("Correo o contraseña incorrectos")
            usuarioActual.value = usuario
            return ResultadoAuth.Exito(usuario)
        }

        override suspend fun cerrarSesion() {
            usuarioActual.value = null
        }
    }

    private lateinit var repo: CuentasFalsas
    private lateinit var vm: AuthViewModel

    // viewModelScope usa Dispatchers.Main, que no existe en la JVM de los tests:
    // lo reemplazamos por un dispatcher de prueba que ejecuta todo al instante.
    @Before
    fun preparar() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        repo = CuentasFalsas(ultimo = "ana@correo.cl")
        vm = AuthViewModel(repo)
    }

    @After
    fun limpiar() {
        Dispatchers.resetMain()
    }

    private fun llenarRegistroValido() {
        vm.onRegistroNombre("Ana Pérez")
        vm.onRegistroCorreo("ana@correo.cl")
        vm.onRegistroTelefono("912345678")
        vm.onRegistroPassword("clave123")
        vm.onRegistroConfirmar("clave123")
    }

    @Test
    fun `Al abrir se rellena el correo con el ultimo usado`() {
        assertEquals("ana@correo.cl", vm.login.value.correo)
    }

    @Test
    fun `Registro vacio marca errores por campo y no llama al repositorio`() {
        vm.registrar(onExito = {})

        val estado = vm.registro.value
        assertTrue(estado.intentado)
        assertNotNull(estado.errores.nombre)
        assertNotNull(estado.errores.correo)
        assertNotNull(estado.errores.telefono)
        assertNotNull(estado.errores.password)
        assertEquals("Revisa los campos marcados", estado.mensaje)
        assertEquals(0, repo.llamadasRegistrar)
    }

    @Test
    fun `Antes del primer envio escribir no marca errores`() {
        vm.onRegistroCorreo("no-es-correo")
        assertNull(vm.registro.value.errores.correo)
    }

    @Test
    fun `Despues del primer envio el campo se revalida al escribir`() {
        vm.registrar(onExito = {})
        assertNotNull(vm.registro.value.errores.correo)

        vm.onRegistroCorreo("ana@correo")
        assertNotNull(vm.registro.value.errores.correo) // aún sin dominio válido

        vm.onRegistroCorreo("ana@correo.cl")
        assertNull(vm.registro.value.errores.correo)
    }

    @Test
    fun `Cambiar la contrasena revalida la confirmacion`() {
        llenarRegistroValido()
        vm.onRegistroConfirmar("otra123")
        vm.registrar(onExito = {})
        assertNotNull(vm.registro.value.errores.confirmar)

        vm.onRegistroPassword("otra123")
        assertNull(vm.registro.value.errores.confirmar)
    }

    @Test
    fun `Registro valido crea la cuenta, limpia el formulario y avisa el exito`() {
        llenarRegistroValido()
        var exito = false

        vm.registrar(onExito = { exito = true })

        assertTrue(exito)
        assertEquals(1, repo.usuarios.size)
        assertEquals(RegistroUiState(), vm.registro.value)
        assertTrue(vm.estadoSesion.value is EstadoSesion.Activa)
    }

    @Test
    fun `Correo repetido se marca bajo el campo correo`() {
        llenarRegistroValido()
        vm.registrar(onExito = {})
        llenarRegistroValido()

        vm.registrar(onExito = {})

        val estado = vm.registro.value
        assertEquals("Ya existe una cuenta con ese correo", estado.errores.correo)
        assertFalse(estado.guardando)
    }

    @Test
    fun `Login con contrasena incorrecta borra la contrasena y muestra el mensaje`() {
        llenarRegistroValido()
        vm.registrar(onExito = {})
        vm.onLoginCorreo("ana@correo.cl")
        vm.onLoginPassword("mala123")
        var exito = false

        vm.iniciarSesion(onExito = { exito = true })

        val estado = vm.login.value
        assertFalse(exito)
        assertEquals("", estado.password)
        assertEquals("Correo o contraseña incorrectos", estado.mensaje)
        assertFalse(estado.cargando)
    }

    @Test
    fun `Cerrar sesion deja la sesion inactiva`() {
        llenarRegistroValido()
        vm.registrar(onExito = {})
        var despues = false

        vm.cerrarSesion { despues = true }

        assertTrue(despues)
        assertEquals(EstadoSesion.Inactiva, vm.estadoSesion.value)
    }
}
