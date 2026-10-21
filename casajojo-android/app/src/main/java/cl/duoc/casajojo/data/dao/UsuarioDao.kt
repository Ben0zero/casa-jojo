package cl.duoc.casajojo.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import cl.duoc.casajojo.data.entity.Usuario
import kotlinx.coroutines.flow.Flow

/** Consultas de la tabla "usuarios". */
@Dao
interface UsuarioDao {

    /**
     * Inserta un usuario y devuelve su id generado.
     * ABORT: si el correo ya existe (índice único), lanza una excepción
     * en vez de reemplazar la cuenta existente.
     */
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertar(usuario: Usuario): Long

    /** Busca por correo; se usa en el login y para evitar correos repetidos. */
    @Query("SELECT * FROM usuarios WHERE correo = :correo LIMIT 1")
    suspend fun porCorreo(correo: String): Usuario?

    /** Usuario por id como Flow: la UI se actualiza si cambian sus datos. */
    @Query("SELECT * FROM usuarios WHERE id = :id LIMIT 1")
    fun porId(id: Long): Flow<Usuario?>
}