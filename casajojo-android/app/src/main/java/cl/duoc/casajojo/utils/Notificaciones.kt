package cl.duoc.casajojo.utils

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import cl.duoc.casajojo.MainActivity
import cl.duoc.casajojo.R

/**
 * Recurso nativo 1: notificaciones locales.
 *
 * Cuando el pedido queda guardado en Room, avisamos al cliente con una
 * notificación del sistema (con el número de pedido y la hora de retiro).
 * Al tocarla se abre la app.
 *
 * El canal se crea cada vez que se notifica: crear un canal ya existente es
 * una operación idempotente (no lanza error ni duplica), así no hace falta
 * una clase Application solo para esto.
 */
object Notificaciones {

    const val CANAL_PEDIDOS = "pedidos"
    private const val ID_PEDIDO_REGISTRADO = 1001

    /** Crea el canal en Android 8.0+ (obligatorio desde API 26). */
    fun crearCanal(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val canal = NotificationChannel(
            CANAL_PEDIDOS,
            "Estado de mi pedido",
            NotificationManager.IMPORTANCE_DEFAULT,
        ).apply {
            description = "Avisos cuando tu pedido queda registrado en La Casa JoJo."
        }
        val manager = context.getSystemService(NotificationManager::class.java)
        manager?.createNotificationChannel(canal)
    }

    /**
     * Muestra "Pedido #12 registrado".
     *
     * Si el usuario no dio el permiso POST_NOTIFICATIONS (Android 13+),
     * NotificationManagerCompat simplemente no muestra nada: no hay crash.
     */
    fun pedidoRegistrado(context: Context, numero: Int, horaRetiro: String) {
        crearCanal(context)

        val abrirApp = PendingIntent.getActivity(
            context,
            numero,
            Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val notificacion = NotificationCompat.Builder(context, CANAL_PEDIDOS)
            .setSmallIcon(R.drawable.ic_notificacion)
            .setContentTitle("Pedido #$numero registrado")
            .setContentText("Te esperamos a las $horaRetiro h para el retiro.")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(abrirApp)
            .setAutoCancel(true)
            .build()

        // En API 33+ el permiso lo pide la UI (ver PedirPermisoNotificaciones).
        if (NotificationManagerCompat.from(context).areNotificationsEnabled()) {
            NotificationManagerCompat.from(context).notify(ID_PEDIDO_REGISTRADO, notificacion)
        }
    }
}
