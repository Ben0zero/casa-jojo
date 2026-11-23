package cl.casajojo.auth.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/** M1: la web (Vite, puerto 5173) puede llamar a /api/auth/** con JSON y token. */
class CorsConfigTest {

    private final CorsConfigurationSource fuente =
            new CorsConfig().corsConfigurationSource("http://localhost:5173, http://127.0.0.1:5173");

    @Test
    @DisplayName("M1: orígenes, métodos y cabeceras permitidos")
    void configuracionCors() {
        CorsConfiguration c = fuente.getCorsConfiguration(new MockHttpServletRequest("POST", "/api/auth/login"));

        assertNotNull(c);
        assertEquals(List.of("http://localhost:5173", "http://127.0.0.1:5173"), c.getAllowedOrigins());
        assertEquals(List.of("GET", "POST", "PUT", "OPTIONS"), c.getAllowedMethods());
        assertEquals(List.of("Authorization", "Content-Type"), c.getAllowedHeaders());
        assertNull(c.checkOrigin("http://malicioso.example"));
    }
}
