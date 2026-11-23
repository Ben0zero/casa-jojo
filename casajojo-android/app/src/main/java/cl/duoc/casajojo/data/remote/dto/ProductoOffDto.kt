package cl.duoc.casajojo.data.remote.dto

import com.google.gson.annotations.SerializedName

data class BusquedaOff(
    val products: List<ProductoOff> = emptyList(),
)

data class ProductoOff(
    @SerializedName("product_name") val nombre: String? = null,
    @SerializedName("brands") val marcas: String? = null,
    val nutriments: NutrimentosOff? = null,
)

data class NutrimentosOff(
    @SerializedName("energy-kcal_100g") val kcal: Double? = null,
    @SerializedName("proteins_100g") val proteinas: Double? = null,
    @SerializedName("carbohydrates_100g") val carbohidratos: Double? = null,
    @SerializedName("fat_100g") val grasas: Double? = null,
)
