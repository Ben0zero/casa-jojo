package cl.duoc.casajojo.ui.viewmodels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import cl.duoc.casajojo.data.local.CasaJojoDatabase
import cl.duoc.casajojo.data.local.SesionPreferences
import cl.duoc.casajojo.data.repository.AuthRepository
import cl.duoc.casajojo.data.repository.ResultadoAuth
import cl.duoc.casajojo.domain.error.CasaJojoException
import cl.duoc.casajojo.domain.error.ValidacionException
import cl.duoc.casajojo.domain.usecase.RestablecerErrores
import cl.duoc.casajojo.domain.usecase.ValidacionesAuth
import cl.duoc.casajojo.domain.usecase.ValidacionesPerfil
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * ViewModel de login, registro y sesión.
 *
 * Es AndroidViewModel porque necesita la Application (un Context) para abrir
 * Room y DataStore, igual que en la guía datastore.
 *
 * Cada formulario tiene su ficha en un MutableStateFlow privado (_login, _registro)
 * y se expone como StateFlow de solo lectura. La pantalla lo observa con
 * collectAsState() y se redibuja cuando la ficha cambia.
 */
class AuthViewModel @JvmOverloads constructor(
    app: Application,
    private val repo: AuthRepository = AuthRepository(
        CasaJojoDatabase.get(app).usuarioDao(), SesionPreferences(app)
    ),
) : AndroidViewModel(app) {

    /**
     * Estado de la sesión. Eagerly: empieza a leer DataStore apenas se crea el
     * ViewModel, para decidir cuanto antes si mostrar login o inicio.
     */
    val estadoSesion: StateFlow<EstadoSesion> = repo.usuarioActual
        .map { usuario -> if (usuario == null) EstadoSesion.Inactiva else EstadoSesion.Activa(usuario) }
        .stateIn(viewModelScope, SharingStarted.Eagerly, EstadoSesion.Cargando)

    private val _login = MutableStateFlow(LoginUiState())
    val login: StateFlow<LoginUiState> = _login.asStateFlow()

    private val _registro = MutableStateFlow(RegistroUiState())
    val registro: StateFlow<RegistroUiState> = _registro.asStateFlow()

    init {
        // Al abrir, rellena el correo con el último usado (leído desde DataStore).
        viewModelScope.launch {
            val correo = repo.ultimoCorreo()
            if (correo.isNotBlank()) _login.update { it.copy(correo = correo) }
        }
    }

    // ---------------- LOGIN ----------------
    // Al escribir en un campo, borramos SU error: el aviso rojo desaparece
    // en cuanto la persona empieza a corregirlo.

    fun onLoginCorreo(valor: String) =
        _login.update { it.copy(correo = valor, errores = it.errores.copy(correo = null)) }

    fun onLoginPassword(valor: String) =
        _login.update { it.copy(password = valor, errores = it.errores.copy(password = null)) }

    /** Valida, verifica la contraseña y, si todo está bien, ejecuta onExito (navegar al inicio). */
    fun iniciarSesion(onExito: () -> Unit) {
        val actual = _login.value
        if (actual.cargando) return // evita doble toque

        val errores = ValidacionesAuth.validarLogin(actual.correo, actual.password)
        if (errores.hayErrores) {
            _login.update { it.copy(errores = errores, mensaje = "Revisa los campos marcados") }
            return
        }

        viewModelScope.launch {
            _login.update { it.copy(cargando = true) }
            val resultado = ejecutarSeguro { repo.iniciarSesion(actual.correo, actual.password) }
            when (resultado) {
                is ResultadoAuth.Exito -> {
                    // Limpiamos la contraseña de la ficha; el correo queda para la próxima vez.
                    _login.update { LoginUiState(correo = it.correo) }
                    onExito()
                }
                is ResultadoAuth.Error -> _login.update {
                    it.copy(cargando = false, password = "", mensaje = resultado.mensaje)
                }
            }
        }
    }

    fun limpiarMensajeLogin() = _login.update { it.copy(mensaje = null) }

    // ---------------- REGISTRO ----------------

    fun onRegistroNombre(v: String) =
        _registro.update { it.copy(nombre = v, errores = it.errores.copy(nombre = null)) }

    fun onRegistroCorreo(v: String) =
        _registro.update { it.copy(correo = v, errores = it.errores.copy(correo = null)) }

    fun onRegistroTelefono(v: String) =
        _registro.update { it.copy(telefono = v, errores = it.errores.copy(telefono = null)) }

    fun onRegistroPassword(v: String) =
        _registro.update { it.copy(password = v, errores = it.errores.copy(password = null)) }

    fun onRegistroConfirmar(v: String) =
        _registro.update { it.copy(confirmar = v, errores = it.errores.copy(confirmar = null)) }

    /** Valida todo el formulario, crea la cuenta e inicia sesión. */
    fun registrar(onExito: () -> Unit) {
        val a = _registro.value
        if (a.guardando) return

        val errores = ValidacionesAuth.validarRegistro(a.nombre, a.correo, a.telefono, a.password, a.confirmar)
        if (errores.hayErrores) {
            _registro.update { it.copy(errores = errores, mensaje = "Revisa los campos marcados") }
            return
        }

        viewModelScope.launch {
            _registro.update { it.copy(guardando = true) }
            val resultado = ejecutarSeguro { repo.registrar(a.nombre, a.correo, a.telefono, a.password) }
            when (resultado) {
                is ResultadoAuth.Exito -> {
                    _registro.value = RegistroUiState() // formulario limpio para la próxima vez
                    _login.update { LoginUiState(correo = resultado.usuario.correo) }
                    onExito()
                }
                is ResultadoAuth.Error -> _registro.update {
                    it.copy(
                        guardando = false,
                        mensaje = resultado.mensaje,
                        // Si el problema es el correo, también se marca bajo ese campo.
                        errores = if (resultado.campoCorreo) {
                            it.errores.copy(correo = resultado.mensaje)
                        } else {
                            it.errores
                        },
                    )
                }
            }
        }
    }

    fun limpiarMensajeRegistro() = _registro.update { it.copy(mensaje = null) }

    // ---------------- EFT: ¿OLVIDASTE TU CONTRASEÑA? ----------------

    private val _recuperar = MutableStateFlow(RecuperarUiState())
    val recuperar: StateFlow<RecuperarUiState> = _recuperar.asStateFlow()

    /** Abre el flujo desde el login, con el correo que ya estaba escrito. */
    fun abrirRecuperacion() {
        _recuperar.value = RecuperarUiState(correo = _login.value.correo)
    }

    fun onRecuperarCorreo(v: String) =
        _recuperar.update { it.copy(correo = v, erroresCorreo = it.erroresCorreo.copy(correo = null)) }

    fun onRecuperarCodigo(v: String) =
        _recuperar.update {
            // Solo dígitos y como máximo 6: el teclado numérico igual permite pegar texto.
            it.copy(codigo = v.filter(Char::isDigit).take(ValidacionesPerfil.LARGO_CODIGO),
                errores = it.errores.copy(codigo = null))
        }

    fun onRecuperarPassword(v: String) =
        _recuperar.update { it.copy(password = v, errores = it.errores.copy(password = null)) }

    fun onRecuperarConfirmar(v: String) =
        _recuperar.update { it.copy(confirmar = v, errores = it.errores.copy(confirmar = null)) }

    /** Paso 1 → 2: valida el correo y pide a ms-auth que envíe el código. */
    fun enviarCodigo() {
        val a = _recuperar.value
        if (a.cargando) return
        val errores = ValidacionesPerfil.validarRecuperar(a.correo)
        if (errores.hayErrores) {
            _recuperar.update { it.copy(erroresCorreo = errores, mensaje = "Revisa el correo") }
            return
        }
        viewModelScope.launch {
            _recuperar.update { it.copy(cargando = true) }
            repo.solicitarRecuperacion(a.correo).fold(
                onSuccess = { texto ->
                    _recuperar.update {
                        it.copy(cargando = false, paso = PasoRecuperacion.CODIGO, mensaje = texto)
                    }
                },
                onFailure = { e -> _recuperar.update { it.copy(cargando = false, mensaje = mensajeDe(e)) } },
            )
        }
    }

    /** Paso 2: valida código y contraseña; si ms-auth acepta, vuelve al login con el correo listo. */
    fun restablecerPassword(onExito: () -> Unit) {
        val a = _recuperar.value
        if (a.cargando) return
        val errores = ValidacionesPerfil.validarRestablecer(a.codigo, a.password, a.confirmar)
        if (errores.hayErrores) {
            _recuperar.update { it.copy(errores = errores, mensaje = "Revisa los campos marcados") }
            return
        }
        viewModelScope.launch {
            _recuperar.update { it.copy(cargando = true) }
            repo.restablecerPassword(a.correo, a.codigo, a.password).fold(
                onSuccess = { texto ->
                    _recuperar.value = RecuperarUiState()
                    _login.update {
                        LoginUiState(correo = ValidacionesAuth.normalizarCorreo(a.correo), mensaje = texto)
                    }
                    onExito()
                },
                onFailure = { e ->
                    val mensaje = mensajeDe(e)
                    _recuperar.update {
                        it.copy(
                            cargando = false,
                            mensaje = mensaje,
                            // Un código malo se marca bajo su campo.
                            errores = if ((e as? ValidacionException)?.campo == "codigo") {
                                it.errores.copy(codigo = mensaje)
                            } else {
                                it.errores
                            },
                        )
                    }
                },
            )
        }
    }

    /** Volver al paso 1 (por ejemplo, para corregir el correo o pedir otro código). */
    fun volverAPedirCodigo() =
        _recuperar.update { it.copy(paso = PasoRecuperacion.CORREO, codigo = "", errores = RestablecerErrores()) }

    fun limpiarMensajeRecuperar() = _recuperar.update { it.copy(mensaje = null) }

    private fun mensajeDe(e: Throwable): String =
        (e as? CasaJojoException)?.mensajeUsuario ?: "Ocurrió un error. Intenta de nuevo."

    // ---------------- SESIÓN ----------------

    /** Borra la sesión en DataStore y luego ejecuta "despues" (volver al login). */
    fun cerrarSesion(despues: () -> Unit) {
        viewModelScope.launch {
            repo.cerrarSesion()
            despues()
        }
    }

    /**
     * Si algo inesperado falla (por ejemplo la base de datos), lo convertimos en
     * un mensaje en vez de cerrar la app. La cancelación se deja pasar.
     */
    private suspend fun ejecutarSeguro(bloque: suspend () -> ResultadoAuth): ResultadoAuth =
        try {
            bloque()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            ResultadoAuth.Error("Ocurrió un error. Intenta de nuevo.")
        }
}