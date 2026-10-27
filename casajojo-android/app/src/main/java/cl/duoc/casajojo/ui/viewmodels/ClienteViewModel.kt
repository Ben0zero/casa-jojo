package cl.duoc.casajojo.ui.viewmodels

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import cl.duoc.casajojo.data.local.CasaJojoDatabase
import cl.duoc.casajojo.utils.Notificaciones
import cl.duoc.casajojo.data.local.SesionPreferences
import cl.duoc.casajojo.data.local.entity.PedidoWithPlatos
import cl.duoc.casajojo.data.local.entity.Producto
import cl.duoc.casajojo.data.repository.PedidoRepository
import cl.duoc.casajojo.domain.model.PlatoDraft
import cl.duoc.casajojo.domain.usecase.ReglasPedido
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * ViewModel único del flujo del cliente. Lo comparten todas las pantallas
 * (se crea una sola vez en App.kt), por eso el carrito "sobrevive" al ir y
 * volver entre Proteína, Acompañamientos, Carrito y Retiro.
 *
 * Tiene dos tipos de estado:
 * 1) Datos que vienen de Room/DataStore como StateFlow (menú, último pedido).
 *    Las pantallas deben leerlos con collectAsState().
 * 2) Estado del formulario en memoria con mutableStateOf (carrito, nombre,
 *    hora, etc.). Compose redibuja automáticamente cuando cambian.
 */
class ClienteViewModel(app: Application) : AndroidViewModel(app) {

    private val db = CasaJojoDatabase.get(app)
    private val repo = PedidoRepository(db)

    // NUEVO (fase 1): sesión guardada en DataStore (id del usuario logueado).
    // CAMBIO (P3): los datos del usuario (nombre para saludar y prellenar) ya no
    // se cargan aquí: los entrega AuthViewModel y App.kt los pasa a cada pantalla.
    private val sesion = SesionPreferences(app)

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

    /**
     * NUEVO: historial de pedidos del usuario logueado (el más reciente primero),
     * para la pantalla "Mis pedidos". Igual que ultimoPedido, sigue a la sesión.
     */
    @OptIn(ExperimentalCoroutinesApi::class)
    val misPedidos: StateFlow<List<PedidoWithPlatos>> = sesion.usuarioId
        .flatMapLatest { id -> if (id == null) flowOf(emptyList()) else repo.pedidosDe(id) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // ---------- 2) Estado del armado del pedido ----------
    // "private set": las pantallas pueden leerlo, pero solo el ViewModel lo modifica.

    /** Platos ya armados y agregados al pedido. */
    var cart by mutableStateOf<List<PlatoDraft>>(emptyList())
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
     * CAMBIO (P1): ficha del formulario de Retiro. Antes eran cuatro variables
     * públicas que la pantalla modificaba directo; ahora solo cambian a través
     * de las funciones onNombre, onHoraRetiro, etc.
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
                precio = ReglasPedido.precioProteina(p.precio, solo = true),
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
        // CORRECCIÓN (P2): si no hay proteína (por ejemplo, Android cerró la app
        // en segundo plano y se perdió la selección), antes no pasaba nada al
        // tocar el botón. Ahora se avisa qué hacer.
        val proteina = proteinaSeleccionada
        if (proteina == null) {
            error = "Primero elige una proteína (toca ‹ Volver)"
            return false
        }
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

    // ---------- PASO 4. Formulario de Retiro ----------
    // Igual que en el registro: antes del primer "Confirmar" escribir no marca
    // errores; después, el nombre se revalida en cada cambio (⚠ o ✓ al instante).

    fun onNombre(v: String) {
        val error = if (retiro.intentado) ReglasPedido.validarRetiro(v).nombre else null
        retiro = retiro.copy(nombre = v, errores = retiro.errores.copy(nombre = error))
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

    /** PASO 3. Quita el plato en la posición indicada. */
    fun quitarPlato(index: Int) {
        if (index in cart.indices) {
            cart = cart.filterIndexed { i, _ -> i != index }
        }
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
        // El carrito no es un campo del formulario: su error va como aviso (Toast).
        val errCarrito = ReglasPedido.validarCarrito(cart)
        if (errCarrito != null) {
            error = errCarrito
            return
        }

        // CAMBIO (P1): los errores del formulario se marcan bajo cada campo.
        // El aviso general ayuda si el campo quedó fuera de la pantalla (scroll).
        val errores = ReglasPedido.validarRetiro(retiro.nombre)
        retiro = retiro.copy(errores = errores, intentado = true)
        if (errores.hayErrores) {
            error = "Revisa los campos marcados"
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
                )
                ultimoNumero = numero

                // RECURSO NATIVO: aviso del sistema. Se usa la hora de "datos"
                // (copia tomada antes de guardar), porque resetear() deja el
                // formulario vacío y la notificación quedaría sin hora.
                Notificaciones.pedidoRegistrado(getApplication(), numero, datos.horaRetiro)

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

    /** NUEVO (fase 1): al cerrar sesión se descarta el pedido a medio armar. */
    fun limpiarTodo() {
        resetear()
        ultimoNumero = null
        error = null
    }

    /** Deja el formulario y el carrito listos para un pedido nuevo. */
    private fun resetear() {
        solo = false
        proteinaSeleccionada = null
        acompanamientosSel = emptyList()
        retiro = RetiroUiState()
        cart = emptyList()
    }
}