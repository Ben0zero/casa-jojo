package cl.duoc.casajojo.ui.viewmodels

import android.content.Context
import cl.duoc.casajojo.data.local.entity.Pedido
import cl.duoc.casajojo.utils.Notificaciones

/**
 * EFT / RF-12: ficha del diálogo "Editar pedido". Mismo patrón que RetiroUiState:
 * datos y error por campo juntos. [pedido] == null → el diálogo está cerrado.
 */
data class EdicionPedidoUiState(
    val pedido: Pedido? = null,
    val nombre: String = "",
    val horaRetiro: String = "",
    val formaPago: String = "",
    val indicaciones: String = "",
    val errorNombre: String? = null,
    val guardando: Boolean = false,
) {
    val abierta: Boolean get() = pedido != null
}

/**
 * Avisos del sistema sobre el pedido (recurso nativo: notificaciones).
 * Es una interfaz para que el ViewModel no dependa de Android en las pruebas
 * unitarias (polimorfismo: en la app se usa [DelSistema], en los tests un mock).
 */
interface AvisosPedido {
    fun pedidoRegistrado(numero: Int, horaRetiro: String)
    fun quitarPedidoRegistrado()

    /** Implementación real: notificaciones de Android (utils/Notificaciones). */
    class DelSistema(private val context: Context) : AvisosPedido {
        override fun pedidoRegistrado(numero: Int, horaRetiro: String) =
            Notificaciones.pedidoRegistrado(context, numero, horaRetiro)

        override fun quitarPedidoRegistrado() = Notificaciones.quitarPedidoRegistrado(context)
    }
}
