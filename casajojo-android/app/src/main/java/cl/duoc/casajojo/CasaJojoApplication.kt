package cl.duoc.casajojo

import android.app.Application
import cl.duoc.casajojo.data.local.SesionPreferences
import cl.duoc.casajojo.data.remote.RetrofitInstance
import cl.duoc.casajojo.utils.Notificaciones
import kotlinx.coroutines.runBlocking

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

        // A1: el interceptor de OkHttp lee el token JWT guardado en DataStore.
        // Corre en el hilo de red de OkHttp (nunca en el hilo principal), por eso
        // runBlocking aquí no congela la interfaz.
        val sesion = SesionPreferences(this)
        RetrofitInstance.proveedorToken = {
            runBlocking { sesion.token() }.takeIf { it.isNotBlank() }
        }
    }
}
