package cl.duoc.casajojo.domain.model

import cl.duoc.casajojo.domain.usecase.ReglasPedido
import io.kotest.matchers.booleans.shouldBeFalse
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import org.junit.Test

/** EFT (POO): herencia y polimorfismo de EstadoPedido y FormaPago. */
class JerarquiasPooTest {

    @Test
    fun `cada estado entrega su propia etiqueta y color`() {
        EstadoPedido.todos.map { it.etiqueta() } shouldBe
            listOf("Recibido", "En preparación", "Listo", "Entregado", "Cancelado")
        EstadoPedido.Recibido.color() shouldBe 0xFF6D6D67
        EstadoPedido.EnPreparacion.color() shouldBe 0xFFB7791F
        EstadoPedido.Listo.color() shouldBe 0xFF2B6CB0
        EstadoPedido.Entregado.color() shouldBe 0xFF2F855A
        EstadoPedido.Cancelado.color() shouldBe 0xFFC53030
    }

    @Test
    fun `siguiente recorre la maquina de estados de la cocina`() {
        EstadoPedido.Recibido.siguiente() shouldBe EstadoPedido.EnPreparacion
        EstadoPedido.EnPreparacion.siguiente() shouldBe EstadoPedido.Listo
        EstadoPedido.Listo.siguiente() shouldBe EstadoPedido.Entregado
        EstadoPedido.Entregado.siguiente().shouldBeNull()
        EstadoPedido.Cancelado.siguiente().shouldBeNull()
        EstadoPedido.Entregado.esFinal.shouldBeTrue()
        EstadoPedido.Recibido.esFinal.shouldBeFalse()
    }

    @Test
    fun `solo Recibido se cancela y edita, y solo Listo se retira con QR`() {
        EstadoPedido.Recibido.puedeCancelar(fecha = 0, ahora = 15_000).shouldBeTrue()
        EstadoPedido.Recibido.puedeCancelar(fecha = 0, ahora = 15_001).shouldBeFalse()
        EstadoPedido.todos.filter { it.puedeEditar() } shouldBe listOf(EstadoPedido.Recibido)
        EstadoPedido.todos.filter { it.puedeRetirarConQr() } shouldBe listOf(EstadoPedido.Listo)
        EstadoPedido.todos.filter { it.puedeCancelar(0, 0) } shouldBe listOf(EstadoPedido.Recibido)
    }

    @Test
    fun `conversion desde Room y desde ms-ventas`() {
        EstadoPedido.desde("listo") shouldBe EstadoPedido.Listo
        EstadoPedido.desde("raro") shouldBe EstadoPedido.Recibido
        EstadoPedido.desdeOrNull("raro").shouldBeNull()
        EstadoPedido.desdeRemoto("EN_PREPARACION") shouldBe EstadoPedido.EnPreparacion
        EstadoPedido.desdeRemoto("cancelado") shouldBe EstadoPedido.Cancelado
        EstadoPedido.desdeRemoto(null) shouldBe EstadoPedido.Recibido
        EstadoPedido.todos.map { it.paso } shouldBe listOf(0, 1, 2, 3, -1)
    }

    @Test
    fun `ReglasPedido delega en la jerarquia sin cambiar el comportamiento`() {
        ReglasPedido.puedeCancelar(0, 1_000, "recibido").shouldBeTrue()
        ReglasPedido.puedeCancelar(0, 1_000, "desconocido").shouldBeFalse()
        ReglasPedido.puedeEditar("recibido").shouldBeTrue()
        ReglasPedido.puedeEditar("listo").shouldBeFalse()
        ReglasPedido.puedeRetirarConQr("listo").shouldBeTrue()
        ReglasPedido.puedeRetirarConQr("otro").shouldBeFalse()
    }

    @Test
    fun `forma de pago sabe si se paga al retirar`() {
        FormaPago.etiquetas shouldBe listOf("Transferencia", "Pagar en tienda", "Tarjeta")
        FormaPago.desde("Tarjeta").pagaAlRetirar().shouldBeFalse()
        FormaPago.desde("Pagar en tienda").pagaAlRetirar().shouldBeTrue()
        FormaPago.desde("Transferencia").pagaAlRetirar().shouldBeFalse()
        FormaPago.desde("Bitcoin") shouldBe FormaPago.Transferencia
        FormaPago.todas.all { it.descripcion().isNotBlank() }.shouldBeTrue()
    }

    @Test
    fun `validar edicion distingue estado no editable de nombre vacio`() {
        ReglasPedido.validarEdicion("recibido", "Ana").shouldBeNull()
        ReglasPedido.validarEdicion("recibido", " ") shouldBe "Escribe tu nombre para el retiro"
        ReglasPedido.validarEdicion("preparacion", "Ana") shouldBe
            "Tu pedido ya está en preparación y no se puede editar"
    }
}
