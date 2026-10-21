package cl.duoc.casajojo

import cl.duoc.casajojo.domain.PlatoDraft
import cl.duoc.casajojo.domain.ReglasPedido
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
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
}