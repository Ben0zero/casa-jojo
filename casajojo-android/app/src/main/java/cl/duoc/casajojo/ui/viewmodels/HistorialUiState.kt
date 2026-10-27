package cl.duoc.casajojo.ui.viewmodels

import cl.duoc.casajojo.data.local.entity.PedidoWithPlatos

/**
 * NUEVO (RF-14): filtros del Historial. Cada uno sabe qué estado de pedido
 * busca en Room y qué texto muestra su chip.
 */
enum class FiltroHistorial(val etiqueta: String, val estado: String) {
    Entregados("Entregados", "entregado"),
    Cancelados("Cancelados", "cancelado"),
}

/**
 * NUEVO (RF-14): lista del Historial junto con el filtro al que pertenece.
 * Así, al cambiar de chip, la pantalla no muestra por un instante la lista
 * del filtro anterior con el título del nuevo.
 */
data class HistorialUiState(
    val filtro: FiltroHistorial = FiltroHistorial.Entregados,
    val pedidos: List<PedidoWithPlatos> = emptyList(),
)
