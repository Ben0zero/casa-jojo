package cl.duoc.casajojo.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cl.duoc.casajojo.data.repository.ClimaRepository
import cl.duoc.casajojo.domain.error.CasaJojoException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/** Estado de la API externa: nunca bloquea ni rompe el flujo de pedidos. */
data class ClimaUiState(
    val texto: String? = null,
    val cargando: Boolean = false,
    val fallo: Boolean = false,
    /** EFT: mensaje claro del error (CasaJojoException) para mostrar junto a "Reintentar". */
    val error: String? = null,
)

class ClimaViewModel(
    private val repo: ClimaRepository = ClimaRepository(),
) : ViewModel() {

    private val _estado = MutableStateFlow(ClimaUiState(cargando = true))
    private var trabajo: Job? = null
    val estado: StateFlow<ClimaUiState> = _estado.asStateFlow()

    init {
        cargar()
    }

    /** Carga (o reintenta) el clima. Ignora toques repetidos mientras ya está cargando. */
    fun cargar() {
        if (trabajo?.isActive == true) return
        trabajo = viewModelScope.launch {
            _estado.value = ClimaUiState(cargando = true)
            _estado.value = repo.climaActual().fold(
                onSuccess = { ClimaUiState(texto = it) },
                onFailure = { e ->
                    ClimaUiState(
                        fallo = true,
                        error = (e as? CasaJojoException)?.mensajeUsuario ?: "Sin conexión con el clima",
                    )
                },
            )
        }
    }
}
