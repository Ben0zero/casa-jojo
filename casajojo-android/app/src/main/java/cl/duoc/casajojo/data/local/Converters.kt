package cl.duoc.casajojo.data.local

import androidx.room.TypeConverter

class Converters {
    @TypeConverter
    fun listToString(list: List<String>): String = list.joinToString("|")

    @TypeConverter
    fun stringToList(value: String): List<String> =
        if (value.isBlank()) emptyList() else value.split("|")
}