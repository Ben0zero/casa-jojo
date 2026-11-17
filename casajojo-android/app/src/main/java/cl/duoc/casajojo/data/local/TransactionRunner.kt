package cl.duoc.casajojo.data.local

import androidx.room.withTransaction

/**
 * Abstracción de la transacción de Room. El repositorio la recibe por
 * constructor: en producción usa Room y en las pruebas unitarias una
 * implementación que solo ejecuta el bloque (así se puede testear sin Android).
 */
interface TransactionRunner {
    suspend fun <R> ejecutar(bloque: suspend () -> R): R
}

class RoomTransactionRunner(private val db: CasaJojoDatabase) : TransactionRunner {
    override suspend fun <R> ejecutar(bloque: suspend () -> R): R = db.withTransaction { bloque() }
}
