package cl.duoc.casajojo.ui.components

import cl.duoc.casajojo.R
import cl.duoc.casajojo.data.local.MenuSeed
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class ImagenesPlatosTest {

    private val skus = (1..6).map { "CJ-PROT-0$it" } + (1..8).map { "CJ-ACOM-0$it" }

    @Test
    fun `todos los sku del menu tienen imagen y son distintas`() {
        val imagenes = skus.map { sku ->
            assertNotNull("Sin imagen para $sku", imagenDePlato(sku, null))
            imagenDePlato(sku, null)
        }
        assertEquals(14, imagenes.toSet().size)
    }

    @Test
    fun `todos los nombres del menu local tienen imagen`() {
        MenuSeed.menu().forEach { p ->
            assertNotNull("Sin imagen para ${p.nombre}", imagenDePlato(null, p.nombre))
        }
    }

    @Test
    fun `sku desconocido sin nombre devuelve null`() {
        assertNull(imagenDePlato("CJ-PROT-99", null))
        assertNull(imagenDePlato(null, null))
        assertNull(imagenDePlato(null, "Plato nuevo del backend"))
    }

    @Test
    fun `sku desconocido cae al nombre normalizado`() {
        assertEquals(R.drawable.plato_pure, imagenDePlato("CJ-ACOM-99", "  PURÉ "))
        assertEquals(
            R.drawable.plato_tortilla_porotos_verdes,
            imagenDePlato(null, "Tortilla de  Porotos Verdes"),
        )
    }

    @Test
    fun `el sku tiene prioridad sobre el nombre`() {
        assertEquals(R.drawable.plato_pollo_asado, imagenDePlato("cj-prot-01", "Ensalada"))
    }

    @Test
    fun `normaliza tildes, mayusculas y espacios`() {
        assertEquals("pure", normalizarNombrePlato(" Puré "))
        assertEquals("fideos al pesto", normalizarNombrePlato("FIDEOS   al Pesto"))
    }
}
