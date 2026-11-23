package cl.casajojo.auth.security;

import cl.casajojo.auth.security.jwt.JwtUtil;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.nio.charset.StandardCharsets;
import java.util.Date;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/** B12: un token malo nunca debe convertirse en un 500; la petición sigue sin autenticar. */
class JwtAuthenticationFilterTest {

    private static final String SECRETO = "secreto-de-prueba-con-mas-de-32-caracteres-123";

    private final UserDetailsService usuarios = mock(UserDetailsService.class);
    private final JwtAuthenticationFilter filtro =
            new JwtAuthenticationFilter(new JwtUtil(SECRETO), usuarios);

    @AfterEach
    void limpiar() {
        SecurityContextHolder.clearContext();
    }

    private MockFilterChain ejecutar(String token) throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/auth/login");
        request.addHeader("Authorization", "Bearer " + token);
        MockFilterChain cadena = new MockFilterChain();
        filtro.doFilter(request, new MockHttpServletResponse(), cadena);
        return cadena;
    }

    @Test
    @DisplayName("B12: token vencido → no lanza excepción y la cadena continúa")
    void tokenVencidoNoRompe() throws Exception {
        String vencido = Jwts.builder()
                .setSubject("cliente@casajojo.cl")
                .setIssuedAt(new Date(System.currentTimeMillis() - 120_000))
                .setExpiration(new Date(System.currentTimeMillis() - 60_000))
                .signWith(Keys.hmacShaKeyFor(SECRETO.getBytes(StandardCharsets.UTF_8)), SignatureAlgorithm.HS256)
                .compact();

        MockFilterChain cadena = assertDoesNotThrow(() -> ejecutar(vencido));
        assertNotNull(cadena.getRequest(), "la petición debe seguir su curso");
        assertNull(SecurityContextHolder.getContext().getAuthentication());
    }

    @Test
    @DisplayName("B12: token alterado o basura → no lanza excepción")
    void tokenBasuraNoRompe() {
        MockFilterChain cadena = assertDoesNotThrow(() -> ejecutar("esto.no.es-un-jwt"));
        assertNotNull(cadena.getRequest());
        assertNull(SecurityContextHolder.getContext().getAuthentication());
    }

    @Test
    @DisplayName("B12: token válido de un usuario que ya no existe → no lanza excepción")
    void usuarioInexistenteNoRompe() {
        String token = new JwtUtil(SECRETO).generateToken("borrado@casajojo.cl", "CLIENTE");
        when(usuarios.loadUserByUsername(anyString())).thenThrow(new UsernameNotFoundException("no existe"));

        MockFilterChain cadena = assertDoesNotThrow(() -> ejecutar(token));
        assertNotNull(cadena.getRequest());
        assertNull(SecurityContextHolder.getContext().getAuthentication());
    }
}
