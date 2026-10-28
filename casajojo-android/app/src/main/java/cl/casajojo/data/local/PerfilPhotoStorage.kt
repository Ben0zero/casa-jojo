package cl.duoc.casajojo.data.local

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileOutputStream

/**
 * Persistencia local de la foto de perfil.
 *
 * La imagen se guarda en filesDir, un directorio privado de la aplicación:
 * otras apps no pueden leerla y Android la conserva aunque la app se cierre.
 *
 * NUEVO (P3): foto en alta resolución. Antes se usaba la miniatura que entrega
 * la cámara (muy pequeña, se veía pixelada). Ahora la cámara escribe la foto
 * completa en cacheDir/fotos/captura.jpg (compartido mediante FileProvider) y
 * aquí se reduce a un tamaño razonable y se endereza según su EXIF.
 */
object PerfilPhotoStorage {
    private const val FILE_NAME = "foto_perfil.jpg"
    private const val CAPTURA = "captura.jpg"

    /** Lado máximo (px) de la foto guardada: nítida en pantalla y liviana en disco. */
    private const val LADO_MAXIMO = 1024

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

    private fun archivoCaptura(context: Context): File =
        File(context.cacheDir, "fotos").apply { mkdirs() }.resolve(CAPTURA)

    /**
     * Uri content:// donde la app de cámara debe escribir la foto.
     * La autoridad coincide con el <provider> del AndroidManifest.
     */
    fun uriCaptura(context: Context): Uri =
        FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", archivoCaptura(context))

    /**
     * Lee la foto que dejó la cámara, la reduce y la gira si hace falta.
     * Es trabajo pesado: llamarla fuera del hilo principal (Dispatchers.IO).
     */
    fun leerCaptura(context: Context): Bitmap? {
        val archivo = archivoCaptura(context)
        if (!archivo.exists() || archivo.length() == 0L) return null
        val ruta = archivo.absolutePath

        // 1) Solo medidas (inJustDecodeBounds), sin cargar la imagen en memoria.
        val medidas = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(ruta, medidas)

        // 2) inSampleSize: carga 1 de cada N píxeles (potencia de 2) para no
        //    traer una foto de 12 MP completa a la memoria.
        var muestra = 1
        while (medidas.outWidth / (muestra * 2) >= LADO_MAXIMO &&
            medidas.outHeight / (muestra * 2) >= LADO_MAXIMO
        ) {
            muestra *= 2
        }
        val bitmap = BitmapFactory.decodeFile(ruta, BitmapFactory.Options().apply { inSampleSize = muestra })
            ?: return null

        // 3) Muchas cámaras guardan la foto "acostada" y anotan el giro en EXIF.
        val grados = when (ExifInterface(ruta).getAttributeInt(
            ExifInterface.TAG_ORIENTATION,
            ExifInterface.ORIENTATION_NORMAL,
        )) {
            ExifInterface.ORIENTATION_ROTATE_90 -> 90f
            ExifInterface.ORIENTATION_ROTATE_180 -> 180f
            ExifInterface.ORIENTATION_ROTATE_270 -> 270f
            else -> 0f
        }
        archivo.delete() // la captura temporal ya no se necesita

        if (grados == 0f) return bitmap
        val giro = Matrix().apply { postRotate(grados) }
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, giro, true)
    }
}
