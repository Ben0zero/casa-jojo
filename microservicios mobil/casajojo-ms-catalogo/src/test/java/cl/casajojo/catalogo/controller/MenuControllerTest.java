package cl.casajojo.catalogo.controller;

import cl.casajojo.catalogo.exception.GlobalExceptionHandler;
import cl.casajojo.catalogo.model.Producto;
import cl.casajojo.catalogo.repository.ProductoRepository;
import cl.casajojo.catalogo.service.impl.MenuServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.Optional;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** B1: GET /api/menu/{id} responde 404 cuando el plato no existe o no es del menú. */
class MenuControllerTest {

    private ProductoRepository repo;
    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        repo = mock(ProductoRepository.class);
        mvc = MockMvcBuilders.standaloneSetup(new MenuController(new MenuServiceImpl(repo)))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    @DisplayName("B1: id inexistente → 404")
    void idInexistente404() throws Exception {
        when(repo.findById(999L)).thenReturn(Optional.empty());

        mvc.perform(get("/api/menu/999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.mensaje").value("Plato no encontrado: 999"));
    }

    @Test
    @DisplayName("B1: producto ajeno al menú (sin prefijo CJ-) → 404")
    void productoDeGas404() throws Exception {
        Producto ajeno = new Producto();
        ajeno.setId(1L);
        ajeno.setSku("OTRO-005");
        when(repo.findById(1L)).thenReturn(Optional.of(ajeno));

        mvc.perform(get("/api/menu/1")).andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Plato del menú → 200")
    void platoDelMenu200() throws Exception {
        Producto pollo = new Producto();
        pollo.setId(15L);
        pollo.setSku("CJ-PROT-01");
        pollo.setNombre("Pollo asado");
        pollo.setPrecio(4800);
        pollo.setCategoria("PROTEINA");
        pollo.setActivo(true);
        when(repo.findById(15L)).thenReturn(Optional.of(pollo));

        mvc.perform(get("/api/menu/15"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tipo").value("proteina"));
    }
}
