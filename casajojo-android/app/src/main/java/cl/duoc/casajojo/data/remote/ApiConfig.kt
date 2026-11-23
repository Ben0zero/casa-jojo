package cl.duoc.casajojo.data.remote

import cl.duoc.casajojo.BuildConfig

/**
 * URLs de los microservicios adaptados a Casa JoJo.
 *
 * El host se define en app/build.gradle.kts (API_HOST). Por defecto es
 * 10.0.2.2, que es como el emulador Android ve a la PC host. Para probar en un
 * celular físico, cambia API_HOST por la IP de la PC en la misma red Wi-Fi.
 */
object ApiConfig {
    val AUTH_URL = "http://" + BuildConfig.API_HOST + ":8081/"
    val MENU_URL = "http://" + BuildConfig.API_HOST + ":8082/"
    val PEDIDOS_URL = "http://" + BuildConfig.API_HOST + ":8086/"

    /** API externa (Open-Meteo): HTTPS y sin API key. No es un microservicio propio. */
    const val CLIMA_URL = "https://api.open-meteo.com/"

    const val NUTRICION_URL = "https://world.openfoodfacts.org/"
}
