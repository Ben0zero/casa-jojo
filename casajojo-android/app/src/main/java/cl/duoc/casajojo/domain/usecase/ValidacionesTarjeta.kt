package cl.duoc.casajojo.domain.usecase

import java.time.YearMonth

/**
 * Billetera (SIMULADA): marcas reconocidas y largo de su CVV.
 * American Express usa 4 dígitos de seguridad; el resto, 3.
 */
enum class MarcaTarjeta(val etiqueta: String, val largoCvv: Int) {
    VISA("Visa", 3),
    MASTERCARD("Mastercard", 3),
    AMEX("American Express", 4),
    OTRA("Otra", 3);

    companion object {
        /** Marca desde el texto guardado en Room; desconocido → OTRA. */
        fun desde(etiqueta: String?): MarcaTarjeta = entries.firstOrNull { it.etiqueta == etiqueta } ?: OTRA
    }
}

/** Errores del formulario "Agregar tarjeta" (null = campo correcto), uno por campo. */
data class TarjetaErrores(
    val numero: String? = null,
    val vencimiento: String? = null,
    val cvv: String? = null,
    val titular: String? = null,
) {
    val hayErrores: Boolean get() = listOf(numero, vencimiento, cvv, titular).any { it != null }
}

/**
 * Reglas de la tarjeta, en Kotlin puro (se prueban con JUnit sin emulador).
 *
 * SEGURIDAD: estas funciones solo REVISAN el número y el CVV; no los guardan.
 * Lo único que sale de aquí hacia Room son la marca, los últimos 4 dígitos y el vencimiento.
 */
object ValidacionesTarjeta {

    const val MIN_DIGITOS = 13
    const val MAX_DIGITOS = 19

    private val REGEX_TITULAR = Regex("^[\\p{L} ]+$")

    fun soloDigitos(texto: String): String = texto.filter(Char::isDigit)

    /**
     * Marca según los primeros dígitos (IIN):
     * Visa empieza con 4; Amex con 34 o 37; Mastercard con 51–55 o 2221–2720.
     */
    fun detectarMarca(numero: String): MarcaTarjeta {
        val d = soloDigitos(numero)
        val dos = d.take(2).toIntOrNull()
        val cuatro = d.take(4).toIntOrNull()
        return when {
            d.startsWith("4") -> MarcaTarjeta.VISA
            dos == 34 || dos == 37 -> MarcaTarjeta.AMEX
            dos != null && d.length >= 2 && dos in 51..55 -> MarcaTarjeta.MASTERCARD
            cuatro != null && d.length >= 4 && cuatro in 2221..2720 -> MarcaTarjeta.MASTERCARD
            else -> MarcaTarjeta.OTRA
        }
    }

    /** "4242424242424242" → "4242 4242 4242 4242" (grupos de 4, máximo 19 dígitos). */
    fun formatearNumero(texto: String): String =
        soloDigitos(texto).take(MAX_DIGITOS).chunked(4).joinToString(" ")

    /** "1229" → "12/29": inserta la barra después del mes (máximo 4 dígitos). */
    fun formatearVencimiento(texto: String): String {
        val d = soloDigitos(texto).take(4)
        return if (d.length <= 2) d else d.take(2) + "/" + d.drop(2)
    }

    /**
     * Algoritmo de Luhn: desde la derecha, se duplica un dígito sí y otro no (restando 9
     * si pasa de 9); el número es válido si la suma total es múltiplo de 10.
     */
    fun luhnValido(numero: String): Boolean {
        val d = soloDigitos(numero)
        if (d.isEmpty()) return false
        val suma = d.reversed().mapIndexed { i, c ->
            val n = c.digitToInt()
            if (i % 2 == 1) (n * 2).let { if (it > 9) it - 9 else it } else n
        }.sum()
        return suma % 10 == 0
    }

    fun validarNumero(numero: String): String? {
        val d = soloDigitos(numero)
        return when {
            d.isEmpty() -> "Ingresa el número de la tarjeta"
            d.length !in MIN_DIGITOS..MAX_DIGITOS -> "Debe tener entre $MIN_DIGITOS y $MAX_DIGITOS dígitos"
            !luhnValido(d) -> "El número de tarjeta no es válido"
            else -> null
        }
    }

    private val REGEX_VENCIMIENTO = Regex("^([0-9]{2})/?([0-9]{2})$")

    /** "12/29" o "1229" → (12, 2029); null si no tiene la forma MM/AA. */
    fun leerVencimiento(texto: String): Pair<Int, Int>? {
        val m = REGEX_VENCIMIENTO.matchEntire(texto.trim()) ?: return null
        val (mes, anio) = m.destructured
        return mes.toInt() to 2000 + anio.toInt()
    }

    /** Vencimiento MM/AA con mes 01–12 y que no haya pasado (vence a fin de ese mes). */
    fun validarVencimiento(texto: String, hoy: YearMonth): String? {
        if (texto.isBlank()) return "Ingresa el vencimiento (MM/AA)"
        val (mes, anio) = leerVencimiento(texto) ?: return "Usa el formato MM/AA"
        return when {
            mes !in 1..12 -> "El mes debe estar entre 01 y 12"
            YearMonth.of(anio, mes).isBefore(hoy) -> "La tarjeta está vencida"
            else -> null
        }
    }

    /** CVV de 3 dígitos (4 para American Express). Se valida y se descarta: nunca se guarda. */
    fun validarCvv(cvv: String, marca: MarcaTarjeta): String? = when {
        cvv.isBlank() -> "Ingresa el código de seguridad (CVV)"
        !cvv.all(Char::isDigit) || cvv.length != marca.largoCvv ->
            "El CVV debe tener ${marca.largoCvv} dígitos"
        else -> null
    }

    fun validarTitular(titular: String): String? {
        val t = titular.trim()
        return when {
            t.isEmpty() -> "Ingresa el nombre del titular"
            !REGEX_TITULAR.matches(t) -> "Solo letras y espacios"
            t.length < 2 -> "El nombre es muy corto"
            else -> null
        }
    }

    /** Valida el formulario completo (la marca del número decide el largo del CVV). */
    fun validar(
        numero: String,
        vencimiento: String,
        cvv: String,
        titular: String,
        hoy: YearMonth,
    ): TarjetaErrores = TarjetaErrores(
        numero = validarNumero(numero),
        vencimiento = validarVencimiento(vencimiento, hoy),
        cvv = validarCvv(cvv, detectarMarca(numero)),
        titular = validarTitular(titular),
    )

    /** Últimos 4 dígitos: lo ÚNICO del número que se guarda. */
    fun ultimos4(numero: String): String = soloDigitos(numero).takeLast(4)
}
