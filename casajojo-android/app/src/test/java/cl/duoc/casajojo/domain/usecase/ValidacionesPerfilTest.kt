package cl.duoc.casajojo.domain.usecase

import io.kotest.matchers.booleans.shouldBeFalse
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import org.junit.Test

/** EFT: validaciones de recuperación de contraseña y del Perfil (domain puro). */
class ValidacionesPerfilTest {

    @Test
    fun `codigo valido tiene exactamente 6 digitos`() {
        ValidacionesPerfil.validarCodigo("482913").shouldBeNull()
        ValidacionesPerfil.validarCodigo(" 482913 ").shouldBeNull()
        ValidacionesPerfil.validarCodigo("") shouldBe "Ingresa el código que te enviamos"
        ValidacionesPerfil.validarCodigo("12345") shouldBe "El código tiene 6 dígitos"
        ValidacionesPerfil.validarCodigo("12a456") shouldBe "El código tiene 6 dígitos"
    }

    @Test
    fun `recuperar exige un correo valido`() {
        ValidacionesPerfil.validarRecuperar("ana@correo.cl").hayErrores.shouldBeFalse()
        val errores = ValidacionesPerfil.validarRecuperar("ana")
        errores.hayErrores.shouldBeTrue()
        errores.correo shouldBe "Ingresa un correo válido (ej.: nombre@correo.cl)"
    }

    @Test
    fun `restablecer valida codigo, contraseña y confirmacion por campo`() {
        ValidacionesPerfil.validarRestablecer("123456", "Nueva123", "Nueva123").hayErrores.shouldBeFalse()

        val errores = ValidacionesPerfil.validarRestablecer("12", "abc", "xyz")
        errores.codigo shouldBe "El código tiene 6 dígitos"
        errores.password shouldBe "Debe tener al menos 6 caracteres"
        errores.confirmar shouldBe "Las contraseñas no coinciden"
        errores.hayErrores.shouldBeTrue()
    }

    @Test
    fun `perfil valida nombre y celular chileno`() {
        ValidacionesPerfil.validarPerfil("Camila", "+56 9 1234 5678").hayErrores.shouldBeFalse()

        val errores = ValidacionesPerfil.validarPerfil(" ", "12345")
        errores.nombre shouldBe "El nombre es obligatorio"
        errores.telefono shouldBe "Ingresa un celular de 9 dígitos que empiece con 9"
        errores.hayErrores.shouldBeTrue()
    }

    @Test
    fun `cambio de contraseña exige la actual y una nueva distinta`() {
        ValidacionesPerfil.validarCambioPassword("Vieja123", "Nueva123", "Nueva123").hayErrores.shouldBeFalse()

        val vacia = ValidacionesPerfil.validarCambioPassword("", "Nueva123", "Nueva123")
        vacia.actual shouldBe "Ingresa tu contraseña actual"

        val repetida = ValidacionesPerfil.validarCambioPassword("Clave123", "Clave123", "Clave123")
        repetida.nueva shouldBe "Debe ser distinta de la actual"

        val debil = ValidacionesPerfil.validarCambioPassword("Clave123", "soloLetras", "otra")
        debil.nueva shouldBe "Debe tener letras y números"
        debil.confirmar shouldBe "Las contraseñas no coinciden"
        debil.hayErrores.shouldBeTrue()
    }
}
