package cl.duoc.casajojo.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import cl.duoc.casajojo.data.local.Converters

@Entity(
    tableName = "platos",
    foreignKeys = [
        ForeignKey(
            entity = Pedido::class,
            parentColumns = ["id"],
            childColumns = ["pedidoId"],
            onDelete = ForeignKey.CASCADE,
        )
    ],
    indices = [Index("pedidoId")],
)
@TypeConverters(Converters::class)
data class Plato(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val pedidoId: Long,
    val nombreProteina: String,
    val precio: Int,
    val solo: Boolean = false,
    val acompanamientos: List<String> = emptyList(),
)