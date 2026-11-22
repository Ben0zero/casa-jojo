package cl.duoc.casajojo.ui.viewmodels

import cl.duoc.casajojo.data.local.entity.PedidoWithPlatos

/** RF-14 (portado desde origin/android): filtros del Historial. */
enum class FiltroHistorial(val etiqueta: String, val estado: String) {
    Entregados("Entregados", "entregado"),
    Cancelados("Cancelados", "cancelado"),
}

/**
 * Lista del Historial junto con el filtro al que pertenece. Así, al cambiar de
 * chip, la pantalla no muestra por un instante la lista anterior con el título nuevo.
 */
data class HistorialUiState(
    val filtro: FiltroHistorial = FiltroHistorial.Entregados,
    val pedidos: List<PedidoWithPlatos> = emptyList(),
)
