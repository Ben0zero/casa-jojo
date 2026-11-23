package cl.duoc.casajojo.data.repository

import android.database.sqlite.SQLiteConstraintException
import cl.duoc.casajojo.data.local.SesionPreferences
import cl.duoc.casajojo.data.local.dao.UsuarioDao
import cl.duoc.casajojo.data.local.entity.Usuario
import cl.duoc.casajojo.data.remote.AuthApi
import cl.duoc.casajojo.data.remote.ErroresRemotos
import cl.duoc.casajojo.data.remote.RetrofitInstance
import cl.duoc.casajojo.data.remote.dto.ActualizarPerfilDto
import cl.duoc.casajojo.data.remote.dto.CambiarPasswordDto
import cl.duoc.casajojo.data.remote.dto.LoginRequestDto
import cl.duoc.casajojo.data.remote.dto.RecuperarRequestDto
import cl.duoc.casajojo.data.remote.dto.RegistroRequestDto
import cl.duoc.casajojo.data.remote.dto.RestablecerRequestDto
import cl.duoc.casajojo.domain.error.SesionExpiradaException
import kotlinx.coroutines.flow.first
import cl.duoc.casajojo.data.remote.dto.UsuarioResponseDto
import cl.duoc.casajojo.domain.usecase.ValidacionesAuth
import cl.duoc.casajojo.utils.Contrasenas
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.withContext
import retrofit2.HttpException

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
    /** EFT: borra los pedidos locales de un usuario al eliminar su cuenta (lo da PedidoDao). */
    private val pedidosDelUsuario: (suspend (Long) -> Unit)? = null,
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
        // M8: si el servidor RECHAZA el registro (4xx), no se crea una cuenta solo
        // local: antes un correo ya registrado en otro teléfono quedaba "registrado"
        // aquí con otra contraseña distinta a la del servidor.
        val remoto = when (
            val r = ejecutarRemoto {
                api.registrar(
                    RegistroRequestDto(
                        nombre = nombre.trim(),
                        correo = correoNormal,
                        telefono = telefonoNormal,
                        password = password,
                    )
                )
            }
        ) {
            is Remoto.Ok -> r.valor
            is Remoto.Rechazado -> return if (r.codigo == HTTP_CONFLICTO) {
                ResultadoAuth.Error("Ya existe una cuenta con ese correo", campoCorreo = true)
            } else {
                ResultadoAuth.Error("No pudimos crear la cuenta, revisa los datos")
            }
            Remoto.SinServicio -> null
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

        val remoto = when (
            val r = ejecutarRemoto {
                api.login(LoginRequestDto(correo = correoNormal, password = password))
            }
        ) {
            is Remoto.Ok -> r.valor
            // M7: el servidor dijo "credenciales incorrectas" (401 u otro 4xx). NO se
            // prueba la caché local: una contraseña antigua guardada en el teléfono
            // no debe servir si el servidor ya la rechazó.
            is Remoto.Rechazado -> {
                // Excepción: una cuenta creada SIN conexión solo existe en el teléfono, así
                // que el servidor responde 401 aunque la contraseña sea la correcta. Si
                // coincide con la local, se registra ahora en ms-auth y se sigue con su token.
                // Si el correo ya existe en el servidor con otra clave (409), se rechaza.
                if (r.codigo == HTTP_NO_AUTORIZADO) {
                    registrarCuentaSoloLocal(correoNormal, password)?.let { return it }
                }
                return error
            }
            // Sin red o servidor caído: se permite el login con la caché local.
            Remoto.SinServicio -> null
        }

        if (remoto != null && !remoto.token.isNullOrBlank()) {
            val usuarioRemoto = usuarios.porCorreo(correoNormal) ?: crearCopiaLocal(correoNormal, password, remoto)
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

    // ---------------- EFT: recuperación de contraseña ----------------

    /** Paso 1: ms-auth envía un código de 6 dígitos al correo (vence en 15 min). */
    suspend fun solicitarRecuperacion(correo: String): Result<String> = remoto {
        val correoNormal = ValidacionesAuth.normalizarCorreo(correo)
        api.recuperar(RecuperarRequestDto(correoNormal)).mensaje
            ?: "Si el correo está registrado, te enviamos un código"
    }

    /**
     * Paso 2: cambia la contraseña con el código. Si la cuenta también está en Room,
     * se actualiza su hash local para que el login sin conexión use la clave nueva.
     */
    suspend fun restablecerPassword(correo: String, codigo: String, nueva: String): Result<String> =
        remoto(en400 = "El código no es válido o ya venció", campo400 = "codigo") {
            val correoNormal = ValidacionesAuth.normalizarCorreo(correo)
            val respuesta = api.restablecer(RestablecerRequestDto(correoNormal, codigo.trim(), nueva))
            usuarios.porCorreo(correoNormal)?.let { guardarHashLocal(it, nueva) }
            respuesta.mensaje ?: "Contraseña actualizada. Ya puedes iniciar sesión"
        }

    // ---------------- EFT: perfil (CRUD de la cuenta propia) ----------------

    /** READ: trae el perfil de ms-auth y refresca la copia de Room. */
    suspend fun refrescarPerfil(): Result<Usuario> = remotoConSesion { local ->
        val dto = api.perfil()
        val actualizado = local.copy(
            nombre = dto.nombre?.takeIf { it.isNotBlank() } ?: local.nombre,
            telefono = dto.telefono?.let { ValidacionesAuth.normalizarTelefono(it) }?.takeIf { it.isNotBlank() }
                ?: local.telefono,
        )
        if (actualizado != local) usuarios.actualizar(actualizado)
        actualizado
    }

    /** UPDATE: nombre y teléfono en ms-auth y luego en Room. */
    suspend fun actualizarPerfil(nombre: String, telefono: String): Result<Usuario> =
        remotoConSesion { local ->
            val telefonoNormal = ValidacionesAuth.normalizarTelefono(telefono)
            val dto = api.actualizarPerfil(ActualizarPerfilDto(nombre.trim(), telefonoNormal))
            val actualizado = local.copy(
                nombre = dto.nombre?.takeIf { it.isNotBlank() } ?: nombre.trim(),
                telefono = dto.telefono?.takeIf { it.isNotBlank() } ?: telefonoNormal,
            )
            usuarios.actualizar(actualizado)
            actualizado
        }

    /** UPDATE: contraseña (pide la actual). Room guarda el nuevo hash. */
    suspend fun cambiarPassword(actual: String, nueva: String): Result<Unit> =
        remotoConSesion(en400 = null, campo400 = "passwordActual") { local ->
            api.cambiarPassword(CambiarPasswordDto(actual, nueva))
            guardarHashLocal(local, nueva)
        }

    /** DELETE: borra la cuenta en ms-auth, su copia y sus pedidos en Room, y cierra la sesión. */
    suspend fun eliminarCuenta(): Result<Unit> = remotoConSesion { local ->
        val respuesta = api.eliminarCuenta()
        if (!respuesta.isSuccessful) throw HttpException(respuesta)
        pedidosDelUsuario?.invoke(local.id)
        usuarios.eliminar(local)
        cerrarSesion()
    }

    /** Usuario con sesión iniciada en Room; falla si no hay sesión o no hay token del servidor. */
    private suspend fun usuarioConSesion(): Usuario {
        val id = sesion.usuarioId.first() ?: throw SesionExpiradaException()
        if (sesion.token().isBlank()) throw SesionExpiradaException()
        return usuarios.buscarPorId(id) ?: throw SesionExpiradaException()
    }

    private suspend fun guardarHashLocal(usuario: Usuario, password: String) {
        val salt = Contrasenas.generarSalt()
        val hash = withContext(Dispatchers.Default) { Contrasenas.hash(password, salt) }
        usuarios.actualizar(usuario.copy(passwordHash = hash, salt = salt))
    }

    /** Ejecuta una llamada a ms-auth y traduce cualquier falla a [CasaJojoException]. */
    private suspend fun <T> remoto(
        en400: String? = null,
        campo400: String? = null,
        bloque: suspend () -> T,
    ): Result<T> = try {
        Result.success(bloque())
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        Result.failure(ErroresRemotos.traducir(e, en400 = en400, campo400 = campo400))
    }

    private suspend fun <T> remotoConSesion(
        en400: String? = null,
        campo400: String? = null,
        bloque: suspend (Usuario) -> T,
    ): Result<T> = remoto(en400, campo400) { bloque(usuarioConSesion()) }

    /**
     * Sube a ms-auth una cuenta que se registró sin conexión (solo está en Room).
     * Devuelve Exito con la sesión iniciada, o null si no corresponde o el servidor
     * no lo acepta (p. ej. 409: el correo ya existe allá con otra contraseña).
     */
    private suspend fun registrarCuentaSoloLocal(correo: String, password: String): ResultadoAuth? {
        val local = usuarios.porCorreo(correo) ?: return null
        val coincide = withContext(Dispatchers.Default) {
            Contrasenas.verificar(password, local.salt, local.passwordHash)
        }
        if (!coincide) return null
        val r = ejecutarRemoto {
            api.registrar(
                RegistroRequestDto(
                    nombre = local.nombre,
                    correo = correo,
                    telefono = local.telefono.takeIf { it.isNotBlank() },
                    password = password,
                )
            )
        }
        val token = (r as? Remoto.Ok)?.valor?.token?.takeIf { it.isNotBlank() } ?: return null
        sesion.iniciar(local.id, correo, token)
        return ResultadoAuth.Exito(local)
    }

    /** Crea la copia local de una cuenta que solo existe en la API. */
    private suspend fun crearCopiaLocal(correo: String, password: String, remoto: UsuarioResponseDto): Usuario {
        val salt = Contrasenas.generarSalt()
        val hash = withContext(Dispatchers.Default) { Contrasenas.hash(password, salt) }
        val usuario = Usuario(
            // B5: se usan el nombre y el teléfono que devuelve ms-auth (antes se perdían).
            nombre = remoto.nombre?.takeIf { it.isNotBlank() } ?: correo.substringBefore("@"),
            correo = correo,
            telefono = remoto.telefono?.let { ValidacionesAuth.normalizarTelefono(it) }.orEmpty(),
            passwordHash = hash,
            salt = salt,
        )
        val id = usuarios.insertar(usuario)
        return usuario.copy(id = id)
    }

    /**
     * Las fallas de red no tiran la app. M7/M8: se distingue un RECHAZO del
     * servidor (HTTP 4xx: datos malos o correo repetido) de la FALTA de servicio
     * (sin red, timeout o HTTP 5xx), porque solo en el segundo caso tiene sentido
     * seguir en modo local.
     */
    private suspend fun <T> ejecutarRemoto(bloque: suspend () -> T): Remoto<T> = try {
        Remoto.Ok(bloque())
    } catch (e: CancellationException) {
        throw e
    } catch (e: HttpException) {
        if (e.code() in 400..499) Remoto.Rechazado(e.code()) else Remoto.SinServicio
    } catch (e: Exception) {
        Remoto.SinServicio
    }

    /** Resultado de una llamada a ms-auth (M7/M8). */
    private sealed class Remoto<out T> {
        /** El servidor respondió bien. */
        data class Ok<T>(val valor: T) : Remoto<T>()

        /** El servidor respondió 4xx: credenciales incorrectas, datos inválidos o correo repetido. */
        data class Rechazado(val codigo: Int) : Remoto<Nothing>()

        /** Sin red, timeout o error 5xx: no hubo respuesta útil del servidor. */
        object SinServicio : Remoto<Nothing>()
    }

    private companion object {
        const val HTTP_CONFLICTO = 409
        const val HTTP_NO_AUTORIZADO = 401
    }
}
