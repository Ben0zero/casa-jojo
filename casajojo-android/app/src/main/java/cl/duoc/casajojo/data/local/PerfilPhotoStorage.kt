package cl.duoc.casajojo.data.local

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import java.io.File
import java.io.FileOutputStream

/**
 * Persistencia local de la foto de perfil.
 *
 * La imagen se guarda en filesDir, un directorio privado de la aplicación:
 * otras apps no pueden leerla y Android la conserva aunque la app se cierre.
 */
object PerfilPhotoStorage {
    private const val FILE_NAME = "foto_perfil.jpg"

    fun cargar(context: Context): Bitmap? {
        val archivo = File(context.filesDir, FILE_NAME)
        return if (archivo.exists()) BitmapFactory.decodeFile(archivo.absolutePath) else null
    }

    fun guardar(context: Context, bitmap: Bitmap): Boolean {
        val archivo = File(context.filesDir, FILE_NAME)
        return try {
            FileOutputStream(archivo).use { salida ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, 90, salida)
            }
        } catch (_: Exception) {
            false
        }
    }
}
