package cl.duoc.casajojo.ui.viewmodels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import cl.duoc.casajojo.data.local.CasaJojoDatabase
import cl.duoc.casajojo.data.local.RoomTransactionRunner
import cl.duoc.casajojo.data.local.SesionPreferences
import cl.duoc.casajojo.data.local.entity.Tarjeta
import cl.duoc.casajojo.data.repository.TarjetaInvalidaException
import cl.duoc.casajojo.data.repository.TarjetaRepository
import cl.duoc.casajojo.domain.usecase.MarcaTarjeta
import cl.duoc.casajojo.domain.usecase.TarjetaErrores
import cl.duoc.casajojo.domain.usecase.ValidacionesTarjeta
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.YearMonth

/**
 * Ficha del formulario "Agregar tarjeta". El número y el CVV viven SOLO aquí, en
 * memoria, mientras la persona escribe; al guardar o cerrar el formulario se borran.
 */
data class FormTarjetaUiState(
    val numero: String = "",
    val vencimiento: String = "",
    val cvv: String = "",
    val titular: String = "",
    val alias: String = "",
    val errores: TarjetaErrores = TarjetaErrores(),
    /** true después del primer "Guardar tarjeta": desde ahí cada campo se revalida al escribir. */
    val intentado: Boolean = false,
    val guardando: Boolean = false,
) {
    /** Marca detectada en vivo para mostrarla mientras se escribe. */
    val marca: MarcaTarjeta get() = ValidacionesTarjeta.detectarMarca(numero)
}

/** Estado de la pantalla Billetera (además de la lista de tarjetas). */
data class BilleteraUiState(
    val formulario: FormTarjetaUiState = FormTarjetaUiState(),
    val mostrandoFormulario: Boolean = false,
    /** Tarjeta que espera confirmación para eliminarse (diálogo abierto). */
    val porEliminar: Tarjeta? = null,
    val mensaje: String? = null,
)

/**
 * Billetera (SIMULADA): agregar, eliminar y elegir la tarjeta predeterminada.
 * Las tarjetas vienen de Room por usuario; las reglas, de ValidacionesTarjeta.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class BilleteraViewModel @JvmOverloads constructor(
    app: Application,
    private val repo: TarjetaRepository = CasaJojoDatabase.get(app).let { db ->
        TarjetaRepository(db.tarjetaDao(), RoomTransactionRunner(db))
    },
    private val usuarioId: Flow<Long?> = SesionPreferences(app).usuarioId,
    private val hoy: () -> YearMonth = { YearMonth.now() },
) : AndroidViewModel(app) {

    /** Tarjetas de la cuenta con sesión; si cambia la cuenta, cambia la lista. */
    val tarjetas: StateFlow<List<Tarjeta>> = usuarioId
        .flatMapLatest { id -> if (id == null) flowOf(emptyList()) else repo.tarjetasDe(id) }
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    private val _estado = MutableStateFlow(BilleteraUiState())
    val estado: StateFlow<BilleteraUiState> = _estado.asStateFlow()

    // ---------- Formulario ----------

    fun abrirFormulario() = _estado.update {
        it.copy(mostrandoFormulario = true, formulario = FormTarjetaUiState())
    }

    /** Cierra y BORRA lo escrito (número y CVV no quedan en memoria). */
    fun cerrarFormulario() = _estado.update {
        it.copy(mostrandoFormulario = false, formulario = FormTarjetaUiState())
    }

    /**
     * Se guardan SOLO los dígitos (máx. 19). El formato "4242 4242 …" lo pone la pantalla
     * con una VisualTransformation: así el cursor no salta al insertar los espacios.
     */
    fun onNumero(v: String) = editar {
        it.copy(numero = ValidacionesTarjeta.soloDigitos(v).take(ValidacionesTarjeta.MAX_DIGITOS))
    }

    /** Solo dígitos "MMAA"; la pantalla muestra "MM/AA". */
    fun onVencimiento(v: String) = editar { it.copy(vencimiento = ValidacionesTarjeta.soloDigitos(v).take(4)) }

    fun onCvv(v: String) = editar { it.copy(cvv = ValidacionesTarjeta.soloDigitos(v).take(4)) }

    fun onTitular(v: String) = editar { it.copy(titular = v) }

    fun onAlias(v: String) = editar { it.copy(alias = v.take(20)) }

    /** Aplica el cambio y, si ya se intentó guardar, revalida en vivo. */
    private fun editar(cambio: (FormTarjetaUiState) -> FormTarjetaUiState) = _estado.update { e ->
        val nuevo = cambio(e.formulario)
        e.copy(formulario = if (nuevo.intentado) nuevo.copy(errores = validar(nuevo)) else nuevo)
    }

    private fun validar(f: FormTarjetaUiState): TarjetaErrores =
        ValidacionesTarjeta.validar(f.numero, f.vencimiento, f.cvv, f.titular, hoy())

    fun agregar() {
        val f = _estado.value.formulario
        if (f.guardando) return
        val errores = validar(f)
        if (errores.hayErrores) {
            _estado.update {
                it.copy(formulario = f.copy(errores = errores, intentado = true), mensaje = "Revisa los campos marcados")
            }
            return
        }
        _estado.update { it.copy(formulario = f.copy(guardando = true, errores = errores, intentado = true)) }
        viewModelScope.launch {
            try {
                val id = usuarioId.first()
                if (id == null) {
                    _estado.update { it.copy(formulario = it.formulario.copy(guardando = false), mensaje = "Inicia sesión para guardar tarjetas") }
                    return@launch
                }
                val r = repo.agregar(id, f.numero, f.vencimiento, f.cvv, f.titular, f.alias)
                r.onSuccess { t ->
                    // El formulario se vacía: el número completo y el CVV se descartan aquí.
                    _estado.update {
                        it.copy(mostrandoFormulario = false, formulario = FormTarjetaUiState(), mensaje = "Tarjeta ${t.etiqueta} agregada")
                    }
                }.onFailure { e ->
                    val err = (e as? TarjetaInvalidaException)?.errores ?: _estado.value.formulario.errores
                    _estado.update {
                        it.copy(formulario = it.formulario.copy(guardando = false, errores = err), mensaje = e.message)
                    }
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _estado.update {
                    it.copy(formulario = it.formulario.copy(guardando = false), mensaje = "No pudimos guardar la tarjeta. Intenta de nuevo.")
                }
            }
        }
    }

    // ---------- Lista ----------

    fun marcarPredeterminada(t: Tarjeta) {
        if (t.predeterminada) return
        viewModelScope.launch {
            repo.marcarPredeterminada(t.usuarioId, t.id)
            _estado.update { it.copy(mensaje = "${t.etiqueta} es ahora tu tarjeta predeterminada") }
        }
    }

    fun pedirEliminar(t: Tarjeta) = _estado.update { it.copy(porEliminar = t) }

    fun cancelarEliminar() = _estado.update { it.copy(porEliminar = null) }

    fun confirmarEliminar() {
        val t = _estado.value.porEliminar ?: return
        _estado.update { it.copy(porEliminar = null) }
        viewModelScope.launch {
            val ok = repo.eliminar(t.usuarioId, t)
            _estado.update { it.copy(mensaje = if (ok) "Tarjeta ${t.etiqueta} eliminada" else "No se encontró la tarjeta") }
        }
    }

    fun limpiarMensaje() = _estado.update { it.copy(mensaje = null) }
}
