package cl.duoc.casajojo.data.remote

import cl.duoc.casajojo.BuildConfig
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

/** Cliente Retrofit (guía IL3.1): una instancia por microservicio. */
object RetrofitInstance {

    private val clienteHttp: OkHttpClient by lazy {
        val logging = HttpLoggingInterceptor().apply {
            level = if (BuildConfig.DEBUG) HttpLoggingInterceptor.Level.BODY
            else HttpLoggingInterceptor.Level.NONE
        }
        OkHttpClient.Builder()
            .addInterceptor(logging)
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .writeTimeout(15, TimeUnit.SECONDS)
            .build()
    }

    private fun retrofit(baseUrl: String): Retrofit = Retrofit.Builder()
        .baseUrl(baseUrl)
        .client(clienteHttp)
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    val authApi: AuthApi by lazy { retrofit(ApiConfig.AUTH_URL).create(AuthApi::class.java) }
    val menuApi: MenuApi by lazy { retrofit(ApiConfig.MENU_URL).create(MenuApi::class.java) }
    val pedidoApi: PedidoApi by lazy { retrofit(ApiConfig.PEDIDOS_URL).create(PedidoApi::class.java) }
}
