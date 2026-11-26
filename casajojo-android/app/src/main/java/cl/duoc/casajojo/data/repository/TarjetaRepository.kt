package cl.duoc.casajojo.data.repository

import cl.duoc.casajojo.data.local.TransactionRunner
import cl.duoc.casajojo.data.local.dao.TarjetaDao
import cl.duoc.casajojo.data.local.entity.Tarjeta
import cl.duoc.casajojo.domain.usecase.TarjetaErrores
import cl.duoc.casajojo.domain.usecase.ValidacionesTarjeta
import kotlinx.coroutines.flow.Flow
import java.time.YearMonth

/** El formulario de la tarjeta tiene errores; [errores] dice cuál campo falló. */
class TarjetaInvalidaException(val errores: TarjetaErrores) :
    Exception("Revisa los datos de la tarjeta")

/**
 * Billetera (SIMULADA): tarjetas guardadas SOLO en Room, por usuario.
 * No hay microservicio de pagos: nada de esto viaja a un servidor ni se cobra.
 *
 * SEGURIDAD: [agregar] recibe el número y el CVV únicamente para validarlos. El objeto
 * que se inserta ([Tarjeta]) no tiene dónde guardarlos: se queda con la marca, los
 * últimos 4 dígitos y el vencimiento. Este repositorio tampoco registra nada en Log.
 */
class TarjetaRepository(
    private val dao: TarjetaDao,
    private val transacciones: TransactionRunner,
    private val hoy: () -> YearMonth = { YearMonth.now() },
) {

    fun tarjetasDe(usuarioId: Long): Flow<List<Tarjeta>> = dao.deUsuario(usuarioId)

    /**
     * Valida y guarda una tarjeta enmascarada. La primera tarjeta de la cuenta queda
     * como predeterminada. Devuelve la tarjeta guardada (sin número completo ni CVV).
     */
    suspend fun agregar(
        usuarioId: Long,
        numero: String,
        vencimiento: String,
        cvv: String,
        titular: String,
        alias: String = "",
    ): Result<Tarjeta> {
        val errores = ValidacionesTarjeta.validar(numero, vencimiento, cvv, titular, hoy())
        if (errores.hayErrores) return Result.failure(TarjetaInvalidaException(errores))

        val (mes, anio) = ValidacionesTarjeta.leerVencimiento(vencimiento)
            ?: return Result.failure(TarjetaInvalidaException(errores))
        // Desde aquí el número y el CVV ya no se usan: solo marca y últimos 4.
        val base = Tarjeta(
            usuarioId = usuarioId,
            marca = ValidacionesTarjeta.detectarMarca(numero).etiqueta,
            ultimos4 = ValidacionesTarjeta.ultimos4(numero),
            titular = titular.trim().uppercase(),
            mesVencimiento = mes,
            anioVencimiento = anio,
            alias = alias.trim(),
        )
        val guardada = transacciones.ejecutar {
            val primera = dao.listaDeUsuario(usuarioId).isEmpty()
            val nueva = base.copy(predeterminada = primera)
            nueva.copy(id = dao.insertar(nueva))
        }
        return Result.success(guardada)
    }

    suspend fun marcarPredeterminada(usuarioId: Long, tarjetaId: Long) =
        dao.marcarPredeterminada(usuarioId, tarjetaId)

    /**
     * Elimina una tarjeta. Si era la predeterminada, la más reciente que quede pasa a
     * serlo (así siempre hay una elegida para pagar mientras exista alguna).
     */
    suspend fun eliminar(usuarioId: Long, tarjeta: Tarjeta): Boolean = transacciones.ejecutar {
        val borradas = dao.eliminar(usuarioId, tarjeta.id)
        if (borradas > 0 && tarjeta.predeterminada) {
            dao.listaDeUsuario(usuarioId).firstOrNull()?.let { dao.marcarPredeterminada(usuarioId, it.id) }
        }
        borradas > 0
    }

    suspend fun borrarDelUsuario(usuarioId: Long) = dao.borrarDelUsuario(usuarioId)
}
