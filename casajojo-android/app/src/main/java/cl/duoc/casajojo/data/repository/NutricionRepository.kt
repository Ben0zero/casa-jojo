package cl.duoc.casajojo.data.repository

import cl.duoc.casajojo.data.remote.OpenFoodFactsApi
import cl.duoc.casajojo.data.remote.RetrofitInstance
import cl.duoc.casajojo.domain.model.NutricionItem
import cl.duoc.casajojo.domain.model.NutricionPlato
import cl.duoc.casajojo.domain.model.PlatoDraft
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

class NutricionRepository(
    private val api: OpenFoodFactsApi = RetrofitInstance.openFoodFactsApi,
) {
    private val codigos = mapOf(
        "Pollo asado" to "8436565361973",
        "Carne al jugo" to "2302800009732",
        "Guatitas" to "8421719100010",
        "Cerdo mongoliano" to "2380820005651",
        "Filete de pollo" to "2302787004348",
        "Tortilla de porotos verdes" to "00646318001322000002",
        "Arroz" to "8480024817587",
        "Arroz integral" to "8413567111041",
        "Verduras salteadas" to "24082921",
        "Fideos blancos" to "8076809583428",
        "Fideos al pesto" to "8422388081655",
        "Papas mayo" to "8437013139731",
        "Puré" to "7501071300592",
        "Ensalada" to "8436585061662",
    )

    private val cache = mutableMapOf<String, NutricionItem>()
    private val candado = Mutex()

    suspend fun nutricionDe(plato: PlatoDraft): Result<NutricionPlato> = withContext(Dispatchers.IO) {
        runCatching {
            val ingredientes = listOf(plato.nombreProteina) +
                if (plato.solo) emptyList() else plato.acompanamientos
            val items = ingredientes.map { ingrediente ->
                val codigo = codigos[ingrediente] ?: error("Sin código nutricional para $ingrediente")
                itemDe(codigo, ingrediente) ?: error("Sin datos nutricionales para $ingrediente")
            }
            NutricionPlato(
                items = items,
                total = NutricionItem(
                    ingrediente = "Total",
                    producto = "",
                    kcal = items.sumOf { it.kcal },
                    proteinas = items.sumOf { it.proteinas },
                    carbohidratos = items.sumOf { it.carbohidratos },
                    grasas = items.sumOf { it.grasas },
                ),
            )
        }
    }

    private suspend fun itemDe(codigo: String, ingrediente: String): NutricionItem? = candado.withLock {
        val guardado = cache[codigo]
        if (guardado != null) {
            guardado.copy(ingrediente = ingrediente)
        } else {
            val encontrado = buscarUno(codigo, ingrediente)
            if (encontrado != null) cache[codigo] = encontrado
            encontrado
        }
    }

    private suspend fun buscarUno(codigo: String, ingrediente: String): NutricionItem? = try {
        var respuesta = api.producto(codigo)
        var intentos = 1
        while (!respuesta.isSuccessful && respuesta.code() in CODIGOS_REINTENTABLES && intentos < 3) {
            delay(500L * intentos)
            respuesta = api.producto(codigo)
            intentos++
        }
        if (!respuesta.isSuccessful) {
            null
        } else {
            val producto = respuesta.body()?.product
            val n = producto?.nutriments
            if (producto == null || n == null || (n.kcal == null && n.proteinas == null &&
                    n.carbohidratos == null && n.grasas == null)
            ) {
                null
            } else {
                NutricionItem(
                    ingrediente = ingrediente,
                    producto = producto.nombre ?: producto.marcas.orEmpty(),
                    kcal = n.kcal ?: 0.0,
                    proteinas = n.proteinas ?: 0.0,
                    carbohidratos = n.carbohidratos ?: 0.0,
                    grasas = n.grasas ?: 0.0,
                )
            }
        }
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        null
    }

    companion object {
        private val CODIGOS_REINTENTABLES = setOf(429, 500, 502, 503, 504)
    }
}
