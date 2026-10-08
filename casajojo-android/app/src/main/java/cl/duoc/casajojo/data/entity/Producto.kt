package cl.duoc.casajojo.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "productos")
data class Producto(
    @PrimaryKey val id: Long,
    val nombre: String,
    val precio: Int,
    val tipo: String,
    val disponible: Boolean = true,
) {
    companion object {
        const val TIPO_PROTEINA = "proteina"
        const val TIPO_ACOMPANAMIENTO = "acompanamiento"
    }
}