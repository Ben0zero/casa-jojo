package cl.elvolcan.ventas.service;

import cl.elvolcan.ventas.assembler.VentaModelAssembler;
import cl.elvolcan.ventas.client.CatalogoClient;
import cl.elvolcan.ventas.client.InventarioClient;
import cl.elvolcan.ventas.dto.*;
import cl.elvolcan.ventas.exception.InsufficientStockException;
import cl.elvolcan.ventas.exception.ResourceNotFoundException;
import cl.elvolcan.ventas.model.DetalleVenta;
import cl.elvolcan.ventas.model.Venta;
import cl.elvolcan.ventas.repository.VentaRepository;
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
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class VentaServiceTest {

    @Mock
    private VentaRepository ventaRepository;

    @Mock
    private CatalogoClient catalogoClient;

    @Mock
    private InventarioClient inventarioClient;

    @Mock
    private VentaModelAssembler ventaModelAssembler;

    @InjectMocks
    private VentaService ventaService;

    @Test
    @DisplayName("crearPedido debe guardar venta de abarrotes correctamente")
    void crearPedidoDebeGuardarVentaAbarrotes() {
        // Given
        DetalleVentaRequestDTO detalleDTO = new DetalleVentaRequestDTO();
        detalleDTO.setProductoId(10L);
        detalleDTO.setCantidad(2);

        VentaRequestDTO request = new VentaRequestDTO();
        request.setDetalles(List.of(detalleDTO));

        ProductoDTO productoMock = new ProductoDTO();
        productoMock.setId(10L);
        productoMock.setSku("PROD-123");
        productoMock.setNombre("Bebida");
        productoMock.setPrecio(1500);

        StockResponseDTO stockMock = new StockResponseDTO();
        stockMock.setStockActual(10.0); // Hay stock suficiente

        Venta ventaGuardada = new Venta();
        ventaGuardada.setId(1L);
        ventaGuardada.setTotal(3000.0);

        VentaResponseDTO responseEsperado = new VentaResponseDTO();
        responseEsperado.setId(1L);
        responseEsperado.setTotal(3000.0);

        // Mocks de clientes y repositorio
        when(catalogoClient.obtenerProductoPorId(anyString(), eq(10L))).thenReturn(productoMock);
        when(inventarioClient.obtenerStock(anyString(), eq("PROD-123"))).thenReturn(stockMock);
        when(ventaRepository.save(any(Venta.class))).thenReturn(ventaGuardada);
        when(ventaModelAssembler.toModel(ventaGuardada)).thenReturn(responseEsperado);

        // When
        VentaResponseDTO resultado = ventaService.crearPedido(request);

        // Then
        assertNotNull(resultado);
        assertEquals(3000.0, resultado.getTotal());
        verify(ventaRepository).save(any(Venta.class));
    }

    @Test
    @DisplayName("crearPedido debe lanzar InsufficientStockException si no hay stock")
    void crearPedidoDebeLanzarExcepcionSinStock() {
        // Given
        DetalleVentaRequestDTO detalleDTO = new DetalleVentaRequestDTO();
        detalleDTO.setProductoId(10L);
        detalleDTO.setCantidad(5); // Pide 5

        VentaRequestDTO request = new VentaRequestDTO();
        request.setDetalles(List.of(detalleDTO));

        ProductoDTO productoMock = new ProductoDTO();
        productoMock.setId(10L);
        productoMock.setSku("PROD-123");
        productoMock.setPrecio(1000);

        StockResponseDTO stockMock = new StockResponseDTO();
        stockMock.setStockActual(2.0); // Solo hay 2 en inventario

        when(catalogoClient.obtenerProductoPorId(anyString(), eq(10L))).thenReturn(productoMock);
        when(inventarioClient.obtenerStock(anyString(), eq("PROD-123"))).thenReturn(stockMock);

        // When + Then
        assertThrows(InsufficientStockException.class, () -> ventaService.crearPedido(request));
        verify(ventaRepository, never()).save(any(Venta.class)); // Nunca debe guardar
    }

    @Test
    @DisplayName("confirmarPago debe cambiar estado a PAGADA y descontar stock")
    void confirmarPagoDebeCambiarEstadoYDescontarStock() {
        // Given
        Long idVenta = 1L;
        Venta venta = new Venta();
        venta.setId(idVenta);
        venta.setEstado(Venta.EstadoVenta.PENDIENTE_PAGO);

        DetalleVenta detalle = new DetalleVenta();
        detalle.setProductoId(10L);
        detalle.setCantidad(2);
        venta.setDetalles(List.of(detalle));

        ProductoDTO productoMock = new ProductoDTO();
        productoMock.setSku("PROD-123");

        Venta ventaPagada = new Venta();
        ventaPagada.setEstado(Venta.EstadoVenta.PAGADA);
        
        VentaResponseDTO responseEsperado = new VentaResponseDTO();
        responseEsperado.setEstado(Venta.EstadoVenta.PAGADA);

        when(ventaRepository.findById(idVenta)).thenReturn(Optional.of(venta));
        when(catalogoClient.obtenerProductoPorId(anyString(), eq(10L))).thenReturn(productoMock);
        when(ventaRepository.save(any(Venta.class))).thenReturn(ventaPagada);
        when(ventaModelAssembler.toModel(ventaPagada)).thenReturn(responseEsperado);

        // When
        VentaResponseDTO resultado = ventaService.confirmarPago(idVenta);

        // Then
        assertEquals(Venta.EstadoVenta.PAGADA, resultado.getEstado());
        verify(inventarioClient).disminuirStock(anyString(), any(StockRequestDTO.class)); // Verifica que llame a inventario
        verify(ventaRepository).save(venta);
    }

    @Test
    @DisplayName("confirmarPago debe lanzar ResourceNotFoundException si la venta no existe")
    void confirmarPagoDebeLanzarExcepcionSiVentaNoExiste() {
        // Given
        when(ventaRepository.findById(99L)).thenReturn(Optional.empty());

        // When + Then
        assertThrows(ResourceNotFoundException.class, () -> ventaService.confirmarPago(99L));
        verify(inventarioClient, never()).disminuirStock(anyString(), any());
    }
}
