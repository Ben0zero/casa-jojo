package cl.duoc.casajojo.domain.usecase

import cl.duoc.casajojo.domain.model.PlatoDraft
import cl.duoc.casajojo.domain.usecase.ReglasPedido
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Pruebas unitarias de las reglas de negocio (no necesitan emulador). */
class ReglasPedidoTest {

    @Test
    fun `Total de platos suman sus precios`() {
        val platos = listOf(
            PlatoDraft(nombreProteina = "Pollo asado", precio = 4800, solo = false, acompanamientos = listOf("Papas")),
            PlatoDraft(nombreProteina = "Cerdo mongoliano", precio = 3000, solo = true),
        )
        assertEquals(7800, ReglasPedido.total(platos))
    }

    @Test
    fun `Total de un carrito vacio es cero`() {
        assertEquals(0, ReglasPedido.total(emptyList()))
    }

    @Test
    fun `Precio de proteina sola es 3000`() {
        assertEquals(3000, ReglasPedido.precioProteina(4800, solo = true))
    }

    @Test
    fun `Precio de proteina con acompanamientos conserva el precio`() {
        assertEquals(4800, ReglasPedido.precioProteina(4800, solo = false))
    }

    @Test
    fun `Sin acompanamientos se piden al menos uno`() {
        assertEquals("Elige al menos un acompañamiento", ReglasPedido.validarAcompanamientos(false, emptyList()))
    }

    @Test
    fun `Validacion pasa cuando hay acompanamientos`() {
        assertNull(ReglasPedido.validarAcompanamientos(false, listOf("Arroz")))
    }

    @Test
    fun `Solo proteina no exige acompanamientos`() {
        assertNull(ReglasPedido.validarAcompanamientos(true, emptyList()))
    }

    // ---------- NUEVOS: validaciones agregadas en esta corrección ----------

    @Test
    fun `Carrito vacio no se puede confirmar`() {
        assertEquals("Agrega al menos un plato a tu pedido", ReglasPedido.validarCarrito(emptyList()))
    }

    @Test
    fun `Carrito con un plato es valido`() {
        val platos = listOf(PlatoDraft(nombreProteina = "Guatitas", precio = 4600, solo = false, acompanamientos = listOf("Arroz")))
        assertNull(ReglasPedido.validarCarrito(platos))
    }

    @Test
    fun `Nombre en blanco no es valido`() {
        assertEquals("Escribe tu nombre para el retiro", ReglasPedido.validarNombre("   "))
    }

    @Test
    fun `Nombre con texto es valido`() {
        assertNull(ReglasPedido.validarNombre("Nicolás"))
    }

    // ---------- NUEVOS (P1): formulario de Retiro ----------

    @Test
    fun `Retiro sin nombre marca el error en el campo nombre`() {
        val errores = ReglasPedido.validarRetiro("")
        assertEquals("Escribe tu nombre para el retiro", errores.nombre)
        assertTrue(errores.hayErrores)
    }

    @Test
    fun `Retiro con nombre no tiene errores`() {
        val errores = ReglasPedido.validarRetiro("Nicolás")
        assertNull(errores.nombre)
        assertFalse(errores.hayErrores)
    }

    // ---------- NUEVOS (RF-13): cancelar dentro de 15 segundos ----------

    private val fecha = 1_000_000L

    @Test
    fun `Pedido recibido dentro del plazo se puede cancelar`() {
        assertTrue(ReglasPedido.puedeCancelar(fecha, fecha + 5_000, "recibido"))
    }

    @Test
    fun `Pedido recibido justo a los 15 segundos todavia se puede cancelar`() {
        assertTrue(ReglasPedido.puedeCancelar(fecha, fecha + ReglasPedido.PLAZO_CANCELACION_MS, "recibido"))
    }

    @Test
    fun `Pedido recibido fuera del plazo no se puede cancelar`() {
        assertFalse(ReglasPedido.puedeCancelar(fecha, fecha + ReglasPedido.PLAZO_CANCELACION_MS + 1, "recibido"))
    }

    @Test
    fun `Pedido que no esta recibido no se puede cancelar aunque este en plazo`() {
        assertFalse(ReglasPedido.puedeCancelar(fecha, fecha + 1_000, "preparacion"))
        assertFalse(ReglasPedido.puedeCancelar(fecha, fecha + 1_000, "cancelado"))
    }

    @Test
    fun `Tiempo para cancelar baja hasta cero y no queda negativo`() {
        assertEquals(12_000L, ReglasPedido.msParaCancelar(fecha, fecha + 3_000))
        assertEquals(0L, ReglasPedido.msParaCancelar(fecha, fecha + 60_000))
    }

    // ---------- NUEVOS (RF-14): retiro con QR ----------

    @Test
    fun `Pedido cancelado o entregado no se retira con QR`() {
        assertFalse(ReglasPedido.puedeRetirarConQr("cancelado"))
        assertFalse(ReglasPedido.puedeRetirarConQr("entregado"))
        assertTrue(ReglasPedido.puedeRetirarConQr("recibido"))
    }
}