package cl.elvolcan.ventas.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import lombok.Data;

@Data
@Schema(description = "Detalle de un producto de gas en la venta")
public class DetalleVentaRequestDTO {
    
    @Schema(description = "ID del producto en el catálogo", example = "10")
    private Long productoId; 
    
    @Min(value = 1, message = "Si envías cantidad, debe ser al menos 1")
    @Schema(description = "Cantidad del producto a comprar", example = "2")
    private Integer cantidad;

}
