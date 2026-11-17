package cl.elvolcan.ventas.assembler;

import cl.elvolcan.ventas.controller.VentaController;
import cl.elvolcan.ventas.dto.DetalleVentaResponseDTO;
import cl.elvolcan.ventas.dto.VentaResponseDTO;
import cl.elvolcan.ventas.model.DetalleVenta;
import cl.elvolcan.ventas.model.Venta;
import org.springframework.hateoas.server.mvc.RepresentationModelAssemblerSupport;
import org.springframework.stereotype.Component;

import java.util.stream.Collectors;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

@Component
public class VentaModelAssembler extends RepresentationModelAssemblerSupport<Venta, VentaResponseDTO> {

    public VentaModelAssembler() {
        super(VentaController.class, VentaResponseDTO.class);
    }

    @Override
    public VentaResponseDTO toModel(Venta entity) {
        // 1. Instanciar el DTO
        VentaResponseDTO dto = instantiateModel(entity);

        // 2. Mapear datos de Venta a VentaResponseDTO
        dto.setId(entity.getId());
        dto.setTotal(entity.getTotal());
        dto.setEstado(entity.getEstado());
        dto.setFechaVenta(entity.getFechaVenta());

        // 3. Mapear la lista de Detalles a DetalleVentaResponseDTO
        if (entity.getDetalles() != null) {
            dto.setDetalles(entity.getDetalles().stream()
                    .map(this::toDetalleDTO)
                    .collect(Collectors.toList()));
        }

        // 4. Agregar Links HATEOAS
        // Link hacia la lista general de ventas
        dto.add(linkTo(methodOn(VentaController.class).listarTodas()).withRel("todas-las-ventas"));

        // Link para confirmar el pago (solo si está pendiente)
        if (entity.getEstado() == Venta.EstadoVenta.PENDIENTE_PAGO) {
            dto.add(linkTo(methodOn(VentaController.class).confirmarPago(entity.getId())).withRel("confirmar-pago"));
        }

        return dto;
    }

    // Método auxiliar para mapear los detalles
    private DetalleVentaResponseDTO toDetalleDTO(DetalleVenta detalle) {
        DetalleVentaResponseDTO dto = new DetalleVentaResponseDTO();
        dto.setId(detalle.getId());
        dto.setProductoId(detalle.getProductoId());
        dto.setCantidad(detalle.getCantidad());
        dto.setPrecioUnitario(detalle.getPrecioUnitario());
        dto.setSubtotal(detalle.getSubtotal());
        return dto;
    }
}
