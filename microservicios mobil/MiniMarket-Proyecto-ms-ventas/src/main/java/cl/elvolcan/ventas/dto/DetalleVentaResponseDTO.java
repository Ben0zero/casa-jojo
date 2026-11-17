package cl.elvolcan.ventas.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "Representa una línea de detalle dentro de una venta confirmada")
public class DetalleVentaResponseDTO {
    
    @Schema(description = "ID del registro de detalle", example = "1")
    private Long id;
    
    @Schema(description = "ID del producto en el catálogo", example = "10")
    private Long productoId;
    
    @Schema(description = "Cantidad del producto comprada", example = "2")
    private Integer cantidad;
    
    @Schema(description = "Precio unitario del producto al momento de la venta", example = "1000.0")
    private Double precioUnitario;
    
    @Schema(description = "Subtotal de esta línea (cantidad * precioUnitario)", example = "2000.0")
    private Double subtotal;
}
