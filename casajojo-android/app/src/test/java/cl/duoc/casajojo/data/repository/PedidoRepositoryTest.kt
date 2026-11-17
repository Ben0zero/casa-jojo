package cl.duoc.casajojo.data.repository

import cl.duoc.casajojo.data.local.TransactionRunner
import cl.duoc.casajojo.data.local.dao.PedidoDao
import cl.duoc.casajojo.data.local.dao.ProductoDao
import cl.duoc.casajojo.data.local.entity.Pedido
import cl.duoc.casajojo.data.local.entity.Plato
import cl.duoc.casajojo.data.local.entity.Producto
import cl.duoc.casajojo.data.remote.MenuApi
import cl.duoc.casajojo.data.remote.PedidoApi
import cl.duoc.casajojo.data.remote.dto.PedidoItemResponseDto
import cl.duoc.casajojo.data.remote.dto.PedidoRequestDto
import cl.duoc.casajojo.data.remote.dto.PedidoResponseDto
import cl.duoc.casajojo.data.remote.dto.PlatoMenuDto
import cl.duoc.casajojo.domain.model.PlatoDraft
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.test.runTest
import org.junit.Test
import java.io.IOException

/** EA3 / IL3.2: menú y pedidos con API mockeada y Room simulado. */
class PedidoRepositoryTest {

    /** En producción es Room.withTransaction; aquí solo ejecuta el bloque. */
    private val transacciones = object : TransactionRunner {
        override suspend fun <R> ejecutar(bloque: suspend () -> R): R = bloque()
    }

    private val productos = mockk<ProductoDao>(relaxed = true)
    private val pedidos = mockk<PedidoDao>(relaxed = true)
    private val menuApi = mockk<MenuApi>(relaxed = true)
    private val pedidoApi = mockk<PedidoApi>(relaxed = true)
    private val repo = PedidoRepository(productos, pedidos, transacciones, menuApi, pedidoApi)

    private val platoDraft = PlatoDraft(
        nombreProteina = "Pollo asado",
        precio = 4800,
        solo = false,
        acompanamientos = listOf("Arroz", "Ensalada"),
    )

    @Test
    fun `sincronizar menu reemplaza la caché con los platos de la API`() = runTest {
        coEvery { menuApi.menu() } returns listOf(
            PlatoMenuDto(1, "CJ-PROT-01", "Pollo asado", "A la parrilla", 4800, "proteina", true),
            PlatoMenuDto(7, "CJ-ACOM-01", "Arroz", "Blanco", 0, "acompanamiento", true),
        )

        repo.sincronizarMenu() shouldBe true

        coVerify { productos.borrarTodo() }
        val insertados = slot<List<Producto>>()
        coVerify { productos.insertAll(capture(insertados)) }
        insertados.captured.size shouldBe 2
        insertados.captured[0].tipo shouldBe Producto.TIPO_PROTEINA
        insertados.captured[1].tipo shouldBe Producto.TIPO_ACOMPANAMIENTO
        insertados.captured[1].precio shouldBe 0
    }

    @Test
    fun `sincronizar menu sin red conserva la caché existente`() = runTest {
        coEvery { menuApi.menu() } throws IOException("sin red")

        repo.sincronizarMenu() shouldBe false

        coVerify(exactly = 0) { productos.borrarTodo() }
        coVerify(exactly = 0) { productos.insertAll(any()) }
    }

    @Test
    fun `registrar pedido guarda platos en Room y envia el carrito a la API`() = runTest {
        coEvery { pedidos.maxNumero() } returns 6
        coEvery { pedidos.insertPedido(any()) } returns 21L
        coEvery { pedidoApi.crear(any()) } returns PedidoResponseDto(id = 100, numero = 7)

        val numero = repo.registrarPedido(
            usuarioId = 11L,
            nombre = "Camila",
            horaRetiro = "13:00",
            formaPago = "Transferencia",
            indicaciones = "Sin cebolla",
            platos = listOf(platoDraft),
            correo = "camila@casajojo.cl",
            origen = "Mostrador",
        )

        numero shouldBe 7
        val guardado = slot<Pedido>()
        coVerify { pedidos.insertPedido(capture(guardado)) }
        guardado.captured.total shouldBe 4800
        guardado.captured.estadoPedido shouldBe "recibido"
        guardado.captured.usuarioId shouldBe 11L

        val detalle = slot<Plato>()
        coVerify { pedidos.insertPlato(capture(detalle)) }
        detalle.captured.pedidoId shouldBe 21L
        detalle.captured.acompanamientos shouldBe listOf("Arroz", "Ensalada")

        val enviado = slot<PedidoRequestDto>()
        coVerify { pedidoApi.crear(capture(enviado)) }
        enviado.captured.numero shouldBe 7
        enviado.captured.correo shouldBe "camila@casajojo.cl"
        enviado.captured.origen shouldBe "Mostrador"
        enviado.captured.items.single().nombre shouldBe "Pollo asado"
        enviado.captured.total shouldBe 4800
    }

    @Test
    fun `registrar pedido sin red lo deja registrado igualmente en la cache`() = runTest {
        coEvery { pedidos.maxNumero() } returns 2
        coEvery { pedidos.insertPedido(any()) } returns 21L
        coEvery { pedidoApi.crear(any()) } throws IOException("sin red")

        val numero = repo.registrarPedido(
            usuarioId = null,
            nombre = "Cliente sin cuenta",
            horaRetiro = "13:00",
            formaPago = "Efectivo",
            indicaciones = "",
            platos = listOf(platoDraft),
        )

        numero shouldBe 3
        coVerify { pedidos.insertPedido(any()) }
        coVerify { pedidos.insertPlato(any()) }
    }

    @Test
    fun `sincronizar estado traduce los estados del microservicio a la cache local`() = runTest {
        coEvery { pedidoApi.ultimo("camila@casajojo.cl") } returns PedidoResponseDto(
            id = 100,
            numero = 7,
            estadoPedido = "EN_PREPARACION",
            estadoPago = "pagado",
            items = listOf(PedidoItemResponseDto(nombre = "Pollo asado", precio = 4800)),
        )
        coEvery { pedidos.porNumero(7) } returns Pedido(
            numero = 7,
            nombre = "Camila",
            horaRetiro = "13:00",
            formaPago = "Transferencia",
            indicaciones = "",
            estadoPedido = "recibido",
            estadoPago = "pendiente",
            total = 4800,
            fecha = 1L,
        )

        repo.sincronizarEstado("camila@casajojo.cl")

        val actualizado = slot<Pedido>()
        coVerify { pedidos.actualizar(capture(actualizado)) }
        actualizado.captured.estadoPedido shouldBe "preparacion"
        actualizado.captured.estadoPago shouldBe "pagado"
    }

    @Test
    fun `sincronizar estado sin correo no llama a la API`() = runTest {
        repo.sincronizarEstado("")

        coVerify(exactly = 0) { pedidoApi.ultimo(any()) }
    }
}
