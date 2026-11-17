package cl.duoc.casajojo.data.remote

import cl.duoc.casajojo.data.remote.dto.LoginRequestDto
import cl.duoc.casajojo.data.remote.dto.PedidoRequestDto
import cl.duoc.casajojo.data.remote.dto.PedidoResponseDto
import cl.duoc.casajojo.data.remote.dto.PlatoMenuDto
import cl.duoc.casajojo.data.remote.dto.RegistroRequestDto
import cl.duoc.casajojo.data.remote.dto.UsuarioResponseDto
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
}
