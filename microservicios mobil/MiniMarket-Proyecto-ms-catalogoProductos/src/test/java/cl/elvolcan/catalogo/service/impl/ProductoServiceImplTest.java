package cl.elvolcan.catalogo.service.impl;

import cl.elvolcan.catalogo.client.InventarioClient;
import cl.elvolcan.catalogo.dto.InventarioInitDTO;
import cl.elvolcan.catalogo.dto.ProductoRequestDTO;
import cl.elvolcan.catalogo.dto.ProductoResponseDTO;
import cl.elvolcan.catalogo.model.Producto;
import cl.elvolcan.catalogo.repository.ProductoRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProductoServiceImplTest {

    @Mock
    private ProductoRepository productoRepository;

    @Mock
    private InventarioClient inventarioClient;

    @InjectMocks
    private ProductoServiceImpl productoService;

    @Test
    @DisplayName("crearProducto debe guardar el producto y notificar a Inventario")
    void crearProductoDebeGuardarYNotificarInventario() {
        // Given: Preparamos los datos simulados
        ProductoRequestDTO request = new ProductoRequestDTO();
        request.setSku("PAN-001");
        request.setNombre("Pan Batido");
        request.setPrecio(1800);
        request.setCategoria("Panadería");

        Producto productoGuardado = new Producto();
        productoGuardado.setSku("PAN-001");
        productoGuardado.setNombre("Pan Batido");
        productoGuardado.setPrecio(1800);
        productoGuardado.setCategoria("Panadería");
        productoGuardado.setActivo(true);

        when(productoRepository.existsBySku("PAN-001")).thenReturn(false);
        when(productoRepository.save(any(Producto.class))).thenReturn(productoGuardado);

        // When: Ejecutamos el método
        ProductoResponseDTO resultado = productoService.crearProducto(request);

        // Then: Verificamos resultados y que se llamaron a los métodos correctos
        assertNotNull(resultado);
        assertEquals("PAN-001", resultado.getSku());
        assertEquals("Pan Batido", resultado.getNombre());
        assertTrue(resultado.getActivo());

        verify(productoRepository).existsBySku("PAN-001");
        verify(productoRepository).save(any(Producto.class));
        verify(inventarioClient).crearInventario(any(InventarioInitDTO.class));
    }

    @Test
    @DisplayName("crearProducto debe lanzar excepción si el SKU ya existe")
    void crearProductoDebeLanzarExcepcionSiSkuExiste() {
        // Given
        ProductoRequestDTO request = new ProductoRequestDTO();
        request.setSku("PAN-001");

        when(productoRepository.existsBySku("PAN-001")).thenReturn(true);

        // When + Then
        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> productoService.crearProducto(request)
        );

        assertEquals("Error: Ya existe un producto con el SKU: PAN-001", exception.getMessage());
        
        // Verificamos que NO se guardó en BD ni se llamó al cliente Feign
        verify(productoRepository, never()).save(any(Producto.class));
        verify(inventarioClient, never()).crearInventario(any(InventarioInitDTO.class));
    }

    @Test
    @DisplayName("obtenerPorSku debe retornar el DTO cuando el producto existe")
    void obtenerPorSkuDebeRetornarDtoCuandoExiste() {
        // Given
        Producto producto = new Producto();
        producto.setSku("LAC-001");
        producto.setNombre("Leche Entera");
        producto.setActivo(true);

        when(productoRepository.findBySku("LAC-001")).thenReturn(Optional.of(producto));

        // When
        ProductoResponseDTO resultado = productoService.obtenerPorSku("LAC-001");

        // Then
        assertNotNull(resultado);
        assertEquals("LAC-001", resultado.getSku());
        assertEquals("Leche Entera", resultado.getNombre());

        verify(productoRepository).findBySku("LAC-001");
    }

    @Test
    @DisplayName("obtenerPorSku debe lanzar excepción cuando no existe")
    void obtenerPorSkuDebeLanzarExcepcionCuandoNoExiste() {
        // Given
        when(productoRepository.findBySku("INVENTADO-999")).thenReturn(Optional.empty());

        // When + Then
        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> productoService.obtenerPorSku("INVENTADO-999")
        );

        assertEquals("Producto no encontrado con SKU: INVENTADO-999", exception.getMessage());
        verify(productoRepository).findBySku("INVENTADO-999");
    }

    @Test
    @DisplayName("obtenerTodosActivos debe retornar lista de productos en vitrina")
    void obtenerTodosActivosDebeRetornarLista() {
        // Given
        Producto p1 = new Producto();
        p1.setSku("PAN-001");
        p1.setActivo(true);

        Producto p2 = new Producto();
        p2.setSku("PAN-002");
        p2.setActivo(true);

        when(productoRepository.findByActivoTrue()).thenReturn(List.of(p1, p2));

        // When
        List<ProductoResponseDTO> resultado = productoService.obtenerTodosActivos();

        // Then
        assertEquals(2, resultado.size());
        verify(productoRepository).findByActivoTrue();
    }

    @Test
    @DisplayName("desactivarProducto debe cambiar el estado a false y guardar")
    void desactivarProductoDebeCambiarEstadoYGuardar() {
        // Given
        Producto producto = new Producto();
        producto.setSku("PAN-001");
        producto.setActivo(true);

        when(productoRepository.findBySku("PAN-001")).thenReturn(Optional.of(producto));

        // When
        productoService.desactivarProducto("PAN-001");

        // Then
        assertFalse(producto.getActivo()); // Comprobamos que cambió el estado
        verify(productoRepository).findBySku("PAN-001");
        verify(productoRepository).save(producto); // Comprobamos que se guardó la actualización
    }
}