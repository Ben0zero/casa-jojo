package cl.elvolcan.ventas.dto;

import cl.elvolcan.ventas.model.Venta.EstadoVenta;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.hateoas.RepresentationModel;
import java.time.LocalDateTime;
import java.util.List;

@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "DTO de respuesta que representa una Venta con sus links HATEOAS")
public class VentaResponseDTO extends RepresentationModel<VentaResponseDTO> {

    @Schema(description = "ID único de la venta", example = "1")
    private Long id;

    @Schema(description = "Total a pagar por la venta", example = "4500.0")
    private Double total;

    @Schema(description = "Estado actual de la venta", example = "PENDIENTE_PAGO")
    private EstadoVenta estado;

    @Schema(description = "Fecha y hora en que se registró la venta")
    private LocalDateTime fechaVenta;

    @Schema(description = "Lista de productos incluidos en la venta")
    private List<DetalleVentaResponseDTO> detalles;
}