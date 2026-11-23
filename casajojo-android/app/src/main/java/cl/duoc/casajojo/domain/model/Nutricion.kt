package cl.duoc.casajojo.domain.model

data class NutricionItem(
    val ingrediente: String,
    val producto: String,
    val kcal: Double,
    val proteinas: Double,
    val carbohidratos: Double,
    val grasas: Double,
)

data class NutricionPlato(
    val items: List<NutricionItem>,
    val total: NutricionItem,
)
