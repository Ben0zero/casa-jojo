package cl.duoc.casajojo.ui.viewmodels

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import cl.duoc.casajojo.data.local.CasaJojoDatabase
import cl.duoc.casajojo.data.local.RoomTransactionRunner
import cl.duoc.casajojo.data.local.SesionPreferences
import cl.duoc.casajojo.data.local.entity.Pedido
import cl.duoc.casajojo.data.local.entity.PedidoWithPlatos
import cl.duoc.casajojo.data.local.entity.Producto
import cl.duoc.casajojo.data.local.entity.Usuario
import cl.duoc.casajojo.data.repository.AuthRepository
import cl.duoc.casajojo.data.repository.NutricionRepository
import cl.duoc.casajojo.data.repository.PedidoRepository
import cl.duoc.casajojo.domain.error.CasaJojoException
import cl.duoc.casajojo.domain.error.ValidacionException
import cl.duoc.casajojo.domain.model.NutricionPlato
import cl.duoc.casajojo.domain.model.PlatoDraft
import cl.duoc.casajojo.domain.usecase.QrRetiro
import cl.duoc.casajojo.domain.usecase.ReglasPedido
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalTime

data class NutricionUiState(
    val expandido: Boolean = false,
    val cargando: Boolean = false,
    val datos: NutricionPlato? = null,
    val error: String? = null,
)

/**
 * ViewModel único del flujo del cliente. Lo comparten todas las pantallas
 * (se crea una sola vez en App.kt), por eso el carrito "sobrevive" al ir y
 * volver entre Proteína, Acompañamientos, Carrito y Retiro.
 *
 * Tiene dos tipos de estado:
 * 1) Datos que vienen de Room/DataStore como StateFlow (menú, usuario, último pedido).
 *    Las pantallas deben leerlos con collectAsState().
 * 2) Estado del formulario en memoria con mutableStateOf (carrito, nombre,
 *    hora, etc.). Compose redibuja automáticamente cuando cambian.
 */
class ClienteViewModel(
    app: Application,
    private val repo: PedidoRepository,
    // NUEVO (fase 1): sesión guardada en DataStore y datos del usuario logueado.
    // EFT: se reciben por constructor para poder probar el ViewModel con MockK.
    private val sesion: SesionPreferences,
    private val authRepo: AuthRepository,
    /** Recurso nativo (notificaciones); en las pruebas se reemplaza por uno vacío. */
    private val avisos: AvisosPedido = AvisosPedido.DelSistema(app),
    /** Hora actual (B7: horas de retiro que ya pasaron); en las pruebas se fija. */
    private val ahora: () -> LocalTime = { LocalTime.now() },
    /** API externa Open Food Facts: nutrición de cada plato del carrito (cliente sin JWT). */
    private val nutricionRepo: NutricionRepository = NutricionRepository(),
) : AndroidViewModel(app) {

    /** Constructor que usa la app (viewModel()): arma Room, DataStore y los repositorios. */
    constructor(app: Application) : this(app, CasaJojoDatabase.get(app), SesionPreferences(app))

    private constructor(app: Application, db: CasaJojoDatabase, sesion: SesionPreferences) : this(
        app = app,
        repo = PedidoRepository(
            productos = db.productoDao(),
            pedidos = db.pedidoDao(),
            transacciones = RoomTransactionRunner(db),
        ),
        sesion = sesion,
        authRepo = AuthRepository(db.usuarioDao(), sesion),
    )

    /** Usuario con sesión activa; se usa para saludar y prellenar el nombre del retiro. */
    val usuario: StateFlow<Usuario?> = authRepo.usuarioActual.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), null
    )

    // ---------- 1) Datos desde Room ----------
    // stateIn convierte el Flow en StateFlow con un valor inicial.
    // WhileSubscribed(5000): consulta Room mientras alguna pantalla lo esté
    // observando, y espera 5 s antes de cortar (útil al girar el teléfono).

    val proteinas: StateFlow<List<Producto>> = repo.proteinas.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList()
    )

    val acompanamientos: StateFlow<List<Producto>> = repo.acompanamientos.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList()
    )

    /**
     * CAMBIO (fase 1): último pedido DEL USUARIO LOGUEADO.
     * flatMapLatest: si cambia el usuario (cerrar sesión y entrar con otro),
     * se deja de escuchar el pedido anterior y se busca el del nuevo usuario.
     */
    @OptIn(ExperimentalCoroutinesApi::class)
    val ultimoPedido: StateFlow<PedidoWithPlatos?> = sesion.usuarioId
        .flatMapLatest { id -> if (id == null) flowOf(null) else repo.ultimoPedidoDe(id) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    // ---------- RF-13 / RF-14: portadas desde origin/android ----------

    /** RF-14: todos los pedidos del usuario (antes solo se veía el último). */
    @OptIn(ExperimentalCoroutinesApi::class)
    val misPedidos: StateFlow<List<PedidoWithPlatos>> = sesion.usuarioId
        .flatMapLatest { id -> if (id == null) flowOf(emptyList()) else repo.pedidosDe(id) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    /** Chip elegido en el Historial ("Entregados" por defecto). */
    private val _filtroHistorial = MutableStateFlow(FiltroHistorial.Entregados)
    val filtroHistorial: StateFlow<FiltroHistorial> = _filtroHistorial

    /**
     * Pedidos finalizados según el filtro. combine + flatMapLatest: si cambia la
     * sesión o el chip, se deja la consulta anterior y se escucha la nueva.
     */
    @OptIn(ExperimentalCoroutinesApi::class)
    val historial: StateFlow<HistorialUiState> =
        combine(sesion.usuarioId, _filtroHistorial) { id, filtro -> id to filtro }
            .flatMapLatest { (id, filtro) ->
                if (id == null) flowOf(HistorialUiState(filtro))
                else repo.historialDe(id, filtro.estado).map { HistorialUiState(filtro, it) }
            }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), HistorialUiState())

    fun onFiltroHistorial(filtro: FiltroHistorial) {
        _filtroHistorial.value = filtro
    }

    // ---------- 2) Estado del armado del pedido ----------
    // "private set": las pantallas pueden leerlo, pero solo el ViewModel lo modifica.

    /** Platos ya armados y agregados al pedido. */
    var cart by mutableStateOf<List<PlatoDraft>>(emptyList())
        private set

    var nutricionPorPlato by mutableStateOf<Map<PlatoDraft, NutricionUiState>>(emptyMap())
        private set

    /** true si el cliente marcó "Solo la proteína, sin acompañamientos". */
    var solo by mutableStateOf(false)
        private set

    /** Proteína elegida en el paso 1, a la espera de sus acompañamientos. */
    var proteinaSeleccionada by mutableStateOf<Producto?>(null)
        private set

    var acompanamientosSel by mutableStateOf<List<String>>(emptyList())
        private set

    /**
     * Ficha del formulario de Retiro (portada desde origin/android):
     * datos y errores por campo en un solo objeto. La pantalla ya no escribe
     * directo las variables; usa onNombre, onHoraRetiro, etc.
     */
    var retiro by mutableStateOf(RetiroUiState())
        private set

    /** Mensaje de error a mostrar (lo muestra ErrorEcho como Toast). */
    var error by mutableStateOf<String?>(null)
        private set

    /** Número del último pedido confirmado, para la pantalla Confirmado. */
    var ultimoNumero by mutableStateOf<Int?>(null)
        private set

    /** true mientras se guarda en Room; sirve para desactivar el botón y evitar doble envío. */
    var enRegistro by mutableStateOf(false)
        private set

    val total: Int get() = ReglasPedido.total(cart)

    init {
        resetear()
        // EA3: al abrir la app se actualiza el menú y, A4 / M11, se reenvían los
        // pedidos que quedaron sin enviar y se sincroniza el estado de TODOS los
        // pedidos del usuario. Si no hay red, Room mantiene la última copia.
        viewModelScope.launch {
            repo.sincronizarMenu()
            sincronizarPedidos()
        }
    }

    /** Vuelve a traer el menú y sincronizar los pedidos (p. ej. al conceder el permiso de red local). */
    fun sincronizarConServidor() {
        viewModelScope.launch {
            repo.sincronizarMenu()
            sincronizarPedidos()
        }
    }

    /** Llamado desde "Mi pedido" para reenviar pendientes y refrescar el estado remoto. */
    fun sincronizarUltimoPedido() {
        viewModelScope.launch { sincronizarPedidos() }
    }

    private suspend fun sincronizarPedidos() {
        try {
            repo.sincronizarEstados(sesion.usuarioId.first(), sesion.ultimoCorreo())
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            // La sincronización es de mejor esfuerzo: la pantalla sigue con la caché.
        }
    }

    // M10: se eliminó "marcarListoParaRetiro". Marcar un pedido como LISTO le
    // corresponde a la cocina, no al cliente. La app solo sincroniza el estado
    // (sincronizarUltimoPedido) y cancela o retira con QR.

    fun limpiarError() {
        error = null
    }

    fun toggleSolo() {
        solo = !solo
    }

    /**
     * PASO 1. Al tocar una proteína:
     * - si está marcado "solo proteína", el plato se agrega directo al carrito a $3.000;
     * - si no, se guarda como seleccionada y la pantalla navega a Acompañamientos.
     */
    fun elegirProteina(p: Producto) {
        if (!p.disponible) return // las agotadas no se pueden pedir
        if (solo) {
            cart = cart + PlatoDraft(
                nombreProteina = p.nombre,
                precio = ReglasPedido.PRECIO_SOLO_PROTEINA,
                solo = true,
            )
            solo = false
        } else {
            proteinaSeleccionada = p
            acompanamientosSel = emptyList()
        }
    }

    /** PASO 2. Marca o desmarca un acompañamiento. */
    fun toggleAcompanamiento(nombre: String) {
        acompanamientosSel =
            if (acompanamientosSel.contains(nombre)) {
                acompanamientosSel - nombre
            } else {
                acompanamientosSel + nombre
            }
    }

    /**
     * PASO 2 → 3. Valida y agrega el plato armado al carrito.
     * Devuelve true si se agregó (App.kt solo navega al carrito en ese caso).
     */
    fun agregarPlato(): Boolean {
        val proteina = proteinaSeleccionada ?: return false
        val err = ReglasPedido.validarAcompanamientos(false, acompanamientosSel)
        if (err != null) {
            error = err
            return false
        }
        cart = cart + PlatoDraft(
            nombreProteina = proteina.nombre,
            precio = proteina.precio,
            solo = false,
            acompanamientos = acompanamientosSel.sorted(),
        )
        proteinaSeleccionada = null
        acompanamientosSel = emptyList()
        return true
    }

    /** PASO 3. Quita el plato en la posición indicada. */
    fun quitarPlato(index: Int) {
        if (index in cart.indices) {
            val plato = cart[index]
            cart = cart.filterIndexed { i, _ -> i != index }
            nutricionPorPlato = nutricionPorPlato - plato
        }
    }

    fun mostrarNutricion(plato: PlatoDraft) {
        val actual = nutricionPorPlato[plato] ?: NutricionUiState()
        nutricionPorPlato = nutricionPorPlato + (plato to actual.copy(expandido = true))
        if (actual.datos == null && !actual.cargando) consultarNutricion(plato)
    }

    fun ocultarNutricion(plato: PlatoDraft) {
        val actual = nutricionPorPlato[plato] ?: return
        nutricionPorPlato = nutricionPorPlato + (plato to actual.copy(expandido = false))
    }

    fun reintentarNutricion(plato: PlatoDraft) {
        consultarNutricion(plato, forzar = true)
    }

    private fun consultarNutricion(plato: PlatoDraft, forzar: Boolean = false) {
        val actual = nutricionPorPlato[plato] ?: NutricionUiState()
        if (!forzar && (actual.datos != null || actual.cargando)) return
        nutricionPorPlato = nutricionPorPlato + (
            plato to actual.copy(expandido = true, cargando = true, error = null)
        )
        viewModelScope.launch {
            val resultado = try {
                nutricionRepo.nutricionDe(plato)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Result.failure(e)
            }
            val estado = nutricionPorPlato[plato] ?: NutricionUiState()
            nutricionPorPlato = nutricionPorPlato + (plato to resultado.fold(
                onSuccess = { estado.copy(cargando = false, datos = it, error = null) },
                onFailure = {
                    estado.copy(
                        cargando = false,
                        error = "No se pudo consultar la nutrición",
                    )
                },
            ))
        }
    }

    // ---------- PASO 4. Formulario de Retiro (validación por campo) ----------

    fun onNombre(v: String) {
        val err = if (retiro.intentado) ReglasPedido.validarRetiro(v).nombre else null
        retiro = retiro.copy(nombre = v, errores = retiro.errores.copy(nombre = err))
    }

    fun onHoraRetiro(v: String) {
        retiro = retiro.copy(horaRetiro = v)
    }

    fun onFormaPago(v: String) {
        retiro = retiro.copy(formaPago = v)
    }

    fun onIndicaciones(v: String) {
        retiro = retiro.copy(indicaciones = v)
    }

    /**
     * PASO 4 → 5. Valida el formulario, guarda el pedido en Room y,
     * si todo sale bien, ejecuta "despues" (App.kt navega a Confirmado).
     */
    fun confirmarPedido(despues: () -> Unit) {
        // Evita un segundo toque mientras se está guardando.
        if (enRegistro) return

        // CORRECCIÓN: antes solo se validaba el nombre y se podía guardar un
        // pedido sin platos ($0). Ahora se valida también el carrito.
        // firstNotNullOfOrNull devuelve el primer mensaje de error que encuentre.
        val errCarrito = ReglasPedido.validarCarrito(cart)
        if (errCarrito != null) {
            error = errCarrito
            return
        }

        // Portado desde android: errores bajo cada campo + aviso general.
        val errores = ReglasPedido.validarRetiro(retiro.nombre)
        retiro = retiro.copy(errores = errores, intentado = true)
        if (errores.hayErrores) {
            error = "Revisa los campos marcados"
            return
        }
        // B7: no se aceptan horas de retiro que ya pasaron.
        ReglasPedido.validarHoraRetiro(retiro.horaRetiro, ahora())?.let {
            error = it
            return
        }
        val datos = retiro

        // viewModelScope.launch inicia una corrutina: el guardado corre sin
        // congelar la pantalla, y se cancela sola si el ViewModel se destruye.
        viewModelScope.launch {
            enRegistro = true
            try {
                val numero = repo.registrarPedido(
                    // NUEVO (fase 1): el pedido queda asociado a la cuenta.
                    // first() lee una vez el id guardado en DataStore.
                    usuarioId = sesion.usuarioId.first(),
                    nombre = datos.nombre.trim(),
                    horaRetiro = datos.horaRetiro,
                    formaPago = datos.formaPago,
                    indicaciones = datos.indicaciones.trim(),
                    platos = cart.toList(),
                    correo = sesion.ultimoCorreo(),
                    origen = PedidoRepository.ORIGEN_APP, // M16: antes "Mostrador"
                )
                ultimoNumero = numero

                // RECURSO NATIVO: aviso del sistema. Se toma la hora ANTES de
                // resetear(), porque resetear() deja el formulario vacío y la
                // notificación quedaría sin hora.
                avisos.pedidoRegistrado(numero, datos.horaRetiro)

                resetear()
                despues()
            } catch (e: CancellationException) {
                // Si la corrutina se cancela (el ViewModel se destruye), no es un
                // error del guardado: se relanza para que la cancelación siga su curso.
                throw e
            } catch (e: Exception) {
                // CORRECCIÓN: si Room falla, avisamos y NO borramos el carrito,
                // para que el cliente pueda reintentar sin volver a armar todo.
                error = "No pudimos registrar tu pedido. Intenta de nuevo."
            } finally {
                // finally se ejecuta siempre: el botón nunca queda "Registrando…" pegado.
                enRegistro = false
            }
        }
    }

    /**
     * NUEVO (fase 1): si el campo nombre del retiro está vacío, lo rellena con
     * el nombre de la cuenta. Si la persona ya escribió algo, no lo pisa.
     */
    fun prellenarNombre(nombreUsuario: String?) {
        if (retiro.nombre.isBlank() && !nombreUsuario.isNullOrBlank()) {
            onNombre(nombreUsuario)
        }
    }

    /** RF-13: true mientras se cancela (evita doble toque). */
    var cancelando by mutableStateOf(false)
        private set

    /**
     * RF-13: cancela en ms-ventas/Oracle y refleja el cambio en Room. El plazo
     * se revisa en la UI, en el repositorio y en el UPDATE de Room, así que
     * alTerminar recibe false si ya no se podía.
     */
    fun cancelarPedido(pedido: Pedido, alTerminar: (Boolean) -> Unit) {
        if (cancelando) return
        viewModelScope.launch {
            cancelando = true
            try {
                val resultado = repo.cancelarPedidoRemoto(sesion.ultimoCorreo(), pedido)
                val cancelado = resultado.isSuccess
                if (cancelado) {
                    avisos.quitarPedidoRegistrado()
                } else {
                    error = resultado.exceptionOrNull()?.message
                        ?: "No pudimos cancelar tu pedido. Intenta de nuevo."
                }
                alTerminar(cancelado)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                error = "No pudimos cancelar tu pedido. Intenta de nuevo."
            } finally {
                cancelando = false
            }
        }
    }

    /**
     * RF-14: al leer bien el QR de retiro, el pedido queda "pagado" y
     * "entregado" en Oracle y en Room (así aparece en el Historial). Un pedido
     * cancelado no cambia: lo impiden la regla y la consulta del DAO.
     */
    fun registrarRetiro(pedido: Pedido, contenidoQr: String?) {
        if (!ReglasPedido.puedeRetirarConQr(pedido.estadoPedido)) {
            error = "Tu pedido aún no está listo para retirar"
            return
        }
        // A5: solo se acepta el QR propio de la tienda (casajojo://retiro?codigo=...).
        // Ya no se abre ninguna URL escaneada.
        val codigo = QrRetiro.codigo(contenidoQr)
        if (codigo == null) {
            error = "Este QR no es de La Casa JoJo"
            return
        }
        viewModelScope.launch {
            try {
                val resultado = repo.marcarEntregadoRemoto(sesion.ultimoCorreo(), pedido, codigo)
                if (resultado.isFailure) {
                    error = resultado.exceptionOrNull()?.message
                        ?: "No pudimos registrar el retiro. Intenta de nuevo."
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                error = "No pudimos registrar el retiro. Intenta de nuevo."
            }
        }
    }

    // ---------- EFT / RF-12: editar el pedido mientras está RECIBIDO ----------

    /** Ficha del diálogo "Editar pedido"; pedido == null significa diálogo cerrado. */
    var edicion by mutableStateOf(EdicionPedidoUiState())
        private set

    /** Abre el diálogo con los datos actuales del pedido (solo si sigue "recibido"). */
    fun abrirEdicion(pedido: Pedido) {
        if (!ReglasPedido.puedeEditar(pedido.estadoPedido)) {
            error = PedidoRepository.MSG_NO_EDITABLE
            return
        }
        edicion = EdicionPedidoUiState(
            pedido = pedido,
            nombre = pedido.nombre,
            horaRetiro = pedido.horaRetiro,
            formaPago = pedido.formaPago,
            indicaciones = pedido.indicaciones,
        )
    }

    fun cerrarEdicion() {
        if (!edicion.guardando) edicion = EdicionPedidoUiState()
    }

    fun onEdicionNombre(v: String) {
        edicion = edicion.copy(nombre = v, errorNombre = null)
    }

    fun onEdicionHora(v: String) {
        edicion = edicion.copy(horaRetiro = v)
    }

    fun onEdicionFormaPago(v: String) {
        edicion = edicion.copy(formaPago = v)
    }

    fun onEdicionIndicaciones(v: String) {
        edicion = edicion.copy(indicaciones = v)
    }

    /**
     * Valida (domain) y guarda en ms-ventas + Room. alTerminar recibe el mensaje para
     * el Snackbar. Si la cocina ya empezó (409), el diálogo se cierra con el aviso.
     */
    fun guardarEdicion(alTerminar: (String) -> Unit) {
        val datos = edicion
        val pedido = datos.pedido ?: return
        if (datos.guardando) return
        val errNombre = ReglasPedido.validarNombre(datos.nombre)
        if (errNombre != null) {
            edicion = datos.copy(errorNombre = errNombre)
            return
        }
        viewModelScope.launch {
            edicion = edicion.copy(guardando = true)
            try {
                val resultado = repo.editarPedido(
                    correo = sesion.ultimoCorreo(),
                    pedido = pedido,
                    nombre = datos.nombre,
                    horaRetiro = datos.horaRetiro,
                    formaPago = datos.formaPago,
                    indicaciones = datos.indicaciones,
                )
                resultado.fold(
                    onSuccess = {
                        edicion = EdicionPedidoUiState()
                        alTerminar("Pedido #${pedido.numero} actualizado")
                    },
                    onFailure = { e ->
                        val mensaje = (e as? CasaJojoException)?.mensajeUsuario
                            ?: "No pudimos editar tu pedido. Intenta de nuevo."
                        edicion = if ((e as? ValidacionException)?.campo == "nombre") {
                            edicion.copy(guardando = false, errorNombre = mensaje)
                        } else {
                            EdicionPedidoUiState()
                        }
                        alTerminar(mensaje)
                    },
                )
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                edicion = EdicionPedidoUiState()
                alTerminar("No pudimos editar tu pedido. Intenta de nuevo.")
            }
        }
    }

    /** NUEVO (fase 1): al cerrar sesión se descarta el pedido a medio armar. */
    fun limpiarTodo() {
        resetear()
        ultimoNumero = null
        error = null
        edicion = EdicionPedidoUiState()
    }

    /** Deja el formulario y el carrito listos para un pedido nuevo. */
    private fun resetear() {
        solo = false
        proteinaSeleccionada = null
        acompanamientosSel = emptyList()
        retiro = RetiroUiState()
        cart = emptyList()
        nutricionPorPlato = emptyMap()
    }
}