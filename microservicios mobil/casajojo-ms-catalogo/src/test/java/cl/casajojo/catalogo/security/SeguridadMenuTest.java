package cl.casajojo.catalogo.security;

import cl.casajojo.catalogo.config.CorsConfig;
import cl.casajojo.catalogo.controller.MenuController;
import cl.casajojo.catalogo.exception.GlobalExceptionHandler;
import cl.casajojo.catalogo.model.Producto;
import cl.casajojo.catalogo.repository.ProductoRepository;
import cl.casajojo.catalogo.security.jwt.JwtAuthenticationFilter;
import cl.casajojo.catalogo.security.jwt.JwtUtil;
import cl.casajojo.catalogo.service.MenuService;
import cl.casajojo.catalogo.service.impl.MenuServiceImpl;
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
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockServletContext;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.support.AnnotationConfigWebApplicationContext;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;

import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * PUT /api/menu/{id}/disponible (solo ADMIN o COCINA) y CORS, con la cadena de
 * seguridad real del catálogo en un contexto mínimo (sin Oracle).
 */
class SeguridadMenuTest {

    private static final String SECRETO = "secreto-de-prueba-catalogo-con-mas-de-32-caracteres";
    private static ProductoRepository repoCompartido;

    @Configuration
    @EnableWebMvc
    static class Web {
        @Bean
        MenuService menuService() {
            return new MenuServiceImpl(repoCompartido);
        }
    }

    private AnnotationConfigWebApplicationContext ctx;
    private MockMvc mvc;
    private Producto pollo;

    @BeforeEach
    void setUp() {
        ProductoRepository repo = mock(ProductoRepository.class);
        repoCompartido = repo;
        pollo = new Producto();
        pollo.setId(15L);
        pollo.setSku("CJ-PROT-01");
        pollo.setNombre("Pollo asado");
        pollo.setPrecio(4800);
        pollo.setCategoria("PROTEINA");
        pollo.setActivo(true);
        when(repo.findById(15L)).thenReturn(Optional.of(pollo));
        when(repo.save(any(Producto.class))).thenAnswer(inv -> inv.getArgument(0));

        ctx = new AnnotationConfigWebApplicationContext();
        ctx.setServletContext(new MockServletContext());
        ctx.getEnvironment().getPropertySources().addFirst(new MapPropertySource("prueba", Map.of(
                "jwt.secret", SECRETO,
                "casajojo.cors.origenes", "http://localhost:5173")));
        ctx.register(Web.class, SecurityConfig.class, CorsConfig.class, JwtAuthenticationFilter.class, JwtUtil.class,
                CustomAuthenticationEntryPoint.class, MenuController.class, GlobalExceptionHandler.class);
        ctx.refresh();
        mvc = MockMvcBuilders.webAppContextSetup(ctx)
                .addFilters(ctx.getBean("springSecurityFilterChain", Filter.class))
                .build();
    }

    @AfterEach
    void cerrar() {
        ctx.close();
    }

    private static String token(String rol) {
        return "Bearer " + Jwts.builder()
                .setClaims(Map.of("rol", "ROLE_" + rol))
                .setSubject(rol.toLowerCase() + "@casajojo.cl")
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + 60_000))
                .signWith(Keys.hmacShaKeyFor(SECRETO.getBytes(StandardCharsets.UTF_8)), SignatureAlgorithm.HS256)
                .compact();
    }

    private static final String AGOTADO = "{\"disponible\": false}";

    @Test
    @DisplayName("El menú sigue siendo público")
    void menuPublico() throws Exception {
        mvc.perform(get("/api/menu/15")).andExpect(status().isOk());
    }

    @Test
    @DisplayName("PUT disponible sin token → 401; con CLIENTE → 403")
    void disponibleSinPermiso() throws Exception {
        mvc.perform(put("/api/menu/15/disponible").contentType(MediaType.APPLICATION_JSON).content(AGOTADO))
                .andExpect(status().isUnauthorized());
        mvc.perform(put("/api/menu/15/disponible").contentType(MediaType.APPLICATION_JSON).content(AGOTADO)
                        .header(HttpHeaders.AUTHORIZATION, token("CLIENTE")))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("PUT disponible con COCINA → 200 y el plato queda agotado")
    void cocinaMarcaAgotado() throws Exception {
        mvc.perform(put("/api/menu/15/disponible").contentType(MediaType.APPLICATION_JSON).content(AGOTADO)
                        .header(HttpHeaders.AUTHORIZATION, token("COCINA")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.disponible").value(false));
        assertFalse(pollo.getActivo());
    }

    @Test
    @DisplayName("PUT disponible con ADMIN sobre un plato inexistente → 404; sin body válido → 400")
    void adminCasosDeError() throws Exception {
        mvc.perform(put("/api/menu/999/disponible").contentType(MediaType.APPLICATION_JSON).content(AGOTADO)
                        .header(HttpHeaders.AUTHORIZATION, token("ADMIN")))
                .andExpect(status().isNotFound());
        mvc.perform(put("/api/menu/15/disponible").contentType(MediaType.APPLICATION_JSON).content("{}")
                        .header(HttpHeaders.AUTHORIZATION, token("ADMIN")))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("M1: preflight OPTIONS desde la web → 200 con cabeceras CORS")
    void preflight() throws Exception {
        mvc.perform(options("/api/menu/15/disponible")
                        .header(HttpHeaders.ORIGIN, "http://localhost:5173")
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "PUT")
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_HEADERS, "Authorization, Content-Type"))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, "http://localhost:5173"));
    }
}
