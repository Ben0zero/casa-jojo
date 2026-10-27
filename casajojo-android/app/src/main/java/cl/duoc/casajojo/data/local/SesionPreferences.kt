package cl.duoc.casajojo.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.io.IOException

/**
 * Archivo "sesion" en el teléfono, creado con DataStore (guía datastore).
 * Va fuera de la clase y pegado a Context: así existe UNA sola instancia
 * del archivo en toda la app, aunque se creen varios SesionPreferences.
 */
private val Context.sesionDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "sesion"
)

/**
 * Guarda quién inició sesión. Sobrevive a cerrar la app: al abrirla de nuevo,
 * si hay un usuario_id guardado, se entra directo sin pedir login.
 *
 * Qué guarda:
 *  - usuario_id    → id del usuario logueado (se borra al cerrar sesión).
 *  - ultimo_correo → último correo usado, para rellenar el login la próxima vez.
 *
 * La contraseña NO se guarda aquí.
 */
class SesionPreferences(private val context: Context) {

    private val claveUsuarioId = longPreferencesKey("usuario_id")
    private val claveUltimoCorreo = stringPreferencesKey("ultimo_correo")

    /**
     * "data" es un Flow que entrega el contenido del archivo cada vez que cambia.
     * catch: si el archivo no se puede leer (IOException), seguimos como si
     * estuviera vacío en vez de cerrar la app.
     */
    private val datos: Flow<Preferences> = context.sesionDataStore.data
        .catch { e -> if (e is IOException) emit(emptyPreferences()) else throw e }

    /** id del usuario con sesión activa, o null si no hay sesión. */
    val usuarioId: Flow<Long?> = datos.map { prefs -> prefs[claveUsuarioId] }

    /** Lee una sola vez el último correo usado (first() espera la primera entrega). */
    suspend fun ultimoCorreo(): String = datos.first()[claveUltimoCorreo] ?: ""

    /** edit es una función suspendida: escribe en segundo plano y deja el archivo consistente. */
    suspend fun iniciar(usuarioId: Long, correo: String) {
        context.sesionDataStore.edit { prefs ->
            prefs[claveUsuarioId] = usuarioId
            prefs[claveUltimoCorreo] = correo
        }
    }

    /** Cierra sesión: borra el id, pero conserva el último correo para el próximo login. */
    suspend fun cerrar() {
        context.sesionDataStore.edit { prefs -> prefs.remove(claveUsuarioId) }
    }
}