package cl.duoc.casajojo

import android.app.Application
import cl.duoc.casajojo.utils.Notificaciones

/**
 * Clase Application del proyecto.
 *
 * Sirve como punto de inicialización global: aquí se crea el canal de
 * notificaciones una sola vez, cuando arranca el proceso, en vez de crearlo
 * cada vez que se muestra una notificación.
 */
class CasaJojoApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        Notificaciones.crearCanal(this)
    }
}
