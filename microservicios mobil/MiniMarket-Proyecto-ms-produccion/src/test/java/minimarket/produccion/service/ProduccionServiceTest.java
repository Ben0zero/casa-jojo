package minimarket.produccion.service;

import minimarket.produccion.client.InventarioClient;
import minimarket.produccion.dto.request.ProduccionRequestDTO;
import minimarket.produccion.dto.response.ProduccionResponseDTO;
import minimarket.produccion.exception.ResourceNotFoundException;
import minimarket.produccion.model.LoteProduccion;
import minimarket.produccion.repository.ProduccionRepository;
import minimarket.produccion.service.impl.ProduccionServiceImpl;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProduccionServiceTest {

    @Mock
    private ProduccionRepository repository;

    @Mock
    private InventarioClient inventarioClient;

    @InjectMocks
    private ProduccionServiceImpl service;

    @Test
    @DisplayName("crearLote debe guardar lote, notificar a inventario y retornar DTO correcto")
    void crearLoteDebeGuardarYActualizarInventario() {
        // Given
        ProduccionRequestDTO request = new ProduccionRequestDTO();
        request.setProductoSku("PAN-001");
        request.setCantidad(100);

        LoteProduccion loteGuardado = new LoteProduccion();
        loteGuardado.setProductoSku("PAN-001");
        loteGuardado.setCantidad(100);
        loteGuardado.setFecha(LocalDateTime.now());

        when(repository.save(any(LoteProduccion.class))).thenReturn(loteGuardado);
        when(inventarioClient.sumarStock("PAN-001", 100)).thenReturn("Stock actualizado correctamente");

        // When
        ProduccionResponseDTO resultado = service.crearLote(request);

        // Then
        assertNotNull(resultado);
        assertEquals("PAN-001", resultado.getProductoSku());
        assertEquals(100, resultado.getCantidad());

        verify(repository).save(any(LoteProduccion.class));
        verify(inventarioClient).sumarStock("PAN-001", 100);
    }

    @Test
    @DisplayName("buscarPorId debe retornar lote cuando existe")
    void buscarPorIdDebeRetornarLoteCuandoExiste() {
        // Given
        LoteProduccion lote = new LoteProduccion();
        lote.setProductoSku("PAN-001");
        lote.setCantidad(50);
        lote.setFecha(LocalDateTime.now());

        when(repository.findById(1L)).thenReturn(Optional.of(lote));

        // When
        ProduccionResponseDTO resultado = service.buscarPorId(1L);

        // Then
        assertNotNull(resultado);
        assertEquals("PAN-001", resultado.getProductoSku());
        assertEquals(50, resultado.getCantidad());

        verify(repository).findById(1L);
    }

    @Test
    @DisplayName("buscarPorId debe lanzar excepción cuando no existe")
    void buscarPorIdDebeLanzarExcepcionCuandoNoExiste() {
        // Given
        when(repository.findById(99L)).thenReturn(Optional.empty());

        // When + Then
        assertThrows(
                ResourceNotFoundException.class,
                () -> service.buscarPorId(99L)
        );

        verify(repository).findById(99L);
    }
    
    @Test
    @DisplayName("obtenerTodos debe retornar lista de lotes")
    void obtenerTodosDebeRetornarLista() {
        // Given
        LoteProduccion lote1 = new LoteProduccion();
        lote1.setProductoSku("PAN-001");
        lote1.setCantidad(50);

        LoteProduccion lote2 = new LoteProduccion();
        lote2.setProductoSku("PAN-002");
        lote2.setCantidad(30);

        when(repository.findAll()).thenReturn(List.of(lote1, lote2));

        // When
        List<ProduccionResponseDTO> resultado = service.obtenerTodos();

        // Then
        assertEquals(2, resultado.size());
        verify(repository).findAll();
    }

    @Test
    @DisplayName("eliminar debe invocar el borrado en el repositorio por ID")
    void eliminarDebeBorrarPorId() {
        // Given
        Long idLote = 1L;
        doNothing().when(repository).deleteById(idLote);

        // When
        service.eliminar(idLote);

        // Then
        verify(repository).deleteById(idLote);
    }
}