package cl.duoc.casajojo.utils

import android.app.Activity
import android.content.Context
import android.content.Intent

/**
 * Recurso nativo 2: compartir contenido con otras apps del teléfono.
 *
 * Usa el "share sheet" del sistema (Intent.ACTION_SEND), así que sirve para
 * WhatsApp, correo, notas, etc. sin pedir permisos ni saber qué app eligió
 * el usuario.
 */
fun compartirTexto(
    context: Context,
    texto: String,
    asunto: String = "Mi pedido en La Casa JoJo",
) {
    val enviar = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_SUBJECT, asunto)
        putExtra(Intent.EXTRA_TEXT, texto)
    }

    val selector = Intent.createChooser(enviar, "Compartir pedido").apply {
        // Desde un Context que no es Activity (p. ej. un ViewModel) hace falta
        // esta bandera para abrir la hoja de compartir en una tarea nueva.
        if (context !is Activity) addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }

    context.startActivity(selector)
}
