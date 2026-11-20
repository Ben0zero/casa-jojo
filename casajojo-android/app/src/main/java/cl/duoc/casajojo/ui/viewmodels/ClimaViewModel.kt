package cl.duoc.casajojo.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cl.duoc.casajojo.data.repository.ClimaRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/** Estado de la API externa: nunca bloquea ni rompe el flujo de pedidos. */
data class ClimaUiState(
    val texto: String? = null,
    val cargando: Boolean = false,
    val fallo: Boolean = false,
)

class ClimaViewModel(
    private val repo: ClimaRepository = ClimaRepository(),
) : ViewModel() {

    private val _estado = MutableStateFlow(ClimaUiState(cargando = true))
    val estado: StateFlow<ClimaUiState> = _estado.asStateFlow()

    init {
        cargar()
    }

    fun cargar() {
        viewModelScope.launch {
            _estado.value = ClimaUiState(cargando = true)
            _estado.value = repo.climaActual().fold(
                onSuccess = { ClimaUiState(texto = it) },
                onFailure = { ClimaUiState(fallo = true) },
            )
        }
    }
}
