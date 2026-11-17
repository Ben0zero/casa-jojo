package cl.elvolcan.ventas.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;
import java.util.List;

@Data
@Schema(description = "Objeto que representa la solicitud para registrar una nueva venta en caja")
public class VentaRequestDTO {

    @Schema(description = "Tipo de cliente que determina el precio", example = "Residencial", allowableValues = {"Residencial", "Comercial"})
    private String tipoCliente = "Residencial";

    @NotEmpty(message = "La venta debe tener al menos un producto")
    @Valid // Dispara la validación interna de la lista
    @Schema(description = "Lista de productos de gas incluidos en la venta")
    private List<DetalleVentaRequestDTO> detalles;
}
