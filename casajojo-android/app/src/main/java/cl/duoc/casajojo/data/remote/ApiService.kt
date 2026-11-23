package cl.duoc.casajojo.data.remote

import cl.duoc.casajojo.data.remote.dto.ActualizarPerfilDto
import cl.duoc.casajojo.data.remote.dto.CambiarPasswordDto
import cl.duoc.casajojo.data.remote.dto.RespuestaProductoOff
import cl.duoc.casajojo.data.remote.dto.ClimaDto
import cl.duoc.casajojo.data.remote.dto.LoginRequestDto
import cl.duoc.casajojo.data.remote.dto.MensajeDto
import cl.duoc.casajojo.data.remote.dto.PedidoEdicionDto
import cl.duoc.casajojo.data.remote.dto.RecuperarRequestDto
import cl.duoc.casajojo.data.remote.dto.RestablecerRequestDto
import cl.duoc.casajojo.data.remote.dto.PedidoRequestDto
import cl.duoc.casajojo.data.remote.dto.PedidoResponseDto
import cl.duoc.casajojo.data.remote.dto.PlatoMenuDto
import cl.duoc.casajojo.data.remote.dto.RegistroRequestDto
import cl.duoc.casajojo.data.remote.dto.UsuarioResponseDto
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
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

    /** EFT: envía al correo un código de 6 dígitos (un solo uso, vence en 15 min). */
    @POST("api/auth/recuperar")
    suspend fun recuperar(@Body request: RecuperarRequestDto): MensajeDto

    /** EFT: cambia la contraseña con el código recibido por correo. */
    @POST("api/auth/restablecer")
    suspend fun restablecer(@Body request: RestablecerRequestDto): MensajeDto

    // ---- EFT: CRUD del perfil propio. El correo sale del JWT (AuthInterceptor). ----

    @GET("api/usuarios/me")
    suspend fun perfil(): UsuarioResponseDto

    @PUT("api/usuarios/me")
    suspend fun actualizarPerfil(@Body request: ActualizarPerfilDto): UsuarioResponseDto

    @PUT("api/usuarios/me/password")
    suspend fun cambiarPassword(@Body request: CambiarPasswordDto): MensajeDto

    /** 204 sin cuerpo: por eso se usa Response<Unit>. */
    @DELETE("api/usuarios/me")
    suspend fun eliminarCuenta(): Response<Unit>
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

    /** EFT / RF-12: editar los datos del retiro mientras el pedido siga RECIBIDO. */
    @PUT("api/pedidos/{id}")
    suspend fun editar(
        @Path("id") id: Long,
        @Body request: PedidoEdicionDto,
    ): PedidoResponseDto

    @PUT("api/pedidos/{id}/estado")
    suspend fun cambiarEstado(
        @Path("id") id: Long,
        @Query("estado") estado: String,
    ): PedidoResponseDto

    /**
     * RF-14 / A5: retiro con el QR de la tienda. El servidor valida el código del QR
     * y, si el pedido está LISTO, lo deja pagado y ENTREGADO en un solo paso.
     * (El antiguo PUT /pagar ahora es solo para la caja: rol COCINA o ADMIN.)
     */
    @PUT("api/pedidos/{id}/retirar")
    suspend fun retirar(
        @Path("id") id: Long,
        @Query("codigo") codigo: String,
    ): PedidoResponseDto
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

/**
 * API externa: Open Food Facts (base de datos pública de alimentos). Se consulta
 * por código de barras y solo se piden los campos necesarios. Sin autenticación.
 */
interface OpenFoodFactsApi {
    @GET("api/v3/product/{codigo}")
    suspend fun producto(
        @Path("codigo") codigo: String,
        @Query("fields") campos: String = "product_name,brands,nutriments",
    ): Response<RespuestaProductoOff>
}
