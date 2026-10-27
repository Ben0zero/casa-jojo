package cl.duoc.casajojo.domain.usecase

import cl.duoc.casajojo.domain.usecase.ValidacionesAuth
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Pruebas de las reglas del formulario de login y registro (fase 1). */
class ValidacionesAuthTest {

    // ---------- Correo ----------

    @Test
    fun `Correo vacio es obligatorio`() {
        assertEquals("El correo es obligatorio", ValidacionesAuth.validarCorreo("  "))
    }

    @Test
    fun `Correo sin arroba no es valido`() {
        assertNotNull(ValidacionesAuth.validarCorreo("nicolas.correo.cl"))
    }

    @Test
    fun `Correo con mayusculas y espacios se acepta y se normaliza`() {
        assertNull(ValidacionesAuth.validarCorreo("  Nicolas@Duoc.CL "))
        assertEquals("nicolas@duoc.cl", ValidacionesAuth.normalizarCorreo("  Nicolas@Duoc.CL "))
    }

    // ---------- Celular ----------

    @Test
    fun `Celular con formatos comunes se normaliza a 9 digitos`() {
        assertEquals("912345678", ValidacionesAuth.normalizarTelefono("+56 9 1234 5678"))
        assertEquals("912345678", ValidacionesAuth.normalizarTelefono("9 1234 5678"))
        assertNull(ValidacionesAuth.validarTelefono("+56912345678"))
    }

    @Test
    fun `Celular corto o que no empieza con 9 no es valido`() {
        assertNotNull(ValidacionesAuth.validarTelefono("12345"))
        assertNotNull(ValidacionesAuth.validarTelefono("812345678"))
    }

    // ---------- Contraseña ----------

    @Test
    fun `Contrasena corta no es valida`() {
        assertEquals("Debe tener al menos 6 caracteres", ValidacionesAuth.validarPassword("ab1"))
    }

    @Test
    fun `Contrasena sin numeros no es valida`() {
        assertEquals("Debe tener letras y números", ValidacionesAuth.validarPassword("abcdefg"))
    }

    @Test
    fun `Contrasena con letras y numeros es valida`() {
        assertNull(ValidacionesAuth.validarPassword("jojo2026"))
    }

    @Test
    fun `Confirmacion distinta se marca`() {
        assertEquals("Las contraseñas no coinciden", ValidacionesAuth.validarConfirmacion("jojo2026", "jojo2025"))
    }

    // ---------- Formularios completos ----------

    @Test
    fun `Registro completo y correcto no tiene errores`() {
        val errores = ValidacionesAuth.validarRegistro(
            nombre = "Nicolás Arancibia",
            correo = "nicolas@duoc.cl",
            telefono = "912345678",
            password = "jojo2026",
            confirmar = "jojo2026",
        )
        assertFalse(errores.hayErrores)
    }

    @Test
    fun `Registro vacio marca los cinco campos`() {
        val errores = ValidacionesAuth.validarRegistro("", "", "", "", "")
        assertTrue(errores.hayErrores)
        assertNotNull(errores.nombre)
        assertNotNull(errores.correo)
        assertNotNull(errores.telefono)
        assertNotNull(errores.password)
        assertNotNull(errores.confirmar)
    }

    @Test
    fun `Login sin contrasena marca solo ese campo`() {
        val errores = ValidacionesAuth.validarLogin("nicolas@duoc.cl", "")
        assertNull(errores.correo)
        assertEquals("Ingresa tu contraseña", errores.password)
    }
}