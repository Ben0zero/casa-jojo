package cl.duoc.casajojo.ui.viewmodels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import cl.duoc.casajojo.data.local.AccesibilidadPreferences
import cl.duoc.casajojo.data.local.RepositorioAccesibilidad
import cl.duoc.casajojo.domain.model.OpcionesAccesibilidad
import cl.duoc.casajojo.domain.model.TemaApp
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * Accesibilidad: tema, tamaño de texto y reducir animaciones.
 *
 * Se crea UNA vez en CasaJojoApp (alcance de la Activity). CasaJojoTheme lee
 * [opciones] y la pantalla Accesibilidad las cambia: como es el mismo StateFlow,
 * el cambio se ve en toda la app al instante, sin reiniciar.
 */
class AccesibilidadViewModel @JvmOverloads constructor(
    app: Application,
    private val repo: RepositorioAccesibilidad = AccesibilidadPreferences(app),
) : AndroidViewModel(app) {

    /** Eagerly: el tema se conoce desde el primer cuadro (no parpadea al abrir). */
    val opciones: StateFlow<OpcionesAccesibilidad> =
        repo.opciones.stateIn(viewModelScope, SharingStarted.Eagerly, OpcionesAccesibilidad())

    fun cambiarTema(tema: TemaApp) = guardar { it.copy(tema = tema) }

    fun cambiarTextoGrande(activo: Boolean) = guardar { it.copy(textoGrande = activo) }

    fun cambiarReducirAnimaciones(activo: Boolean) = guardar { it.copy(reducirAnimaciones = activo) }

    private fun guardar(cambio: (OpcionesAccesibilidad) -> OpcionesAccesibilidad) {
        val nuevas = cambio(opciones.value)
        viewModelScope.launch { repo.guardar(nuevas) }
    }
}
