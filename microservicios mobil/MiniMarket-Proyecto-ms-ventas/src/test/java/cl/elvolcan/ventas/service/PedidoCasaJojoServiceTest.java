package cl.elvolcan.ventas.service;

import cl.elvolcan.ventas.dto.ItemPedidoRequestDTO;
import cl.elvolcan.ventas.dto.PedidoRequestDTO;
import cl.elvolcan.ventas.dto.PedidoResponseDTO;
import cl.elvolcan.ventas.model.PedidoCasaJojo;
import cl.elvolcan.ventas.repository.PedidoCasaJojoRepository;
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

/** Pruebas del servicio de pedidos de La Casa JoJo. */
@ExtendWith(MockitoExtension.class)
class PedidoCasaJojoServiceTest {

    @Mock
    private PedidoCasaJojoRepository repository;

    @InjectMocks
    private PedidoCasaJojoService service;

    private PedidoRequestDTO request() {
        ItemPedidoRequestDTO item = new ItemPedidoRequestDTO();
        item.setNombre("Pollo asado");
        item.setPrecio(4800);
        item.setSolo(false);
        item.setAcompanamientos(List.of("Arroz", "Ensalada"));

        PedidoRequestDTO request = new PedidoRequestDTO();
        request.setNumero(7);
        request.setNombre("Camila González");
        request.setCorreo(" CAMILA@CASAJOJO.CL ");
        request.setHoraRetiro("13:00");
        request.setOrigen("Mostrador");
        request.setFormaPago("Transferencia");
        request.setIndicaciones("Sin cebolla");
        request.setItems(List.of(item));
        request.setTotal(4800);
        return request;
    }

    @Test
    @DisplayName("crear guarda el pedido con estado RECIBIDO, pago pendiente y sus platos")
    void crearPedidoCasaJojo() {
        when(repository.save(any(PedidoCasaJojo.class))).thenAnswer(invocacion -> {
            PedidoCasaJojo guardado = invocacion.getArgument(0);
            guardado.setId(100L);
            return guardado;
        });

        PedidoResponseDTO resultado = service.crear(request());

        assertEquals(100L, resultado.getId());
        assertEquals(7, resultado.getNumero());
        assertEquals("camila@casajojo.cl", resultado.getCorreo());
        assertEquals("RECIBIDO", resultado.getEstadoPedido());
        assertEquals("pendiente", resultado.getEstadoPago());
        assertEquals(4800, resultado.getTotal());
        assertEquals(1, resultado.getItems().size());
        assertEquals(List.of("Arroz", "Ensalada"),
                resultado.getItems().get(0).getAcompanamientos());
    }

    @Test
    @DisplayName("listarPorCorreo normaliza el correo antes de consultar")
    void listarPorCorreoNormalizado() {
        when(repository.findByClienteCorreoOrderByFechaCreacionDesc("camila@casajojo.cl"))
                .thenReturn(List.of());

        service.listarPorCorreo(" Camila@Casajojo.cl ");

        verify(repository).findByClienteCorreoOrderByFechaCreacionDesc("camila@casajojo.cl");
    }

    @Test
    @DisplayName("cambiarEstado actualiza el estado del pedido")
    void cambiarEstadoPedido() {
        PedidoCasaJojo pedido = new PedidoCasaJojo();
        pedido.setId(5L);
        pedido.setEstadoPedido(PedidoCasaJojo.EstadoPedido.RECIBIDO);
        when(repository.findById(5L)).thenReturn(Optional.of(pedido));
        when(repository.save(pedido)).thenReturn(pedido);

        PedidoResponseDTO resultado = service.cambiarEstado(5L, "listo");

        assertEquals("LISTO", resultado.getEstadoPedido());
    }
}
