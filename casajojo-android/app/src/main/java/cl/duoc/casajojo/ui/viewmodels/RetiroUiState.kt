package cl.duoc.casajojo.ui.viewmodels

import cl.duoc.casajojo.domain.usecase.RetiroErrores

/**
 * Portado desde origin/android: ficha del formulario de Retiro.
 * Datos y errores viajan juntos, con el mismo patrón que LoginUiState.
 * Antes eran cuatro variables sueltas que la pantalla escribía directo.
 */
data class RetiroUiState(
    val nombre: String = "",
    val horaRetiro: String = "13:00",
    val formaPago: String = "Transferencia",
    val indicaciones: String = "",
    val errores: RetiroErrores = RetiroErrores(),
    /** true después del primer "Confirmar pedido" (validación en tiempo real). */
    val intentado: Boolean = false,
)
