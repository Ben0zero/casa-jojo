package cl.elvolcan.ventas.service;

import cl.elvolcan.ventas.dto.*;
import cl.elvolcan.ventas.exception.ResourceNotFoundException;
import cl.elvolcan.ventas.model.DetallePedidoCasaJojo;
import cl.elvolcan.ventas.model.PedidoCasaJojo;
import cl.elvolcan.ventas.repository.PedidoCasaJojoRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

/** Pedidos de La Casa JoJo: hora de retiro, origen y estados. */
@Service
@RequiredArgsConstructor
public class PedidoCasaJojoService {

    private final PedidoCasaJojoRepository repository;

    @Transactional
    public PedidoResponseDTO crear(PedidoRequestDTO request) {
        PedidoCasaJojo pedido = new PedidoCasaJojo();
        pedido.setNumero(request.getNumero() != null ? request.getNumero() : 1);
        pedido.setNombreCliente(request.getNombre().trim());
        pedido.setClienteCorreo(request.getCorreo().trim().toLowerCase(Locale.ROOT));
        pedido.setHoraRetiro(request.getHoraRetiro());
        pedido.setOrigen(request.getOrigen());
        pedido.setFormaPago(request.getFormaPago() == null ? "Transferencia" : request.getFormaPago());
        pedido.setIndicaciones(request.getIndicaciones() == null ? "" : request.getIndicaciones());
        pedido.setEstadoPago("pendiente");
        pedido.setTotal(request.getTotal());
        pedido.setEstadoPedido(PedidoCasaJojo.EstadoPedido.RECIBIDO);

        for (ItemPedidoRequestDTO item : request.getItems()) {
            DetallePedidoCasaJojo detalle = new DetallePedidoCasaJojo();
            detalle.setPedido(pedido);
            detalle.setNombrePlato(item.getNombre());
            detalle.setPrecio(item.getPrecio());
            detalle.setSolo(Boolean.TRUE.equals(item.getSolo()));
            detalle.setCantidad(1);
            detalle.setAcompanamientos(
                    item.getAcompanamientos() == null ? "" : String.join(", ", item.getAcompanamientos()));
            pedido.getDetalles().add(detalle);
        }

        return mapear(repository.save(pedido));
    }

    public List<PedidoResponseDTO> listarPorCorreo(String correo) {
        String normalizado = correo == null ? "" : correo.trim().toLowerCase(Locale.ROOT);
        return repository.findByClienteCorreoOrderByFechaCreacionDesc(normalizado).stream()
                .map(this::mapear)
                .collect(Collectors.toList());
    }

    public PedidoResponseDTO ultimoPorCorreo(String correo) {
        List<PedidoResponseDTO> pedidos = listarPorCorreo(correo);
        if (pedidos.isEmpty()) {
            throw new ResourceNotFoundException("El cliente no tiene pedidos registrados");
        }
        return pedidos.get(0);
    }

    @Transactional
    public PedidoResponseDTO cambiarEstado(Long id, String estado) {
        PedidoCasaJojo pedido = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Pedido no encontrado: " + id));
        try {
            pedido.setEstadoPedido(PedidoCasaJojo.EstadoPedido.valueOf(estado.trim().toUpperCase(Locale.ROOT)));
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Estado de pedido inválido: " + estado);
        }
        return mapear(repository.save(pedido));
    }

    @Transactional
    public PedidoResponseDTO marcarPagado(Long id) {
        PedidoCasaJojo pedido = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Pedido no encontrado: " + id));
        pedido.setEstadoPago("pagado");
        return mapear(repository.save(pedido));
    }

    private PedidoResponseDTO mapear(PedidoCasaJojo p) {
        List<ItemPedidoResponseDTO> items = p.getDetalles() == null
                ? Collections.emptyList()
                : p.getDetalles().stream()
                .map(d -> new ItemPedidoResponseDTO(
                        d.getNombrePlato(),
                        d.getPrecio(),
                        Boolean.TRUE.equals(d.getSolo()),
                        d.getAcompanamientos() == null || d.getAcompanamientos().isBlank()
                                ? Collections.emptyList()
                                : Arrays.asList(d.getAcompanamientos().split(", ")),
                        d.getCantidad()))
                .collect(Collectors.toList());

        return new PedidoResponseDTO(
                p.getId(), p.getNumero(), p.getNombreCliente(), p.getClienteCorreo(),
                p.getHoraRetiro(), p.getOrigen(), p.getFormaPago(), p.getIndicaciones(),
                p.getEstadoPedido() == null ? "RECIBIDO" : p.getEstadoPedido().name(),
                p.getEstadoPago(), p.getTotal(), p.getFechaCreacion(), items
        );
    }
}
