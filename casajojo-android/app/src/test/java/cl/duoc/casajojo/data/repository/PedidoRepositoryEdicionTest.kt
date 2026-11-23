package cl.duoc.casajojo.data.repository

import cl.duoc.casajojo.data.local.TransactionRunner
import cl.duoc.casajojo.data.local.dao.PedidoDao
import cl.duoc.casajojo.data.local.dao.ProductoDao
import cl.duoc.casajojo.data.local.entity.Pedido
import cl.duoc.casajojo.data.remote.MenuApi
import cl.duoc.casajojo.data.remote.PedidoApi
import cl.duoc.casajojo.data.remote.dto.PedidoEdicionDto
import cl.duoc.casajojo.data.remote.dto.PedidoResponseDto
import cl.duoc.casajojo.domain.error.ConflictoException
import cl.duoc.casajojo.domain.error.NoEncontradoException
import cl.duoc.casajojo.domain.error.RedException
import cl.duoc.casajojo.domain.error.SesionExpiradaException
import cl.duoc.casajojo.domain.error.SinPermisoException
import cl.duoc.casajojo.domain.error.ValidacionException
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Test
import retrofit2.HttpException
import retrofit2.Response
import java.io.IOException

/** EFT / RF-12: editar pedido y errores traducidos a la jerarquía CasaJojoException. */
class PedidoRepositoryEdicionTest {

    private val transacciones = object : TransactionRunner {
        override suspend fun <R> ejecutar(bloque: suspend () -> R): R = bloque()
    }
    private val productos = mockk<ProductoDao>(relaxed = true)
    private val pedidos = mockk<PedidoDao>(relaxed = true)
    private val menuApi = mockk<MenuApi>(relaxed = true)
    private val pedidoApi = mockk<PedidoApi>(relaxed = true)
    private val repo = PedidoRepository(productos, pedidos, transacciones, menuApi, pedidoApi, reloj = { 50_000L })

    private fun pedido(estado: String = "recibido", remotoId: Long? = 100L, referencia: String = "ref-1") = Pedido(
        id = 3L, numero = 7, nombre = "Camila", horaRetiro = "13:00", formaPago = "Transferencia",
        indicaciones = "", estadoPedido = estado, estadoPago = "pendiente", total = 4800,
        fecha = 45_000L, usuarioId = 11L, remotoId = remotoId, referencia = referencia,
    )

    private fun http(codigo: Int) = HttpException(Response.error<Any>(codigo, "".toResponseBody()))

    @Test
    fun `editar un pedido en el servidor actualiza ms-ventas y Room`() = runTest {
        coEvery { pedidoApi.editar(100L, any()) } returns PedidoResponseDto(
            id = 100L, nombre = "Cami", horaRetiro = "14:00", formaPago = "Pagar en tienda",
            indicaciones = "Sin ají", estadoPedido = "RECIBIDO", estadoPago = "pendiente",
        )

        val editado = repo.editarPedido("camila@casajojo.cl", pedido(), " Cami ", "14:00", "Pagar en tienda", " Sin ají ")
            .getOrThrow()

        coVerify { pedidoApi.editar(100L, PedidoEdicionDto("Cami", "14:00", "Pagar en tienda", "Sin ají")) }
        editado.nombre shouldBe "Cami"
        editado.horaRetiro shouldBe "14:00"
        editado.formaPago shouldBe "Pagar en tienda"
        editado.estadoPedido shouldBe "recibido"
        coVerify { pedidos.actualizar(editado) }
    }

    @Test
    fun `un pedido que nunca llego al servidor se edita solo en Room`() = runTest {
        coEvery { pedidoApi.misPedidos(any()) } throws IOException("sin red")

        val editado = repo.editarPedido("camila@casajojo.cl", pedido(remotoId = null), "Cami", "14:00", "Transferencia", "")
            .getOrThrow()

        editado.nombre shouldBe "Cami"
        editado.remotoId shouldBe null
        coVerify(exactly = 0) { pedidoApi.editar(any(), any()) }
        coVerify { pedidos.actualizar(editado) }
    }

    @Test
    fun `un pedido en preparacion no se edita`() = runTest {
        val error = repo.editarPedido("c@c.cl", pedido(estado = "preparacion"), "Cami", "14:00", "Transferencia", "")
            .exceptionOrNull()
        error.shouldBeInstanceOf<ConflictoException>()
        error.message shouldBe PedidoRepository.MSG_NO_EDITABLE
        coVerify(exactly = 0) { pedidoApi.editar(any(), any()) }
    }

    @Test
    fun `nombre vacio es un error de validacion del campo nombre`() = runTest {
        val error = repo.editarPedido("c@c.cl", pedido(), "  ", "14:00", "Transferencia", "").exceptionOrNull()
        error.shouldBeInstanceOf<ValidacionException>()
        error.campo shouldBe "nombre"
    }

    @Test
    fun `si la cocina ya lo tomo (409) se sincroniza el estado y se avisa`() = runTest {
        coEvery { pedidoApi.editar(any(), any()) } throws http(409)

        val error = repo.editarPedido("camila@casajojo.cl", pedido(), "Cami", "14:00", "Transferencia", "")
            .exceptionOrNull()
        error.shouldBeInstanceOf<ConflictoException>()
        error.message shouldBe PedidoRepository.MSG_NO_EDITABLE
        coVerify { pedidoApi.misPedidos("camila@casajojo.cl") }
    }

    @Test
    fun `sin red al editar en el servidor falla con RedException`() = runTest {
        coEvery { pedidoApi.editar(any(), any()) } throws IOException("sin red")

        repo.editarPedido("c@c.cl", pedido(), "Cami", "14:00", "Transferencia", "")
            .exceptionOrNull().shouldBeInstanceOf<RedException>()
    }

    // ---------- traducción de errores en cancelar y retirar ----------

    @Test
    fun `cancelar un pedido ajeno (403) da SinPermisoException`() = runTest {
        coEvery { pedidoApi.cambiarEstado(any(), any()) } throws http(403)

        repo.cancelarPedidoRemoto("c@c.cl", pedido(), ahora = 50_000L)
            .exceptionOrNull().shouldBeInstanceOf<SinPermisoException>()
    }

    @Test
    fun `cancelar fuera de plazo da ConflictoException sin llamar al servidor`() = runTest {
        val error = repo.cancelarPedidoRemoto("c@c.cl", pedido(), ahora = 90_000L).exceptionOrNull()
        error.shouldBeInstanceOf<ConflictoException>()
        error.message shouldBe PedidoRepository.MSG_PLAZO_CANCELACION
        coVerify(exactly = 0) { pedidoApi.cambiarEstado(any(), any()) }
    }

    @Test
    fun `cancelar con sesion expirada (401) pide iniciar sesion`() = runTest {
        coEvery { pedidoApi.cambiarEstado(any(), any()) } throws http(401)

        repo.cancelarPedidoRemoto("c@c.cl", pedido(), ahora = 50_000L)
            .exceptionOrNull().shouldBeInstanceOf<SesionExpiradaException>()
    }

    @Test
    fun `retirar un pedido que el servidor no conoce da NoEncontradoException`() = runTest {
        coEvery { pedidoApi.misPedidos(any()) } returns emptyList()

        repo.marcarEntregadoRemoto("c@c.cl", pedido(estado = "listo", remotoId = null), "CODIGO")
            .exceptionOrNull().shouldBeInstanceOf<NoEncontradoException>()
    }

    @Test
    fun `retirar sin correo de sesion da SesionExpirada`() = runTest {
        repo.marcarEntregadoRemoto("", pedido(estado = "listo", remotoId = null), "CODIGO")
            .exceptionOrNull().shouldBeInstanceOf<SesionExpiradaException>()
    }

    @Test
    fun `retirar con un QR equivocado (400) da ValidacionException`() = runTest {
        coEvery { pedidoApi.retirar(any(), any()) } throws http(400)

        val error = repo.marcarEntregadoRemoto("c@c.cl", pedido(estado = "listo"), "OTRO").exceptionOrNull()
        error.shouldBeInstanceOf<ValidacionException>()
        error.message shouldBe "El código QR no corresponde a la tienda"
    }
}
