package cl.duoc.casajojo.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import cl.duoc.casajojo.domain.model.OpcionesAccesibilidad
import cl.duoc.casajojo.domain.model.TemaApp
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import java.io.IOException

/** Archivo DataStore de accesibilidad (una sola instancia por proceso). */
private val Context.accesibilidadDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "accesibilidad"
)

/**
 * Fuente de las opciones de accesibilidad. Es una interfaz para que
 * AccesibilidadViewModel se pruebe con una implementación en memoria.
 */
interface RepositorioAccesibilidad {
    /** Opciones de la cuenta con sesión (o las de fábrica si no hay sesión). */
    val opciones: Flow<OpcionesAccesibilidad>
    suspend fun guardar(opciones: OpcionesAccesibilidad)
}

/**
 * Accesibilidad guardada en DataStore POR USUARIO: las claves llevan el id de la
 * cuenta ("tema_7"), así cada persona que usa el teléfono conserva su configuración.
 * Se combina con la sesión: al cambiar de cuenta, la app cambia de tema/tamaño al instante.
 */
class AccesibilidadPreferences(context: Context) : RepositorioAccesibilidad {

    private val app = context.applicationContext
    private val sesion = SesionPreferences(app)

    private fun claveTema(id: Long) = stringPreferencesKey("tema_$id")
    private fun claveTextoGrande(id: Long) = booleanPreferencesKey("texto_grande_$id")
    private fun claveReducir(id: Long) = booleanPreferencesKey("reducir_animaciones_$id")

    override val opciones: Flow<OpcionesAccesibilidad> = combine(
        sesion.usuarioId,
        app.accesibilidadDataStore.data
            .catch { e -> if (e is IOException) emit(emptyPreferences()) else throw e },
    ) { id, prefs ->
        if (id == null) {
            OpcionesAccesibilidad()
        } else {
            OpcionesAccesibilidad(
                tema = TemaApp.desde(prefs[claveTema(id)]),
                textoGrande = prefs[claveTextoGrande(id)] ?: false,
                reducirAnimaciones = prefs[claveReducir(id)] ?: false,
            )
        }
    }

    override suspend fun guardar(opciones: OpcionesAccesibilidad) {
        val id = sesion.usuarioId.first() ?: return
        app.accesibilidadDataStore.edit {
            it[claveTema(id)] = opciones.tema.name
            it[claveTextoGrande(id)] = opciones.textoGrande
            it[claveReducir(id)] = opciones.reducirAnimaciones
        }
    }
}
