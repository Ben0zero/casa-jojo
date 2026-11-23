package cl.duoc.casajojo.data.repository

import cl.duoc.casajojo.data.local.TransactionRunner
import cl.duoc.casajojo.data.local.dao.PedidoDao
import cl.duoc.casajojo.data.local.dao.ProductoDao
import cl.duoc.casajojo.data.local.entity.Pedido
import cl.duoc.casajojo.data.local.entity.PedidoWithPlatos
import cl.duoc.casajojo.data.local.entity.Plato
import cl.duoc.casajojo.data.local.entity.Producto
import cl.duoc.casajojo.data.remote.MenuApi
import cl.duoc.casajojo.data.remote.PedidoApi
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
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Test
import retrofit2.HttpException
import retrofit2.Response
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
    private val repo = PedidoRepository(
        productos, pedidos, transacciones, menuApi, pedidoApi,
        reloj = { 50_000L },
        nuevaReferencia = { "ref-fija-0001" },
    )

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
        enviado.captured.origen shouldBe "App" // M16
        enviado.captured.referencia shouldBe "ref-fija-0001" // M9
        guardado.captured.referencia shouldBe "ref-fija-0001"
        coVerify { pedidos.marcarEnviado(21L, 100L, 50_000L) } // A4 + M13
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
        coVerify { pedidos.actualizarFecha(21L, 50_000L) } // M13
        coVerify(exactly = 0) { pedidos.marcarEnviado(any(), any(), any()) } // A4: queda pendiente
    }

    // ---------- A4 / M9 / M11: pendientes, referencia y sincronización ----------

    private fun pedidoLocal(
        id: Long,
        numero: Int,
        remotoId: Long? = null,
        referencia: String = "",
        estado: String = "recibido",
    ) = Pedido(
        id = id,
        numero = numero,
        nombre = "Camila",
        horaRetiro = "13:00",
        formaPago = "Transferencia",
        indicaciones = "",
        estadoPedido = estado,
        estadoPago = "pendiente",
        total = 4800,
        fecha = 1L,
        usuarioId = 11L,
        remotoId = remotoId,
        referencia = referencia,
    )

    @Test
    fun `A4 reenvia los pedidos pendientes con su referencia y guarda el id remoto`() = runTest {
        val pendiente = PedidoWithPlatos(
            pedidoLocal(id = 5, numero = 3, referencia = "ref-pendiente-01"),
            listOf(Plato(pedidoId = 5, nombreProteina = "Pollo asado", precio = 4800, solo = false,
                acompanamientos = listOf("Arroz"))),
        )
        coEvery { pedidos.pendientesDeEnvio(11L) } returns listOf(pendiente)
        coEvery { pedidoApi.crear(any()) } returns PedidoResponseDto(
            id = 300, numero = 3, estadoPedido = "RECIBIDO", referencia = "ref-pendiente-01",
        )

        repo.enviarPendientes(11L, "camila@casajojo.cl") shouldBe 1

        val enviado = slot<PedidoRequestDto>()
        coVerify { pedidoApi.crear(capture(enviado)) }
        enviado.captured.referencia shouldBe "ref-pendiente-01"
        enviado.captured.origen shouldBe "App"
        enviado.captured.total shouldBe 4800
        val actualizado = slot<Pedido>()
        coVerify { pedidos.actualizar(capture(actualizado)) }
        actualizado.captured.remotoId shouldBe 300L
        actualizado.captured.fecha shouldBe 1L // reenviar no reabre el plazo de cancelación
    }

    @Test
    fun `A4 sin red los pendientes siguen pendientes y no se pierde nada`() = runTest {
        coEvery { pedidos.pendientesDeEnvio(11L) } returns listOf(
            PedidoWithPlatos(pedidoLocal(id = 5, numero = 3, referencia = "ref-a")),
            PedidoWithPlatos(pedidoLocal(id = 6, numero = 4, referencia = "ref-b")),
        )
        coEvery { pedidoApi.crear(any()) } throws IOException("sin red")

        repo.enviarPendientes(11L, "camila@casajojo.cl") shouldBe 0

        coVerify(exactly = 1) { pedidoApi.crear(any()) } // se detiene al primer error de red
        coVerify(exactly = 0) { pedidos.actualizar(any()) }
    }

    @Test
    fun `A4 sin sesion no reenvia nada`() = runTest {
        repo.enviarPendientes(null, "camila@casajojo.cl") shouldBe 0
        repo.enviarPendientes(11L, "") shouldBe 0
        coVerify(exactly = 0) { pedidos.pendientesDeEnvio(any()) }
    }

    @Test
    fun `M11 sincroniza el estado de todos los pedidos por id remoto o referencia`() = runTest {
        coEvery { pedidos.pendientesDeEnvio(11L) } returns emptyList()
        coEvery { pedidos.delUsuario(11L) } returns listOf(
            pedidoLocal(id = 1, numero = 1, remotoId = 100),
            pedidoLocal(id = 2, numero = 2, referencia = "ref-dos"),
            pedidoLocal(id = 3, numero = 3, referencia = "ref-sin-remoto"),
        )
        coEvery { pedidoApi.misPedidos("camila@casajojo.cl") } returns listOf(
            PedidoResponseDto(id = 100, numero = 1, estadoPedido = "ENTREGADO", estadoPago = "pagado"),
            PedidoResponseDto(id = 200, numero = 2, estadoPedido = "EN_PREPARACION", referencia = "ref-dos"),
            // Mismo número 3 pero de OTRO teléfono (otra referencia): no debe tocar el pedido local 3.
            PedidoResponseDto(id = 999, numero = 3, estadoPedido = "CANCELADO", referencia = "ref-de-otro-telefono"),
        )

        repo.sincronizarEstados(11L, "camila@casajojo.cl")

        val actualizados = mutableListOf<Pedido>()
        coVerify(exactly = 2) { pedidos.actualizar(capture(actualizados)) }
        actualizados.first { it.id == 1L }.estadoPedido shouldBe "entregado"
        actualizados.first { it.id == 1L }.estadoPago shouldBe "pagado"
        actualizados.first { it.id == 2L }.estadoPedido shouldBe "preparacion"
        actualizados.first { it.id == 2L }.remotoId shouldBe 200L
    }

    @Test
    fun `M9 un pedido antiguo sin referencia se empareja por numero una sola vez`() = runTest {
        coEvery { pedidos.pendientesDeEnvio(11L) } returns emptyList()
        coEvery { pedidos.delUsuario(11L) } returns listOf(pedidoLocal(id = 4, numero = 7))
        coEvery { pedidoApi.misPedidos(any()) } returns listOf(
            PedidoResponseDto(id = 70, numero = 7, estadoPedido = "LISTO"),
        )

        repo.sincronizarEstados(11L, "camila@casajojo.cl")

        val actualizado = slot<Pedido>()
        coVerify { pedidos.actualizar(capture(actualizado)) }
        actualizado.captured.estadoPedido shouldBe "listo"
        actualizado.captured.remotoId shouldBe 70L
    }

    @Test
    fun `sincronizar sin sesion no llama a la API`() = runTest {
        repo.sincronizarEstados(null, "camila@casajojo.cl")
        repo.sincronizarEstados(11L, "")

        coVerify(exactly = 0) { pedidoApi.misPedidos(any()) }
    }

    private fun pedidoReciente(fecha: Long) = Pedido(
        numero = 7,
        nombre = "Camila",
        horaRetiro = "13:00",
        formaPago = "Efectivo",
        indicaciones = "",
        estadoPedido = "recibido",
        estadoPago = "pendiente",
        total = 4800,
        fecha = fecha,
    )

    @Test
    fun `cancelar pedido remoto hace PUT CANCELADO y actualiza Room`() = runTest {
        val ahora = System.currentTimeMillis()
        val pedido = pedidoReciente(ahora - 1_000L)
        coEvery { pedidoApi.misPedidos("camila@casajojo.cl") } returns listOf(
            PedidoResponseDto(id = 100, numero = 7, estadoPedido = "RECIBIDO"),
        )
        coEvery { pedidoApi.cambiarEstado(100L, "CANCELADO") } returns PedidoResponseDto(
            id = 100,
            numero = 7,
            estadoPedido = "CANCELADO",
        )
        coEvery { pedidos.cancelar(any(), any()) } returns 1

        val resultado = repo.cancelarPedidoRemoto("camila@casajojo.cl", pedido, ahora)

        resultado.isSuccess shouldBe true
        coVerify { pedidoApi.cambiarEstado(100L, "CANCELADO") }
        coVerify { pedidos.cancelar(pedido.id, any()) }
        // M9: el id remoto encontrado queda guardado para no volver a buscarlo.
        val guardado = slot<Pedido>()
        coVerify(exactly = 1) { pedidos.actualizar(capture(guardado)) }
        guardado.captured.remotoId shouldBe 100L
    }

    @Test
    fun `cancelar pedido remoto fuera de plazo no llama a la API`() = runTest {
        val ahora = System.currentTimeMillis()
        val pedido = pedidoReciente(ahora - 20_000L)

        val resultado = repo.cancelarPedidoRemoto("camila@casajojo.cl", pedido, ahora)

        resultado.isFailure shouldBe true
        coVerify(exactly = 0) { pedidoApi.misPedidos(any()) }
        coVerify(exactly = 0) { pedidoApi.cambiarEstado(any(), any()) }
        coVerify(exactly = 0) { pedidos.cancelar(any(), any()) }
    }

    @Test
    fun `cancelar sin red un pedido que SI esta en el servidor no cancela la cache local`() = runTest {
        val pedido = pedidoReciente(System.currentTimeMillis()).copy(remotoId = 100L)
        coEvery { pedidoApi.cambiarEstado(any(), any()) } throws IOException("sin red")
        coEvery { pedidoApi.misPedidos(any()) } throws IOException("sin red")

        val resultado = repo.cancelarPedidoRemoto("camila@casajojo.cl", pedido)

        resultado.isFailure shouldBe true
        coVerify(exactly = 0) { pedidos.cancelar(any(), any()) }
        coVerify(exactly = 0) { pedidos.actualizar(any()) }
    }

    @Test
    fun `retiro con QR envia el codigo y queda ENTREGADO y pagado en un solo PUT`() = runTest {
        val pedido = pedidoReciente(System.currentTimeMillis()).copy(estadoPedido = "listo")
        coEvery { pedidoApi.misPedidos("camila@casajojo.cl") } returns listOf(
            PedidoResponseDto(id = 100, numero = 7, estadoPedido = "LISTO"),
        )
        coEvery { pedidoApi.retirar(100L, "CASAJOJO-RETIRO") } returns PedidoResponseDto(
            id = 100,
            numero = 7,
            estadoPedido = "ENTREGADO",
            estadoPago = "pagado",
        )
        coEvery { pedidos.marcarEntregado(pedido.id) } returns 1

        val resultado = repo.marcarEntregadoRemoto("camila@casajojo.cl", pedido, "CASAJOJO-RETIRO")

        resultado.isSuccess shouldBe true
        resultado.getOrNull()?.estadoPago shouldBe "pagado"
        coVerify { pedidoApi.retirar(100L, "CASAJOJO-RETIRO") }
        coVerify(exactly = 0) { pedidoApi.cambiarEstado(any(), any()) }
        coVerify { pedidos.marcarEntregado(pedido.id) }
    }

    @Test
    fun `retiro con QR rechazado por el servidor (400) muestra un mensaje claro y no toca Room`() = runTest {
        val pedido = pedidoReciente(System.currentTimeMillis()).copy(estadoPedido = "listo")
        coEvery { pedidoApi.misPedidos(any()) } returns listOf(PedidoResponseDto(id = 100, numero = 7))
        coEvery { pedidoApi.retirar(any(), any()) } throws
            HttpException(Response.error<Any>(400, "{}".toResponseBody()))

        val resultado = repo.marcarEntregadoRemoto("camila@casajojo.cl", pedido, "OTRO-CODIGO")

        resultado.exceptionOrNull()?.message shouldBe "El código QR no corresponde a la tienda"
        coVerify(exactly = 0) { pedidos.marcarEntregado(any()) }
    }

    @Test
    fun `retiro de un pedido que no esta listo no llama al servidor`() = runTest {
        val pedido = pedidoReciente(System.currentTimeMillis())

        val resultado = repo.marcarEntregadoRemoto("camila@casajojo.cl", pedido, "CASAJOJO-RETIRO")

        resultado.exceptionOrNull()?.message shouldBe "Tu pedido aún no está listo para retirar"
        coVerify(exactly = 0) { pedidoApi.retirar(any(), any()) }
    }

    @Test
    fun `cancelar fuera de plazo segun el servidor (409) muestra el plazo vencido`() = runTest {
        val pedido = pedidoReciente(System.currentTimeMillis())
        coEvery { pedidoApi.misPedidos(any()) } returns listOf(PedidoResponseDto(id = 100, numero = 7))
        coEvery { pedidoApi.cambiarEstado(100L, "CANCELADO") } throws
            HttpException(Response.error<Any>(409, "{}".toResponseBody()))

        val resultado = repo.cancelarPedidoRemoto("camila@casajojo.cl", pedido)

        resultado.exceptionOrNull()?.message shouldBe "El plazo de 15 segundos para cancelar ya terminó"
        coVerify(exactly = 0) { pedidos.cancelar(any(), any()) }
    }

    @Test
    fun `M13 cancelar un pedido que nunca llego al servidor lo cancela solo en el telefono`() = runTest {
        val pedido = pedidoReciente(System.currentTimeMillis()).copy(referencia = "ref-local")
        coEvery { pedidoApi.misPedidos(any()) } throws IOException("sin red")
        coEvery { pedidos.cancelar(pedido.id, any()) } returns 1

        val resultado = repo.cancelarPedidoRemoto("camila@casajojo.cl", pedido, System.currentTimeMillis())

        resultado.isSuccess shouldBe true
        resultado.getOrNull()?.estadoPedido shouldBe "CANCELADO"
        coVerify { pedidos.cancelar(pedido.id, any()) }
        coVerify(exactly = 0) { pedidoApi.cambiarEstado(any(), any()) }
    }

    @Test
    fun `cancelar un pedido con id remoto guardado no consulta mis-pedidos`() = runTest {
        val pedido = pedidoReciente(System.currentTimeMillis()).copy(remotoId = 100L)
        coEvery { pedidoApi.cambiarEstado(100L, "CANCELADO") } returns PedidoResponseDto(id = 100, estadoPedido = "CANCELADO")
        coEvery { pedidos.cancelar(any(), any()) } returns 1

        repo.cancelarPedidoRemoto("camila@casajojo.cl", pedido, System.currentTimeMillis()).isSuccess shouldBe true

        coVerify(exactly = 0) { pedidoApi.misPedidos(any()) }
        coVerify { pedidoApi.cambiarEstado(100L, "CANCELADO") }
    }

    @Test
    fun `marcar entregado remoto sin red no toca Room`() = runTest {
        val pedido = pedidoReciente(System.currentTimeMillis()).copy(estadoPedido = "listo")
        coEvery { pedidoApi.misPedidos(any()) } throws IOException("sin red")

        val resultado = repo.marcarEntregadoRemoto("camila@casajojo.cl", pedido, "CASAJOJO-RETIRO")

        resultado.isFailure shouldBe true
        coVerify(exactly = 0) { pedidoApi.retirar(any(), any()) }
        coVerify(exactly = 0) { pedidos.marcarEntregado(any()) }
    }
}
