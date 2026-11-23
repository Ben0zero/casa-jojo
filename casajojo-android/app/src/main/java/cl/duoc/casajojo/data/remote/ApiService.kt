package cl.duoc.casajojo.data.remote

import cl.duoc.casajojo.data.remote.dto.RespuestaProductoOff
import cl.duoc.casajojo.data.remote.dto.ClimaDto
import cl.duoc.casajojo.data.remote.dto.LoginRequestDto
import cl.duoc.casajojo.data.remote.dto.PedidoRequestDto
import cl.duoc.casajojo.data.remote.dto.PedidoResponseDto
import cl.duoc.casajojo.data.remote.dto.PlatoMenuDto
import cl.duoc.casajojo.data.remote.dto.RegistroRequestDto
import cl.duoc.casajojo.data.remote.dto.UsuarioResponseDto
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query

/**
 * Contratos REST de la guía IL3.1, pero con la API propia de Casa JoJo.
 * Se separan tres interfaces porque cada microservicio vive en su propio puerto.
 */

interface AuthApi {
    @POST("api/auth/login")
    suspend fun login(@Body request: LoginRequestDto): UsuarioResponseDto

    @POST("api/auth/register")
    suspend fun registrar(@Body request: RegistroRequestDto): UsuarioResponseDto
}

interface MenuApi {
    @GET("api/menu")
    suspend fun menu(): List<PlatoMenuDto>
}

interface PedidoApi {
    @POST("api/pedidos")
    suspend fun crear(@Body request: PedidoRequestDto): PedidoResponseDto

    @GET("api/pedidos/mis-pedidos")
    suspend fun misPedidos(@Query("correo") correo: String): List<PedidoResponseDto>

    @GET("api/pedidos/ultimo")
    suspend fun ultimo(@Query("correo") correo: String): PedidoResponseDto

    @PUT("api/pedidos/{id}/estado")
    suspend fun cambiarEstado(
        @Path("id") id: Long,
        @Query("estado") estado: String,
    ): PedidoResponseDto

    /** RF-14: el QR de retiro marca el pago como pagado en ms-ventas/Oracle. */
    @PUT("api/pedidos/{id}/pagar")
    suspend fun pagar(@Path("id") id: Long): PedidoResponseDto
}

/** API externa: Open-Meteo. Base URL distinta, sin autenticación. */
interface ClimaApi {
    @GET("v1/forecast")
    suspend fun climaActual(
        @Query("latitude") lat: Double,
        @Query("longitude") lon: Double,
        @Query("current") current: String,
        @Query("timezone") timezone: String,
    ): ClimaDto
}

interface OpenFoodFactsApi {
    @GET("api/v3/product/{codigo}")
    suspend fun producto(
        @Path("codigo") codigo: String,
        @Query("fields") campos: String = "product_name,brands,nutriments",
    ): Response<RespuestaProductoOff>
}
