package cl.duoc.casajojo.data.remote.dto

/** Plato del menú devuelto por ms-catalogoProductos (/api/menu). */
data class PlatoMenuDto(
    val id: Long? = null,
    val sku: String? = null,
    val nombre: String? = null,
    val descripcion: String? = null,
    val precio: Int? = null,
    val tipo: String? = null,
    val disponible: Boolean? = null,
)
