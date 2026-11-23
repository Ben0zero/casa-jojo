package cl.casajojo.catalogo.service.impl;

import cl.casajojo.catalogo.dto.PlatoMenuDTO;
import cl.casajojo.catalogo.model.Producto;
import cl.casajojo.catalogo.repository.ProductoRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

/** Pruebas del menú Casa JoJo (solo SKU CJ-*). */
@ExtendWith(MockitoExtension.class)
class MenuServiceImplTest {

    @Mock
    private ProductoRepository productoRepository;

    @InjectMocks
    private MenuServiceImpl menuService;

    private Producto producto(Long id, String sku, String nombre, Integer precio,
                              String categoria, Boolean activo) {
        Producto p = new Producto();
        p.setId(id);
        p.setSku(sku);
        p.setNombre(nombre);
        p.setDescripcion(nombre + " (Casa JoJo)");
        p.setPrecio(precio);
        p.setPrecioResidencial(precio);
        p.setCategoria(categoria);
        p.setActivo(activo);
        return p;
    }

    @Test
    @DisplayName("obtenerMenu solo expone los SKU CJ-* y traduce tipo y disponibilidad")
    void obtenerMenuSoloExposePlatosCasaJojo() {
        when(productoRepository.findAll()).thenReturn(List.of(
                producto(1L, "CJ-PROT-01", "Pollo asado", 4800, "PROTEINA", true),
                producto(7L, "CJ-ACOM-01", "Arroz", 0, "ACOMPANAMIENTO", false),
                producto(99L, "OTRO-005", "Producto ajeno", 15990, "OTRO", true)
        ));

        List<PlatoMenuDTO> menu = menuService.obtenerMenu();

        assertEquals(2, menu.size());
        assertEquals("CJ-PROT-01", menu.get(0).getSku());
        assertEquals("proteina", menu.get(0).getTipo());
        assertEquals(4800, menu.get(0).getPrecio());
        assertTrue(menu.get(0).getDisponible());
        assertEquals("acompanamiento", menu.get(1).getTipo());
        assertFalse(menu.get(1).getDisponible());
    }

    @Test
    @DisplayName("obtenerPorId rechaza productos que no pertenecen al menú")
    void obtenerPorIdRechazaProductoAjenoAlMenu() {
        when(productoRepository.findById(99L))
                .thenReturn(Optional.of(producto(99L, "OTRO-005", "Producto ajeno", 15990, "OTRO", true)));

        assertThrows(IllegalArgumentException.class, () -> menuService.obtenerPorId(99L));
    }

    @Test
    @DisplayName("obtenerPorId devuelve un plato del menú con precio y tipo correctos")
    void obtenerPorIdDevuelvePlato() {
        when(productoRepository.findById(1L))
                .thenReturn(Optional.of(producto(1L, "CJ-PROT-01", "Pollo asado", 4800, "PROTEINA", true)));

        PlatoMenuDTO plato = menuService.obtenerPorId(1L);

        assertEquals("Pollo asado", plato.getNombre());
        assertEquals("proteina", plato.getTipo());
        assertEquals(4800, plato.getPrecio());
    }
}
