package cl.duoc.casajojo.domain

import java.security.MessageDigest
import java.security.SecureRandom
import java.util.Base64
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

/**
 * Cifrado de contraseñas (RNF-04: "las contraseñas deben almacenarse cifradas").
 *
 * Usamos PBKDF2 con HMAC-SHA256, que viene incluido en Android (API 26+) y en Java,
 * así que no necesita librerías externas.
 *
 * Cómo funciona:
 * 1. Al registrarse se genera un "salt": 16 bytes aleatorios, distintos por usuario.
 * 2. hash(contraseña, salt) aplica SHA-256 miles de veces (ITERACIONES).
 *    Es lento a propósito: dificulta probar millones de contraseñas por fuerza bruta.
 * 3. En la base se guardan el hash y el salt, NUNCA la contraseña.
 * 4. Al iniciar sesión se vuelve a calcular el hash con el mismo salt y se compara.
 *
 * Gracias al salt, dos usuarios con la misma contraseña tienen hashes distintos.
 */
object Contrasenas {

    private const val ALGORITMO = "PBKDF2WithHmacSHA256"

    // Más iteraciones = más seguro pero más lento. 60.000 tarda una fracción de
    // segundo en un teléfono; para un servidor real se recomiendan valores mayores.
    private const val ITERACIONES = 60_000
    private const val LARGO_BITS = 256
    private const val LARGO_SALT_BYTES = 16

    /** Genera un salt aleatorio y lo devuelve como texto Base64 para guardarlo en Room. */
    fun generarSalt(): String {
        val bytes = ByteArray(LARGO_SALT_BYTES)
        SecureRandom().nextBytes(bytes) // SecureRandom: aleatorio apto para seguridad
        return Base64.getEncoder().encodeToString(bytes)
    }

    /** Calcula el hash de la contraseña con el salt dado. */
    fun hash(password: String, salt: String): String {
        val spec = PBEKeySpec(
            password.toCharArray(),
            Base64.getDecoder().decode(salt),
            ITERACIONES,
            LARGO_BITS,
        )
        try {
            val bytes = SecretKeyFactory.getInstance(ALGORITMO).generateSecret(spec).encoded
            return Base64.getEncoder().encodeToString(bytes)
        } finally {
            spec.clearPassword() // borra la contraseña de la memoria apenas se usa
        }
    }

    /**
     * true si la contraseña escrita corresponde al hash guardado.
     * MessageDigest.isEqual compara en tiempo constante (no "corta" al primer
     * carácter distinto), lo que evita filtrar información por el tiempo de respuesta.
     */
    fun verificar(password: String, salt: String, hashGuardado: String): Boolean =
        MessageDigest.isEqual(
            hash(password, salt).toByteArray(),
            hashGuardado.toByteArray(),
        )
}