package cl.duoc.casajojo

import cl.duoc.casajojo.domain.Contrasenas
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** Pruebas del cifrado de contraseñas (RNF-04). */
class ContrasenasTest {

    @Test
    fun `El hash no contiene la contrasena original`() {
        val salt = Contrasenas.generarSalt()
        val hash = Contrasenas.hash("jojo2026", salt)
        assertFalse(hash.contains("jojo2026"))
    }

    @Test
    fun `La contrasena correcta se verifica`() {
        val salt = Contrasenas.generarSalt()
        val hash = Contrasenas.hash("jojo2026", salt)
        assertTrue(Contrasenas.verificar("jojo2026", salt, hash))
    }

    @Test
    fun `Una contrasena incorrecta no se verifica`() {
        val salt = Contrasenas.generarSalt()
        val hash = Contrasenas.hash("jojo2026", salt)
        assertFalse(Contrasenas.verificar("jojo2025", salt, hash))
    }

    @Test
    fun `Mismo salt y contrasena dan el mismo hash`() {
        val salt = Contrasenas.generarSalt()
        assertEquals(Contrasenas.hash("jojo2026", salt), Contrasenas.hash("jojo2026", salt))
    }

    @Test
    fun `Dos usuarios con la misma contrasena tienen hashes distintos gracias al salt`() {
        val hashA = Contrasenas.hash("jojo2026", Contrasenas.generarSalt())
        val hashB = Contrasenas.hash("jojo2026", Contrasenas.generarSalt())
        assertNotEquals(hashA, hashB)
    }
}