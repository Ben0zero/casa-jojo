package cl.duoc.casajojo.ui.viewmodels

import cl.duoc.casajojo.domain.usecase.RetiroErrores

/**
 * NUEVO (P1): ficha del formulario de Retiro y pago, con el mismo patrón que
 * LoginUiState/RegistroUiState: datos y errores juntos en un solo objeto.
 * Antes eran variables sueltas del ViewModel que la pantalla escribía directo.
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