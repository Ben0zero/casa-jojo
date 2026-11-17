package cl.elvolcan.catalogo.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import lombok.Data;

@Data
@Schema(description = "Estructura para crear o modificar un producto en el catálogo")
public class ProductoRequestDTO {

    @Schema(description = "Código único del producto", example = "PAN-004")
    @NotBlank(message = "El SKU es obligatorio")
    private String sku;

    @Schema(description = "Nombre visible del producto", example = "Pan Amasado")
    @NotBlank(message = "El nombre es obligatorio")
    private String nombre;

    @Schema(description = "Descripción detallada", example = "Pan amasado casero horneado a leña")
    private String descripcion; 

    @Schema(description = "Precio para cliente residencial", example = "15990")
    @Min(value = 0, message = "El precio no puede ser negativo")
    private Integer precioResidencial;

    @Schema(description = "Precio para cliente comercial", example = "14990")
    @Min(value = 0, message = "El precio no puede ser negativo")
    private Integer precioComercial;

    @Schema(description = "Precio de venta al público (compatibilidad)", example = "15990")
    @Min(value = 0, message = "El precio no puede ser negativo")
    private Integer precio;

    @Schema(description = "Categoría a la que pertenece", example = "Gas")
    private String categoria;
}
