package cl.elvolcan.inventario.service;

import cl.elvolcan.inventario.dto.request.StockRequestDTO;
import cl.elvolcan.inventario.dto.response.StockResponseDTO;
import cl.elvolcan.inventario.exception.InsufficientStockException;
import cl.elvolcan.inventario.exception.OverstockException;
import cl.elvolcan.inventario.exception.ResourceNotFoundException;
import cl.elvolcan.inventario.model.Inventario;
import cl.elvolcan.inventario.repository.InventarioRepository;
import cl.elvolcan.inventario.service.impl.InventarioServiceImpl;
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
class InventarioServiceTest {

    @Mock
    private InventarioRepository repository;

    @InjectMocks
    private InventarioServiceImpl service;

    @Test
    @DisplayName("crearProducto debe guardar producto exitosamente")
    void crearProductoDebeGuardarProducto() {
        // Given
        StockRequestDTO request = new StockRequestDTO();
        request.setSku("PAN-001");
        request.setStockActual(100.0);
        request.setNivelCritico(10.0);
        request.setNivelMaximo(500.0);

        when(repository.existsBySku("PAN-001")).thenReturn(false);

        // When
        StockResponseDTO resultado = service.crearProducto(request);

        // Then
        assertNotNull(resultado);
        assertEquals("PAN-001", resultado.getSku());
        verify(repository).save(any(Inventario.class));
    }

    @Test
    @DisplayName("crearProducto debe lanzar excepción si el SKU ya existe")
    void crearProductoDebeLanzarExcepcionSiExiste() {
        // Given
        StockRequestDTO request = new StockRequestDTO();
        request.setSku("PAN-001");

        when(repository.existsBySku("PAN-001")).thenReturn(true);

        // When + Then
        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> service.crearProducto(request)
        );
        assertEquals("El producto ya existe", exception.getMessage());
        verify(repository, never()).save(any(Inventario.class));
    }

    @Test
    @DisplayName("aumentarStock debe sumar cantidad correctamente")
    void aumentarStockDebeSumarCantidad() {
        // Given
        StockRequestDTO request = new StockRequestDTO();
        request.setSku("PAN-001");
        request.setStockActual(20.0); // Cantidad a sumar

        Inventario inventarioExistente = new Inventario();
        inventarioExistente.setSku("PAN-001");
        inventarioExistente.setStockActual(50.0);
        inventarioExistente.setNivelMaximo(100.0);
        inventarioExistente.setNivelCritico(10.0);

        when(repository.findBySku("PAN-001")).thenReturn(Optional.of(inventarioExistente));

        // When
        StockResponseDTO resultado = service.aumentarStock(request);

        // Then
        assertEquals(70.0, resultado.getStockActual());
        verify(repository).save(inventarioExistente);
    }

    @Test
    @DisplayName("aumentarStock debe lanzar OverstockException si supera el máximo")
    void aumentarStockDebeLanzarExcepcionSiSuperaMaximo() {
        // Given
        StockRequestDTO request = new StockRequestDTO();
        request.setSku("PAN-001");
        request.setStockActual(60.0); // 50 + 60 = 110 (supera el max de 100)

        Inventario inventarioExistente = new Inventario();
        inventarioExistente.setSku("PAN-001");
        inventarioExistente.setStockActual(50.0);
        inventarioExistente.setNivelMaximo(100.0);

        when(repository.findBySku("PAN-001")).thenReturn(Optional.of(inventarioExistente));

        // When + Then
        assertThrows(
                OverstockException.class,
                () -> service.aumentarStock(request)
        );
        verify(repository, never()).save(any(Inventario.class));
    }

    @Test
    @DisplayName("disminuirStock debe restar cantidad correctamente")
    void disminuirStockDebeRestarCantidad() {
        // Given
        StockRequestDTO request = new StockRequestDTO();
        request.setSku("PAN-001");
        request.setStockActual(20.0); // Cantidad a restar

        Inventario inventarioExistente = new Inventario();
        inventarioExistente.setSku("PAN-001");
        inventarioExistente.setStockActual(50.0);
        inventarioExistente.setNivelCritico(10.0);

        when(repository.findBySku("PAN-001")).thenReturn(Optional.of(inventarioExistente));

        // When
        StockResponseDTO resultado = service.disminuirStock(request);

        // Then
        assertEquals(30.0, resultado.getStockActual());
        assertFalse(resultado.getBajoStock());
        verify(repository).save(inventarioExistente);
    }

    @Test
    @DisplayName("disminuirStock debe lanzar InsufficientStockException si no hay stock")
    void disminuirStockDebeLanzarExcepcionSiNoHayStock() {
        // Given
        StockRequestDTO request = new StockRequestDTO();
        request.setSku("PAN-001");
        request.setStockActual(60.0); // Intenta sacar 60 pero solo hay 50

        Inventario inventarioExistente = new Inventario();
        inventarioExistente.setSku("PAN-001");
        inventarioExistente.setStockActual(50.0);

        when(repository.findBySku("PAN-001")).thenReturn(Optional.of(inventarioExistente));

        // When + Then
        assertThrows(
                InsufficientStockException.class,
                () -> service.disminuirStock(request)
        );
        verify(repository, never()).save(any(Inventario.class));
    }

    @Test
    @DisplayName("obtenerPorSku debe retornar producto cuando existe")
    void obtenerPorSkuDebeRetornarProductoCuandoExiste() {
        // Given
        Inventario producto = new Inventario();
        producto.setSku("PAN-001");
        producto.setStockActual(100.0);
        producto.setNivelCritico(10.0);

        when(repository.findBySku("PAN-001")).thenReturn(Optional.of(producto));

        // When
        StockResponseDTO resultado = service.obtenerPorSku("PAN-001");

        // Then
        assertNotNull(resultado, "El producto retornado es nulo");
        assertEquals("PAN-001", resultado.getSku());
        verify(repository).findBySku("PAN-001");
    }

    @Test
    @DisplayName("obtenerPorSku debe lanzar ResourceNotFoundException cuando no existe")
    void obtenerPorSkuDebeLanzarExcepcionCuandoNoExiste() {
        // Given
        when(repository.findBySku("XXX")).thenReturn(Optional.empty());

        // When + Then
        assertThrows(
                ResourceNotFoundException.class,
                () -> service.obtenerPorSku("XXX")
        );
        verify(repository).findBySku("XXX");
    }

    @Test
    @DisplayName("obtenerTodos debe retornar lista completa")
    void obtenerTodosDebeRetornarListaDeProductos() {
        // Given
        Inventario producto1 = new Inventario();
        producto1.setSku("PAN-001");
        producto1.setStockActual(100.0);
        producto1.setNivelCritico(10.0);

        Inventario producto2 = new Inventario();
        producto2.setSku("PAN-002");
        producto2.setStockActual(50.0);
        producto2.setNivelCritico(5.0);

        when(repository.findAll()).thenReturn(List.of(producto1, producto2));

        // When
        List<StockResponseDTO> resultado = service.obtenerTodos();

        // Then
        assertEquals(2, resultado.size());
        verify(repository).findAll();
    }
}