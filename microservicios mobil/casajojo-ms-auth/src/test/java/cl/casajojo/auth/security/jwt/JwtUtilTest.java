package cl.casajojo.auth.security.jwt;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/** A7: el secreto llega por configuración (JWT_SECRET) y se valida su largo. */
class JwtUtilTest {

    static final String SECRETO = "secreto-de-prueba-con-mas-de-32-caracteres-123";

    @Test
    @DisplayName("A7: un secreto vacío o corto impide crear JwtUtil")
    void secretoCortoFalla() {
        assertThrows(IllegalStateException.class, () -> new JwtUtil("corto"));
        assertThrows(IllegalStateException.class, () -> new JwtUtil(null));
    }

    @Test
    @DisplayName("A7: el token generado con el secreto configurado es válido")
    void tokenGeneradoEsValido() {
        JwtUtil util = new JwtUtil(SECRETO);
        String token = util.generateToken("cliente@casajojo.cl", "CLIENTE");

        UserDetails usuario = new User("cliente@casajojo.cl", "x", List.of());
        assertEquals("cliente@casajojo.cl", util.extractUsername(token));
        assertTrue(util.isTokenValid(token, usuario));
    }

    @Test
    @DisplayName("A7: un token firmado con OTRO secreto no se acepta")
    void tokenDeOtroSecretoSeRechaza() {
        String ajeno = new JwtUtil("otro-secreto-distinto-con-mas-de-32-caracteres")
                .generateToken("admin@casajojo.cl", "ADMIN");

        JwtUtil util = new JwtUtil(SECRETO);
        assertThrows(io.jsonwebtoken.JwtException.class, () -> util.extractUsername(ajeno));
    }
}
