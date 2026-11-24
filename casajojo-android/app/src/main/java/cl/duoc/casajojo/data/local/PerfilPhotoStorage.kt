package cl.duoc.casajojo.data.local

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

/** Archivo DataStore con la ruta de la foto de perfil (una sola instancia por proceso). */
private val Context.fotoPerfilDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "foto_perfil"
)

/**
 * Persistencia local de la foto de perfil: ÚNICA fuente de verdad del avatar.
 *
 * - La imagen se guarda en filesDir (privado de la app, sobrevive a cerrar la app), una
 *   por cuenta: foto_perfil_<idUsuario>.jpg.
 * - La ruta y una "versión" (marca de tiempo) viven en DataStore y, combinadas con el
 *   usuario con sesión (SesionPreferences), se exponen como
 *   [foto] (StateFlow). Home, Perfil y cualquier avatar observan el mismo flujo, así que
 *   al tomar una foto se actualizan todos sin reiniciar la app.
 * - La cámara escribe la foto a resolución completa en [archivoCaptura] (vía FileProvider);
 *   [guardarCaptura] la endereza según EXIF, la reduce a [LADO_MAXIMO] px y la guarda.
 */
class PerfilPhotoStorage private constructor(context: Context) {

    /** Foto guardada: [version] cambia en cada captura para invalidar la imagen en memoria. */
    data class Foto(val ruta: String, val version: Long)

    private val app = context.applicationContext
    private val sesion = SesionPreferences(app)
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    // Una foto por cuenta: claves y archivo llevan el id del usuario con sesión.
    private fun claveRuta(id: Long) = stringPreferencesKey("ruta_$id")
    private fun claveVersion(id: Long) = longPreferencesKey("version_$id")
    private fun archivoDe(id: Long) = File(app.filesDir, "foto_perfil_$id.jpg")
    private val archivoAntiguo: File get() = File(app.filesDir, FILE_NAME_ANTIGUO)

    /** Foto del usuario con sesión activa, o null si no tiene (o no hay sesión). */
    val foto: StateFlow<Foto?> = combine(
        sesion.usuarioId,
        app.fotoPerfilDataStore.data
            .catch { e -> if (e is IOException) emit(emptyPreferences()) else throw e },
    ) { id, prefs ->
        val ruta = id?.let { prefs[claveRuta(it)] }
        if (id != null && ruta != null && File(ruta).exists()) {
            Foto(ruta, prefs[claveVersion(id)] ?: 0L)
        } else {
            null
        }
    }.stateIn(scope, SharingStarted.Eagerly, null)

    init {
        // Compatibilidad: la versión anterior guardaba UNA foto sin dueño (foto_perfil.jpg).
        // Se asigna a la primera cuenta que abre sesión sin foto propia.
        scope.launch {
            sesion.usuarioId.filterNotNull().collect { id ->
                val antiguo = archivoAntiguo
                if (!antiguo.exists()) return@collect
                val prefs = app.fotoPerfilDataStore.data.first()
                if (prefs[claveRuta(id)] == null && antiguo.renameTo(archivoDe(id))) {
                    app.fotoPerfilDataStore.edit {
                        it[claveRuta(id)] = archivoDe(id).absolutePath
                        it[claveVersion(id)] = System.currentTimeMillis()
                    }
                }
            }
        }
    }

    /** Archivo temporal donde la app de cámara escribe la foto a tamaño completo. */
    fun archivoCaptura(): File =
        File(app.cacheDir, "camara").apply { mkdirs() }.let { File(it, "captura.jpg") }

    /** Procesa la captura recién tomada y la deja como foto de perfil del usuario actual. */
    suspend fun guardarCaptura(): Boolean = withContext(Dispatchers.IO) {
        val origen = archivoCaptura()
        try {
            val id = sesion.usuarioId.first() ?: return@withContext false
            val bitmap = decodificar(origen.absolutePath, LADO_MAXIMO) ?: return@withContext false
            val derecho = enderezar(bitmap, origen.absolutePath)
            val destino = archivoDe(id)
            FileOutputStream(destino).use { salida ->
                derecho.compress(Bitmap.CompressFormat.JPEG, 90, salida)
            }
            app.fotoPerfilDataStore.edit { prefs ->
                prefs[claveRuta(id)] = destino.absolutePath
                prefs[claveVersion(id)] = System.currentTimeMillis()
            }
            origen.delete()
            true
        } catch (_: Exception) {
            false
        }
    }

    /** Borra una foto guardada (por ejemplo, al eliminar la cuenta dueña). */
    suspend fun borrar(foto: Foto) = withContext(Dispatchers.IO) {
        File(foto.ruta).delete()
        app.fotoPerfilDataStore.edit { prefs ->
            prefs.asMap().filter { (clave, valor) -> clave.name.startsWith("ruta_") && valor == foto.ruta }
                .keys.forEach { clave ->
                    val id = clave.name.removePrefix("ruta_")
                    prefs.remove(stringPreferencesKey("ruta_$id"))
                    prefs.remove(longPreferencesKey("version_$id"))
                }
        }
    }

    private fun enderezar(bitmap: Bitmap, ruta: String): Bitmap {
        val grados = when (
            ExifInterface(ruta).getAttributeInt(
                ExifInterface.TAG_ORIENTATION,
                ExifInterface.ORIENTATION_NORMAL,
            )
        ) {
            ExifInterface.ORIENTATION_ROTATE_90 -> 90f
            ExifInterface.ORIENTATION_ROTATE_180 -> 180f
            ExifInterface.ORIENTATION_ROTATE_270 -> 270f
            else -> 0f
        }
        if (grados == 0f) return bitmap
        val matriz = Matrix().apply { postRotate(grados) }
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matriz, true)
    }

    companion object {
        private const val FILE_NAME_ANTIGUO = "foto_perfil.jpg"
        private const val LADO_MAXIMO = 1080

        @Volatile
        private var instancia: PerfilPhotoStorage? = null

        fun get(context: Context): PerfilPhotoStorage =
            instancia ?: synchronized(this) {
                instancia ?: PerfilPhotoStorage(context).also { instancia = it }
            }

        /**
         * Decodifica la imagen sin perder nitidez: el lado menor queda >= [ladoMinimo] px
         * (inSampleSize solo reduce en potencias de 2 mientras siga sobrando resolución).
         */
        fun decodificar(ruta: String, ladoMinimo: Int): Bitmap? {
            val limites = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeFile(ruta, limites)
            if (limites.outWidth <= 0 || limites.outHeight <= 0) return null
            var muestra = 1
            val menor = minOf(limites.outWidth, limites.outHeight)
            while (menor / (muestra * 2) >= ladoMinimo) muestra *= 2
            return BitmapFactory.decodeFile(ruta, BitmapFactory.Options().apply { inSampleSize = muestra })
        }
    }
}
