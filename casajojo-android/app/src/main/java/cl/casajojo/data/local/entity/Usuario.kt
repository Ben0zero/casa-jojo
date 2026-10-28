package cl.duoc.casajojo.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Cliente registrado en la app (tabla "usuarios").
 *
 * - "correo" tiene un índice ÚNICO: Room/SQLite no deja guardar dos cuentas
 *   con el mismo correo.
 * - La contraseña NUNCA se guarda tal cual (RNF-04). Se guarda:
 *     passwordHash = resultado de cifrar la contraseña con PBKDF2
 *     salt         = valor aleatorio único por usuario que se mezcla en el cifrado
 *   Ver utils/Contrasenas.kt.
 */
@Entity(
    tableName = "usuarios",
    indices = [Index(value = ["correo"], unique = true)],
)
data class Usuario(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val nombre: String,
    val correo: String,
    val telefono: String,
    val passwordHash: String,
    val salt: String,
)