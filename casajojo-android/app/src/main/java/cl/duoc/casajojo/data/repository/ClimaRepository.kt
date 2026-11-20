package cl.duoc.casajojo.data.repository

import cl.duoc.casajojo.data.remote.ClimaApi
import cl.duoc.casajojo.data.remote.RetrofitInstance
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.roundToInt

/**
 * API externa (IL3.1 / IE 3.1.4): Open-Meteo, sin API key y sobre HTTPS.
 * No toca Room, DataStore ni los microservicios de Casa JoJo: solo entrega un
 * texto listo para mostrar en la interfaz. Si falla, devuelve Result.failure y
 * la pantalla sigue funcionando con el menú local.
 */
class ClimaRepository(
    private val api: ClimaApi = RetrofitInstance.climaApi,
) {

    /** Coordenadas de Viña del Mar, Chile. */
    suspend fun climaActual(): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
            val respuesta = api.climaActual(
                lat = -33.0246,
                lon = -71.5518,
                current = "temperature_2m,weather_code,wind_speed_10m",
                timezone = "America/Santiago",
            )
            val actual = respuesta.current ?: error("Open-Meteo sin datos actuales")
            val temperatura = actual.temperatura ?: error("Open-Meteo sin temperatura")
            val descripcion = descripcion(actual.codigo)
            val viento = actual.viento?.roundToInt() ?: 0
            "$descripcion · ${temperatura.roundToInt()}° · viento $viento km/h"
        }
    }

    /** Códigos WMO a texto sencillo para la tarjeta del Home. */
    private fun descripcion(codigo: Int?): String = when (codigo) {
        0 -> "Despejado"
        1, 2 -> "Parcialmente nublado"
        3 -> "Nublado"
        45, 48 -> "Niebla"
        51, 53, 55, 56, 57 -> "Llovizna"
        61, 63, 65, 80, 81, 82 -> "Lluvia"
        71, 73, 75, 77, 85, 86 -> "Nieve"
        95, 96, 99 -> "Tormenta"
        else -> "Variable"
    }
}
