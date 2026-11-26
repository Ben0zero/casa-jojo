package cl.duoc.casajojo.data.repository

import cl.duoc.casajojo.data.local.TransactionRunner
import cl.duoc.casajojo.data.local.dao.TarjetaDao
import cl.duoc.casajojo.data.local.entity.Tarjeta
import io.kotest.matchers.booleans.shouldBeFalse
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldNotContain
import io.kotest.matchers.types.shouldBeInstanceOf
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.runTest
import org.junit.Test
import java.time.YearMonth

/** DAO en memoria con las mismas reglas que las consultas SQL de TarjetaDao. */
class TarjetaDaoFalso : TarjetaDao {
    val filas = MutableStateFlow<List<Tarjeta>>(emptyList())
    private var siguienteId = 1L

    override fun deUsuario(usuarioId: Long): Flow<List<Tarjeta>> = filas.map { lista ->
        lista.filter { it.usuarioId == usuarioId }
            .sortedWith(compareByDescending<Tarjeta> { it.predeterminada }.thenByDescending { it.id })
    }

    override suspend fun listaDeUsuario(usuarioId: Long): List<Tarjeta> =
        filas.value.filter { it.usuarioId == usuarioId }.sortedByDescending { it.id }

    override suspend fun insertar(tarjeta: Tarjeta): Long {
        val id = siguienteId++
        filas.value = filas.value + tarjeta.copy(id = id)
        return id
    }

    override suspend fun marcarPredeterminada(usuarioId: Long, id: Long) {
        filas.value = filas.value.map { if (it.usuarioId == usuarioId) it.copy(predeterminada = it.id == id) else it }
    }

    override suspend fun eliminar(usuarioId: Long, id: Long): Int {
        val antes = filas.value.size
        filas.value = filas.value.filterNot { it.usuarioId == usuarioId && it.id == id }
        return antes - filas.value.size
    }

    override suspend fun borrarDelUsuario(usuarioId: Long) {
        filas.value = filas.value.filterNot { it.usuarioId == usuarioId }
    }
}

/** Transacción de prueba: solo ejecuta el bloque. */
object SinTransaccion : TransactionRunner {
    override suspend fun <R> ejecutar(bloque: suspend () -> R): R = bloque()
}

/** Billetera: repositorio de tarjetas (guardar enmascarada, predeterminada y eliminar). */
class TarjetaRepositoryTest {

    private val dao = TarjetaDaoFalso()
    private val repo = TarjetaRepository(dao, SinTransaccion, hoy = { YearMonth.of(2026, 11) })

    @Test
    fun `agrega una tarjeta enmascarada y la primera queda predeterminada`() = runTest {
        val t = repo.agregar(7L, "4242 4242 4242 4242", "12/29", "123", " coca mendoza ", " Personal ").getOrThrow()

        t.marca shouldBe "Visa"
        t.ultimos4 shouldBe "4242"
        t.titular shouldBe "COCA MENDOZA"
        t.mesVencimiento shouldBe 12
        t.anioVencimiento shouldBe 2029
        t.alias shouldBe "Personal"
        t.predeterminada.shouldBeTrue()
        t.etiqueta shouldBe "Visa •••• 4242"
        t.vencimiento shouldBe "12/29"
        repo.tarjetasDe(7L).first() shouldHaveSize 1
    }

    @Test
    fun `nunca guarda el numero completo ni el CVV`() = runTest {
        repo.agregar(7L, "5555555555554444", "0828", "987", "Coca").getOrThrow()
        val guardado = dao.filas.value.single().toString()
        guardado shouldNotContain "5555555555554444"
        guardado shouldNotContain "555555555555"
        guardado shouldNotContain "987"
    }

    @Test
    fun `la segunda tarjeta no es predeterminada y se puede elegir`() = runTest {
        val visa = repo.agregar(7L, "4242424242424242", "12/29", "123", "Coca").getOrThrow()
        val mc = repo.agregar(7L, "5555555555554444", "08/28", "456", "Coca").getOrThrow()
        mc.predeterminada.shouldBeFalse()

        repo.marcarPredeterminada(7L, mc.id)
        val lista = repo.tarjetasDe(7L).first()
        lista.first().id shouldBe mc.id
        lista.single { it.id == visa.id }.predeterminada.shouldBeFalse()
    }

    @Test
    fun `al eliminar la predeterminada otra pasa a serlo`() = runTest {
        val visa = repo.agregar(7L, "4242424242424242", "12/29", "123", "Coca").getOrThrow()
        val mc = repo.agregar(7L, "5555555555554444", "08/28", "456", "Coca").getOrThrow()

        repo.eliminar(7L, visa).shouldBeTrue()
        val quedan = repo.tarjetasDe(7L).first()
        quedan.single().id shouldBe mc.id
        quedan.single().predeterminada.shouldBeTrue()
        repo.eliminar(7L, visa).shouldBeFalse()
    }

    @Test
    fun `datos invalidos no se guardan y devuelven el error por campo`() = runTest {
        val r = repo.agregar(7L, "4242424242424241", "01/20", "1", "")
        val e = r.exceptionOrNull()
        e.shouldBeInstanceOf<TarjetaInvalidaException>()
        e.errores.numero.shouldNotBeNull()
        e.errores.vencimiento.shouldNotBeNull()
        dao.filas.value.shouldBeEmpty()
    }

    @Test
    fun `cada cuenta ve solo sus tarjetas y se borran al eliminar la cuenta`() = runTest {
        repo.agregar(7L, "4242424242424242", "12/29", "123", "Coca").getOrThrow()
        repo.agregar(8L, "5555555555554444", "08/28", "456", "Otra").getOrThrow()
        repo.tarjetasDe(7L).first() shouldHaveSize 1

        repo.borrarDelUsuario(7L)
        repo.tarjetasDe(7L).first().shouldBeEmpty()
        repo.tarjetasDe(8L).first() shouldHaveSize 1
    }
}
