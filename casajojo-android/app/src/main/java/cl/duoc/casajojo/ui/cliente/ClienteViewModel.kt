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
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class ClienteViewModel(app: Application) : AndroidViewModel(app) {

    private val repo = PedidoRepository(CasaJojoDatabase.get(app))

    val proteinas: StateFlow<List<Producto>> = repo.proteinas.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList()
    )

    val acompanamientos: StateFlow<List<Producto>> = repo.acompanamientos.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList()
    )

    val ultimoPedido: StateFlow<PedidoWithPlatos?> = repo.ultimoPedido.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), null
    )

    var cart by mutableStateOf<List<PlatoDraft>>(emptyList())
        private set

    var solo by mutableStateOf(false)
        private set

    var proteinaSeleccionada by mutableStateOf<Producto?>(null)
        private set

    var acompanamientosSel by mutableStateOf<List<String>>(emptyList())
        private set

    var nombre by mutableStateOf("")

    var horaRetiro by mutableStateOf("13:00")

    var formaPago by mutableStateOf("Transferencia")

    var indicaciones by mutableStateOf("")

    var error by mutableStateOf<String?>(null)
        private set

    var ultimoNumero by mutableStateOf<Int?>(null)
        private set

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

    fun elegirProteina(p: Producto) {
        if (!p.disponible) return
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

    fun toggleAcompanamiento(nombre: String) {
        acompanamientosSel =
            if (acompanamientosSel.contains(nombre)) {
                acompanamientosSel - nombre
            } else {
                acompanamientosSel + nombre
            }
    }

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

    fun quitarPlato(index: Int) {
        if (index in cart.indices) {
            cart = cart.filterIndexed { i, _ -> i != index }
        }
    }

    fun confirmarPedido(despues: () -> Unit) {
        if (nombre.isBlank()) {
            error = "Escribe tu nombre para el retiro"
            return
        }
        viewModelScope.launch {
            enRegistro = true
            val copia = cart.toList()
            val numero = repo.registrarPedido(
                nombre = nombre.trim(),
                horaRetiro = horaRetiro,
                formaPago = formaPago,
                indicaciones = indicaciones.trim(),
                platos = copia,
            )
            ultimoNumero = numero
            resetear()
            enRegistro = false
            despues()
        }
    }

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