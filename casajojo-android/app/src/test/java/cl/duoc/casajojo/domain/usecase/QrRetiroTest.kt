package cl.duoc.casajojo.domain.usecase

import io.kotest.matchers.shouldBe
import org.junit.Test

/** A5: solo se acepta el QR propio de la tienda; nunca una URL cualquiera. */
class QrRetiroTest {

    @Test
    fun `QR de la tienda devuelve el codigo`() {
        QrRetiro.codigo("casajojo://retiro?codigo=CASAJOJO-RETIRO") shouldBe "CASAJOJO-RETIRO"
        QrRetiro.codigo("  CASAJOJO://RETIRO?v=1&codigo=Tienda_01  ") shouldBe "Tienda_01"
    }

    @Test
    fun `URLs web y otros esquemas se rechazan`() {
        QrRetiro.codigo("https://casajojo.cl/retiro?codigo=CASAJOJO-RETIRO") shouldBe null
        QrRetiro.codigo("http://phishing.example/pago") shouldBe null
        QrRetiro.codigo("intent://retiro?codigo=CASAJOJO-RETIRO") shouldBe null
    }

    @Test
    fun `QR propio pero mal formado se rechaza`() {
        QrRetiro.codigo("casajojo://pago?codigo=CASAJOJO-RETIRO") shouldBe null
        QrRetiro.codigo("casajojo://retiro") shouldBe null
        QrRetiro.codigo("casajojo://retiro?codigo=") shouldBe null
        QrRetiro.codigo("casajojo://retiro?codigo=a%20b%3Cscript%3E") shouldBe null
        QrRetiro.codigo("no es un qr") shouldBe null
        QrRetiro.codigo(null) shouldBe null
        QrRetiro.codigo("") shouldBe null
    }
}
