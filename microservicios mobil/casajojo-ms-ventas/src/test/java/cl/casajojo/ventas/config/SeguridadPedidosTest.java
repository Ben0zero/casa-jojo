package cl.casajojo.ventas.config;

import cl.casajojo.ventas.controller.PedidoCasaJojoController;
import cl.casajojo.ventas.exception.GlobalExceptionHandler;
import cl.casajojo.ventas.repository.PedidoCasaJojoRepository;
import cl.casajojo.ventas.service.PedidoCasaJojoService;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import jakarta.servlet.Filter;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.MapPropertySource;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.web.MockServletContext;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.support.AnnotationConfigWebApplicationContext;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;

import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.util.Date;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * A1 + M1 con la cadena de seguridad REAL (SecurityConfig, JwtAuthenticationFilter,
 * CorsConfig) en un contexto web mínimo: sin Oracle y sin levantar el servidor.
 */
class SeguridadPedidosTest {

    private static final String SECRETO = "secreto-de-prueba-ventas-con-mas-de-32-caracteres";

    /** Repositorio simulado que comparte la configuración de prueba con cada test. */
    private static PedidoCasaJojoRepository repoCompartido;

    @Configuration
    @EnableWebMvc
    static class Web {
        @Bean
        PedidoCasaJojoService pedidoCasaJojoService() {
            return new PedidoCasaJojoService(repoCompartido, Clock.systemDefaultZone(), "COD");
        }
    }

    private AnnotationConfigWebApplicationContext ctx;
    private MockMvc mvc;
    private PedidoCasaJojoRepository repo;

    @BeforeEach
    void setUp() {
        repo = mock(PedidoCasaJojoRepository.class);
        repoCompartido = repo;
        when(repo.findByClienteCorreoOrderByFechaCreacionDesc(anyString())).thenReturn(List.of());
        when(repo.findByEstadoPedidoInOrderByFechaCreacionAsc(any())).thenReturn(List.of());

        ctx = new AnnotationConfigWebApplicationContext();
        ctx.setServletContext(new MockServletContext());
        ctx.getEnvironment().getPropertySources().addFirst(new MapPropertySource("prueba", Map.of(
                "jwt.secret", SECRETO,
                "casajojo.cors.origenes", "http://localhost:5173")));
        ctx.register(Web.class, SecurityConfig.class, CorsConfig.class, JwtAuthenticationFilter.class,
                PedidoCasaJojoController.class, GlobalExceptionHandler.class);
        ctx.refresh();

        mvc = MockMvcBuilders.webAppContextSetup(ctx)
                .addFilters(ctx.getBean("springSecurityFilterChain", Filter.class))
                .build();
    }

    @AfterEach
    void cerrar() {
        ctx.close();
    }

    private static String token(String correo, String rol, String secreto) {
        return "Bearer " + Jwts.builder()
                .setClaims(Map.of("rol", "ROLE_" + rol))
                .setSubject(correo)
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + 60_000))
                .signWith(Keys.hmacShaKeyFor(secreto.getBytes(StandardCharsets.UTF_8)), SignatureAlgorithm.HS256)
                .compact();
    }

    private static String token(String correo, String rol) {
        return token(correo, rol, SECRETO);
    }

    @Test
    @DisplayName("A1: mis-pedidos sin token → 401")
    void sinToken401() throws Exception {
        mvc.perform(get("/api/pedidos/mis-pedidos").param("correo", "camila@casajojo.cl"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("A1: token firmado con otro secreto → 401")
    void tokenAjeno401() throws Exception {
        mvc.perform(get("/api/pedidos/mis-pedidos").param("correo", "camila@casajojo.cl")
                        .header(HttpHeaders.AUTHORIZATION,
                                token("camila@casajojo.cl", "CLIENTE", "otro-secreto-cualquiera-de-mas-de-32-caracteres")))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("A1: mis-pedidos con el token del mismo cliente → 200; con el de otro → 403")
    void duenoDelPedido() throws Exception {
        mvc.perform(get("/api/pedidos/mis-pedidos").param("correo", "camila@casajojo.cl")
                        .header(HttpHeaders.AUTHORIZATION, token("camila@casajojo.cl", "CLIENTE")))
                .andExpect(status().isOk());

        mvc.perform(get("/api/pedidos/mis-pedidos").param("correo", "camila@casajojo.cl")
                        .header(HttpHeaders.AUTHORIZATION, token("otro@casajojo.cl", "CLIENTE")))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Cocina: GET /api/pedidos con CLIENTE → 403; con COCINA o ADMIN → 200")
    void endpointCocina() throws Exception {
        mvc.perform(get("/api/pedidos").header(HttpHeaders.AUTHORIZATION, token("camila@casajojo.cl", "CLIENTE")))
                .andExpect(status().isForbidden());
        mvc.perform(get("/api/pedidos").header(HttpHeaders.AUTHORIZATION, token("cocina@casajojo.cl", "COCINA")))
                .andExpect(status().isOk());
        mvc.perform(get("/api/pedidos").header(HttpHeaders.AUTHORIZATION, token("admin@casajojo.cl", "ADMIN")))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("A1: un CLIENTE no puede marcar pagado (solo caja) → 403")
    void pagarSoloCocina() throws Exception {
        mvc.perform(put("/api/pedidos/1/pagar").header(HttpHeaders.AUTHORIZATION, token("camila@casajojo.cl", "CLIENTE")))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("M1: preflight OPTIONS desde http://localhost:5173 → 200 con cabeceras CORS")
    void preflightCors() throws Exception {
        mvc.perform(options("/api/pedidos/mis-pedidos")
                        .header(HttpHeaders.ORIGIN, "http://localhost:5173")
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "PUT")
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_HEADERS, "Authorization, Content-Type"))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, "http://localhost:5173"));
    }

    @Test
    @DisplayName("M1: un origen no permitido no recibe cabeceras CORS (403)")
    void origenNoPermitido() throws Exception {
        mvc.perform(options("/api/pedidos/mis-pedidos")
                        .header(HttpHeaders.ORIGIN, "http://malicioso.example")
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "GET"))
                .andExpect(status().isForbidden());
    }
}
