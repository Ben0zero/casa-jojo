package cl.casajojo.ventas.controller;

import cl.casajojo.ventas.exception.GlobalExceptionHandler;
import cl.casajojo.ventas.model.PedidoCasaJojo;
import cl.casajojo.ventas.repository.PedidoCasaJojoRepository;
import cl.casajojo.ventas.service.PedidoCasaJojoService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.Clock;
import java.util.List;
import java.util.Optional;

import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * M3: los errores del cliente responden 400 (antes caían en el catch-all con 500)
 * y un 500 ya no expone el mensaje interno. MockMvc standalone: sin Oracle.
 */
class PedidoCasaJojoControllerTest {

    private PedidoCasaJojoRepository repo;
    private MockMvc mvc;

    /** A1: las rutas exigen un usuario autenticado; aquí lo simulamos con un cliente. */
    private final UsernamePasswordAuthenticationToken cliente = new UsernamePasswordAuthenticationToken(
            "x@x.cl", null, List.of(new SimpleGrantedAuthority("ROLE_CLIENTE")));

    @BeforeEach
    void setUp() {
        repo = mock(PedidoCasaJojoRepository.class);
        mvc = MockMvcBuilders
                .standaloneSetup(new PedidoCasaJojoController(new PedidoCasaJojoService(repo, Clock.systemDefaultZone(), "COD")))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    @DisplayName("M3: estado=FOO → 400")
    void estadoInvalido400() throws Exception {
        PedidoCasaJojo pedido = new PedidoCasaJojo();
        pedido.setId(1L);
        when(repo.findById(1L)).thenReturn(Optional.of(pedido));

        mvc.perform(put("/api/pedidos/1/estado").param("estado", "FOO").principal(cliente))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Estado de pedido inválido: FOO"));
    }

    @Test
    @DisplayName("M3: falta ?correo → 400")
    void faltaCorreo400() throws Exception {
        mvc.perform(get("/api/pedidos/ultimo"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Falta el parámetro obligatorio 'correo'"));
    }

    @Test
    @DisplayName("M3: id no numérico → 400")
    void idNoNumerico400() throws Exception {
        mvc.perform(put("/api/pedidos/abc/pagar"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("M3: JSON mal formado → 400")
    void jsonMalFormado400() throws Exception {
        mvc.perform(post("/api/pedidos").contentType(MediaType.APPLICATION_JSON).content("{malo"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Pedido inexistente → 404 (se mantiene)")
    void pedidoInexistente404() throws Exception {
        when(repo.findById(99L)).thenReturn(Optional.empty());

        mvc.perform(put("/api/pedidos/99/pagar"))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("M3: un 500 no expone el mensaje interno")
    void error500SinDetalles() throws Exception {
        // Error genérico (no tiene handler propio) que simula una falla de base de datos.
        when(repo.findByClienteCorreoOrderByFechaCreacionDesc(anyString()))
                .thenThrow(new UnsupportedOperationException("ORA-00942: la tabla no existe"));

        mvc.perform(get("/api/pedidos/mis-pedidos").param("correo", "x@x.cl").principal(cliente))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.message").value("Error interno en ms-ventas"))
                .andExpect(jsonPath("$.message").value(not(containsString("ORA-"))));
    }
}
