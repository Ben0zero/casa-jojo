package cl.duoc.casajojo.data.repository

import android.database.sqlite.SQLiteConstraintException
import cl.duoc.casajojo.data.local.SesionPreferences
import cl.duoc.casajojo.data.local.dao.UsuarioDao
import cl.duoc.casajojo.data.local.entity.Usuario
import cl.duoc.casajojo.data.remote.AuthApi
import cl.duoc.casajojo.data.remote.RetrofitInstance
import cl.duoc.casajojo.data.remote.dto.LoginRequestDto
import cl.duoc.casajojo.data.remote.dto.RegistroRequestDto
import cl.duoc.casajojo.domain.usecase.ValidacionesAuth
import cl.duoc.casajojo.utils.Contrasenas
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.withContext

/** Resultado de registrarse o iniciar sesión. */
sealed class ResultadoAuth {
    data class Exito(val usuario: Usuario) : ResultadoAuth()

    /** campoCorreo = true si el error debe mostrarse bajo el campo correo. */
    data class Error(val mensaje: String, val campoCorreo: Boolean = false) : ResultadoAuth()
}

/**
 * Cuentas y sesión con estrategia "API primero, caché después" (EA3).
 *
 * - ms-auth (8081) es la fuente de verdad cuando está disponible.
 * - Room guarda la cuenta y DataStore la sesión + el token, así la app sigue
 *   funcionando sin red (requisito de mantener Room/DataStore como caché).
 */
class AuthRepository(
    private val usuarios: UsuarioDao,
    private val sesion: SesionPreferences,
    private val api: AuthApi = RetrofitInstance.authApi,
) {

    @OptIn(ExperimentalCoroutinesApi::class)
    val usuarioActual: Flow<Usuario?> = sesion.usuarioId.flatMapLatest { id ->
        if (id == null) flowOf(null) else usuarios.porId(id)
    }

    suspend fun ultimoCorreo(): String = sesion.ultimoCorreo()

    /** Crea la cuenta en la API, la guarda en Room y deja la sesión iniciada. */
    suspend fun registrar(
        nombre: String,
        correo: String,
        telefono: String,
        password: String,
    ): ResultadoAuth {
        val correoNormal = ValidacionesAuth.normalizarCorreo(correo)
        val telefonoNormal = ValidacionesAuth.normalizarTelefono(telefono)

        if (usuarios.porCorreo(correoNormal) != null) {
            return ResultadoAuth.Error("Ya existe una cuenta con ese correo", campoCorreo = true)
        }

        val salt = Contrasenas.generarSalt()
        val hash = withContext(Dispatchers.Default) { Contrasenas.hash(password, salt) }

        // Llamada REMOTA. Si el microservicio no responde, seguimos en modo local.
        val remoto = ejecutarRemoto {
            api.registrar(
                RegistroRequestDto(
                    nombre = nombre.trim(),
                    correo = correoNormal,
                    telefono = telefonoNormal,
                    password = password,
                )
            )
        }

        val nuevo = Usuario(
            nombre = remoto?.nombre?.takeIf { it.isNotBlank() } ?: nombre.trim(),
            correo = remoto?.correo?.takeIf { it.isNotBlank() }?.let {
                ValidacionesAuth.normalizarCorreo(it)
            } ?: correoNormal,
            telefono = remoto?.telefono?.takeIf { it.isNotBlank() } ?: telefonoNormal,
            passwordHash = hash,
            salt = salt,
        )

        val id = try {
            usuarios.insertar(nuevo)
        } catch (e: SQLiteConstraintException) {
            return ResultadoAuth.Error("Ya existe una cuenta con ese correo", campoCorreo = true)
        }

        sesion.iniciar(id, nuevo.correo, remoto?.token)
        return ResultadoAuth.Exito(nuevo.copy(id = id))
    }

    /** Login remoto por correo; si falla, verifica contra la caché local. */
    suspend fun iniciarSesion(correo: String, password: String): ResultadoAuth {
        val correoNormal = ValidacionesAuth.normalizarCorreo(correo)
        val error = ResultadoAuth.Error("Correo o contraseña incorrectos")

        val remoto = ejecutarRemoto {
            api.login(LoginRequestDto(correo = correoNormal, password = password))
        }

        if (remoto != null && !remoto.token.isNullOrBlank()) {
            val usuarioRemoto = usuarios.porCorreo(correoNormal) ?: crearCopiaLocal(correoNormal, password)
            sesion.iniciar(usuarioRemoto.id, correoNormal, remoto.token)
            return ResultadoAuth.Exito(usuarioRemoto)
        }

        val usuario = usuarios.porCorreo(correoNormal) ?: return error
        val correcta = withContext(Dispatchers.Default) {
            Contrasenas.verificar(password, usuario.salt, usuario.passwordHash)
        }
        if (!correcta) return error

        sesion.iniciar(usuario.id, usuario.correo)
        return ResultadoAuth.Exito(usuario)
    }

    suspend fun cerrarSesion() {
        sesion.cerrar()
        sesion.guardarToken("")
    }

    /** Crea la copia local de una cuenta que solo existe en la API. */
    private suspend fun crearCopiaLocal(correo: String, password: String): Usuario {
        val salt = Contrasenas.generarSalt()
        val hash = withContext(Dispatchers.Default) { Contrasenas.hash(password, salt) }
        val usuario = Usuario(
            nombre = correo.substringBefore("@"),
            correo = correo,
            telefono = "",
            passwordHash = hash,
            salt = salt,
        )
        val id = usuarios.insertar(usuario)
        return usuario.copy(id = id)
    }

    /** Las fallas de red no tiran la app: se convierten en "sin respuesta". */
    private suspend fun <T> ejecutarRemoto(bloque: suspend () -> T): T? = try {
        bloque()
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        null
    }
}
