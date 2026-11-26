package cl.duoc.casajojo.domain.usecase

import io.kotest.matchers.booleans.shouldBeFalse
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import org.junit.Test
import java.time.YearMonth

/** Billetera: reglas de la tarjeta (Luhn, marca, vencimiento, CVV y titular). */
class ValidacionesTarjetaTest {

    private val hoy = YearMonth.of(2026, 11)

    @Test
    fun `Luhn acepta tarjetas de prueba validas`() {
        ValidacionesTarjeta.luhnValido("4242 4242 4242 4242").shouldBeTrue()
        ValidacionesTarjeta.luhnValido("5555555555554444").shouldBeTrue()
        ValidacionesTarjeta.luhnValido("378282246310005").shouldBeTrue()
        ValidacionesTarjeta.validarNumero("4242424242424242").shouldBeNull()
    }

    @Test
    fun `Luhn rechaza un digito cambiado y numeros vacios`() {
        ValidacionesTarjeta.luhnValido("4242424242424241").shouldBeFalse()
        ValidacionesTarjeta.luhnValido("").shouldBeFalse()
        ValidacionesTarjeta.validarNumero("4242424242424241") shouldBe "El número de tarjeta no es válido"
    }

    @Test
    fun `el numero debe tener entre 13 y 19 digitos`() {
        ValidacionesTarjeta.validarNumero("") shouldBe "Ingresa el número de la tarjeta"
        ValidacionesTarjeta.validarNumero("4242 4242") shouldBe "Debe tener entre 13 y 19 dígitos"
        ValidacionesTarjeta.validarNumero("4".repeat(20)) shouldBe "Debe tener entre 13 y 19 dígitos"
    }

    @Test
    fun `detecta la marca por los primeros digitos`() {
        ValidacionesTarjeta.detectarMarca("4242424242424242") shouldBe MarcaTarjeta.VISA
        ValidacionesTarjeta.detectarMarca("5555555555554444") shouldBe MarcaTarjeta.MASTERCARD
        ValidacionesTarjeta.detectarMarca("2223003122003222") shouldBe MarcaTarjeta.MASTERCARD
        ValidacionesTarjeta.detectarMarca("378282246310005") shouldBe MarcaTarjeta.AMEX
        ValidacionesTarjeta.detectarMarca("341111111111111") shouldBe MarcaTarjeta.AMEX
        ValidacionesTarjeta.detectarMarca("6011111111111117") shouldBe MarcaTarjeta.OTRA
        ValidacionesTarjeta.detectarMarca("") shouldBe MarcaTarjeta.OTRA
        MarcaTarjeta.desde("Visa") shouldBe MarcaTarjeta.VISA
        MarcaTarjeta.desde("Diners") shouldBe MarcaTarjeta.OTRA
    }

    @Test
    fun `formatea en grupos de 4 y el vencimiento como MM-AA`() {
        ValidacionesTarjeta.formatearNumero("4242424242424242") shouldBe "4242 4242 4242 4242"
        ValidacionesTarjeta.formatearNumero("42a42 4") shouldBe "4242 4"
        ValidacionesTarjeta.formatearVencimiento("1229") shouldBe "12/29"
        ValidacionesTarjeta.formatearVencimiento("1") shouldBe "1"
        ValidacionesTarjeta.ultimos4("4242 4242 4242 4242") shouldBe "4242"
    }

    @Test
    fun `vencimiento futuro o del mes actual es valido`() {
        ValidacionesTarjeta.validarVencimiento("12/29", hoy).shouldBeNull()
        ValidacionesTarjeta.validarVencimiento("1126", hoy).shouldBeNull()
        ValidacionesTarjeta.leerVencimiento("08/28") shouldBe (8 to 2028)
    }

    @Test
    fun `vencimiento pasado, mal escrito o con mes invalido se rechaza`() {
        ValidacionesTarjeta.validarVencimiento("10/26", hoy) shouldBe "La tarjeta está vencida"
        ValidacionesTarjeta.validarVencimiento("01/20", hoy) shouldBe "La tarjeta está vencida"
        ValidacionesTarjeta.validarVencimiento("13/29", hoy) shouldBe "El mes debe estar entre 01 y 12"
        ValidacionesTarjeta.validarVencimiento("1/29", hoy) shouldBe "Usa el formato MM/AA"
        ValidacionesTarjeta.validarVencimiento("", hoy) shouldBe "Ingresa el vencimiento (MM/AA)"
        ValidacionesTarjeta.leerVencimiento("aa/bb").shouldBeNull()
    }

    @Test
    fun `CVV de 3 digitos, 4 para American Express`() {
        ValidacionesTarjeta.validarCvv("123", MarcaTarjeta.VISA).shouldBeNull()
        ValidacionesTarjeta.validarCvv("1234", MarcaTarjeta.AMEX).shouldBeNull()
        ValidacionesTarjeta.validarCvv("1234", MarcaTarjeta.VISA) shouldBe "El CVV debe tener 3 dígitos"
        ValidacionesTarjeta.validarCvv("123", MarcaTarjeta.AMEX) shouldBe "El CVV debe tener 4 dígitos"
        ValidacionesTarjeta.validarCvv("12a", MarcaTarjeta.MASTERCARD) shouldBe "El CVV debe tener 3 dígitos"
        ValidacionesTarjeta.validarCvv("", MarcaTarjeta.VISA) shouldBe "Ingresa el código de seguridad (CVV)"
    }

    @Test
    fun `titular obligatorio, solo letras y espacios`() {
        ValidacionesTarjeta.validarTitular("Coca Mendoza").shouldBeNull()
        ValidacionesTarjeta.validarTitular("José Ñúñez").shouldBeNull()
        ValidacionesTarjeta.validarTitular("  ") shouldBe "Ingresa el nombre del titular"
        ValidacionesTarjeta.validarTitular("Juan123") shouldBe "Solo letras y espacios"
        ValidacionesTarjeta.validarTitular("J") shouldBe "El nombre es muy corto"
    }

    @Test
    fun `validar revisa el formulario completo`() {
        val ok = ValidacionesTarjeta.validar("4242424242424242", "12/29", "123", "Coca", hoy)
        ok.hayErrores.shouldBeFalse()
        val mal = ValidacionesTarjeta.validar("4242424242424241", "01/20", "1", "", hoy)
        mal.hayErrores.shouldBeTrue()
        mal.numero.shouldNotBeNull()
        mal.vencimiento.shouldNotBeNull()
        mal.cvv.shouldNotBeNull()
        mal.titular.shouldNotBeNull()
    }
}
