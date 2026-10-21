package cl.duoc.casajojo.ui.cliente

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import cl.duoc.casajojo.data.CasaJojoDatabase
import cl.duoc.casajojo.data.entity.PedidoWithPlatos
import cl.duoc.casajojo.data.entity.Producto
import cl.duoc.casajojo.data.repo.PedidoRepository
import cl.duoc.casajojo.domain.PlatoDraft
import cl.duoc.casajojo.domain.ReglasPedido
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * ViewModel único del flujo del cliente. Lo comparten todas las pantallas
 * (se crea una sola vez en App.kt), por eso el carrito "sobrevive" al ir y
 * volver entre Proteína, Acompañamientos, Carrito y Retiro.
 *
 * Tiene dos tipos de estado:
 * 1) Datos que vienen de Room como StateFlow (menú y último pedido).
 *    Las pantallas deben leerlos con collectAsState().
 * 2) Estado del formulario en memoria con mutableStateOf (carrito, nombre,
 *    hora, etc.). Compose redibuja automáticamente cuando cambian.
 */
class ClienteViewModel(app: Application) : AndroidViewModel(app) {

    private val repo = PedidoRepository(CasaJojoDatabase.get(app))

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

    val ultimoPedido: StateFlow<PedidoWithPlatos?> = repo.ultimoPedido.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), null
    )

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

    // Campos del formulario de Retiro (los escribe directamente la pantalla).
    var nombre by mutableStateOf("")
    var horaRetiro by mutableStateOf("13:00")
    var formaPago by mutableStateOf("Transferencia")
    var indicaciones by mutableStateOf("")

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
    }

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
        // firstNotNullOfOrNull devuelve el primer mensaje de error que encuentre.
        val err = listOf(
            ReglasPedido.validarCarrito(cart),
            ReglasPedido.validarNombre(nombre),
        ).firstNotNullOfOrNull { it }
        if (err != null) {
            error = err
            return
        }

        // viewModelScope.launch inicia una corrutina: el guardado corre sin
        // congelar la pantalla, y se cancela sola si el ViewModel se destruye.
        viewModelScope.launch {
            enRegistro = true
            try {
                val numero = repo.registrarPedido(
                    nombre = nombre.trim(),
                    horaRetiro = horaRetiro,
                    formaPago = formaPago,
                    indicaciones = indicaciones.trim(),
                    platos = cart.toList(),
                )
                ultimoNumero = numero
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

    /** Deja el formulario y el carrito listos para un pedido nuevo. */
    private fun resetear() {
        solo = false
        proteinaSeleccionada = null
        acompanamientosSel = emptyList()
        nombre = ""
        horaRetiro = "13:00"
        formaPago = "Transferencia"
        indicaciones = ""
        cart = emptyList()
    }
}