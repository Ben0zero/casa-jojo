package cl.duoc.casajojo.data.remote.dto

import com.google.gson.annotations.SerializedName

/** Respuesta de Open-Meteo (API externa, distinta de los microservicios). */
data class ClimaDto(
    @SerializedName("current") val current: ClimaActualDto? = null,
)

data class ClimaActualDto(
    @SerializedName("temperature_2m") val temperatura: Double? = null,
    @SerializedName("weather_code") val codigo: Int? = null,
    @SerializedName("wind_speed_10m") val viento: Double? = null,
)
