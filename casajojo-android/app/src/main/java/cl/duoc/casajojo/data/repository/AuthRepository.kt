package cl.duoc.casajojo.data.repository

import android.database.sqlite.SQLiteConstraintException
import cl.duoc.casajojo.data.local.CasaJojoDatabase
import cl.duoc.casajojo.data.local.SesionPreferences
import cl.duoc.casajojo.data.local.entity.Usuario
import cl.duoc.casajojo.utils.Contrasenas
import cl.duoc.casajojo.domain.usecase.ValidacionesAuth
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
 * Todo lo de cuentas y sesión pasa por aquí:
 * - Room (tabla usuarios) guarda las cuentas.
 * - DataStore (SesionPreferences) recuerda quién inició sesión.
 */
class AuthRepository(
    db: CasaJojoDatabase,
    private val sesion: SesionPreferences,
) {
    private val usuarios = db.usuarioDao()

    /**
     * Usuario con sesión activa (o null).
     * flatMapLatest: cada vez que cambia el id guardado en DataStore, deja de
     * escuchar al usuario anterior y empieza a escuchar al nuevo en Room.
     */
    @OptIn(ExperimentalCoroutinesApi::class)
    val usuarioActual: Flow<Usuario?> = sesion.usuarioId.flatMapLatest { id ->
        if (id == null) flowOf(null) else usuarios.porId(id)
    }

    suspend fun ultimoCorreo(): String = sesion.ultimoCorreo()

    /** Crea la cuenta, cifra la contraseña y deja la sesión iniciada. */
    suspend fun registrar(
        nombre: String,
        correo: String,
        telefono: String,
        password: String,
    ): ResultadoAuth {
        val correoNormal = ValidacionesAuth.normalizarCorreo(correo)

        if (usuarios.porCorreo(correoNormal) != null) {
            return ResultadoAuth.Error("Ya existe una cuenta con ese correo", campoCorreo = true)
        }

        // El cifrado es un cálculo pesado: se hace en Dispatchers.Default para
        // no congelar la pantalla.
        val salt = Contrasenas.generarSalt()
        val hash = withContext(Dispatchers.Default) { Contrasenas.hash(password, salt) }

        val nuevo = Usuario(
            nombre = nombre.trim(),
            correo = correoNormal,
            telefono = ValidacionesAuth.normalizarTelefono(telefono),
            passwordHash = hash,
            salt = salt,
        )
        val id = try {
            usuarios.insertar(nuevo)
        } catch (e: SQLiteConstraintException) {
            // Por si otro registro con el mismo correo entró justo entre la
            // búsqueda y la inserción: el índice único lo bloquea.
            return ResultadoAuth.Error("Ya existe una cuenta con ese correo", campoCorreo = true)
        }

        sesion.iniciar(id, correoNormal)
        return ResultadoAuth.Exito(nuevo.copy(id = id))
    }

    /** Verifica correo y contraseña; si coinciden, guarda la sesión. */
    suspend fun iniciarSesion(correo: String, password: String): ResultadoAuth {
        val correoNormal = ValidacionesAuth.normalizarCorreo(correo)

        // Usamos el MISMO mensaje si el correo no existe o si la contraseña está
        // mal: así nadie puede averiguar qué correos tienen cuenta.
        val error = ResultadoAuth.Error("Correo o contraseña incorrectos")

        val usuario = usuarios.porCorreo(correoNormal) ?: return error
        val correcta = withContext(Dispatchers.Default) {
            Contrasenas.verificar(password, usuario.salt, usuario.passwordHash)
        }
        if (!correcta) return error

        sesion.iniciar(usuario.id, usuario.correo)
        return ResultadoAuth.Exito(usuario)
    }

    suspend fun cerrarSesion() = sesion.cerrar()
}