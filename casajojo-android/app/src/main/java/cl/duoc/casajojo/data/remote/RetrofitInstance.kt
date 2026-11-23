package cl.duoc.casajojo.data.remote

import cl.duoc.casajojo.BuildConfig
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

/** Cliente Retrofit (guía IL3.1): una instancia por microservicio. */
object RetrofitInstance {

    /**
     * A1: de dónde sale el token JWT. CasaJojoApplication lo conecta con DataStore
     * al arrancar; mientras tanto (o en pruebas) no se envía token.
     */
    @Volatile
    var proveedorToken: () -> String? = { null }

    private val clienteHttp: OkHttpClient by lazy {
        val logging = HttpLoggingInterceptor().apply {
            // B2: en debug se registran método, URL, código y cabeceras, pero NO los
            // cuerpos (llevan contraseñas y datos personales).
            level = if (BuildConfig.DEBUG) HttpLoggingInterceptor.Level.HEADERS
            else HttpLoggingInterceptor.Level.NONE
            // El token nunca se escribe en logcat.
            redactHeader("Authorization")
        }
        OkHttpClient.Builder()
            // A1: Authorization: Bearer solo hacia API_HOST (no hacia Open-Meteo).
            .addInterceptor(AuthInterceptor(BuildConfig.API_HOST) { proveedorToken() })
            .addInterceptor(logging)
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .writeTimeout(15, TimeUnit.SECONDS)
            .build()
    }

    /**
     * API externa Open Food Facts: cliente SEPARADO y SIN AuthInterceptor (es una API
     * pública; el JWT nunca debe salir de nuestros microservicios). Open Food Facts pide
     * un User-Agent que identifique a la app, y responde más lento: timeouts mayores.
     */
    private val clienteOff: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .addInterceptor { cadena ->
                cadena.proceed(
                    cadena.request().newBuilder()
                        .header("User-Agent", "CasaJojo/1.0 (contacto@casajojo.cl)")
                        .build()
                )
            }
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()
    }

    private fun retrofit(baseUrl: String, cliente: OkHttpClient = clienteHttp): Retrofit = Retrofit.Builder()
        .baseUrl(baseUrl)
        .client(cliente)
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    val authApi: AuthApi by lazy { retrofit(ApiConfig.AUTH_URL).create(AuthApi::class.java) }
    val menuApi: MenuApi by lazy { retrofit(ApiConfig.MENU_URL).create(MenuApi::class.java) }
    val pedidoApi: PedidoApi by lazy { retrofit(ApiConfig.PEDIDOS_URL).create(PedidoApi::class.java) }

    /** API externa: cliente separado, HTTPS y sin token. */
    val climaApi: ClimaApi by lazy { retrofit(ApiConfig.CLIMA_URL).create(ClimaApi::class.java) }

    /** API externa: información nutricional de cada ingrediente (HTTPS, sin token). */
    val openFoodFactsApi: OpenFoodFactsApi by lazy {
        retrofit(ApiConfig.NUTRICION_URL, clienteOff).create(OpenFoodFactsApi::class.java)
    }
}
