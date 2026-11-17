package cl.elvolcan.catalogo.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "Datos de respuesta del producto")
public class ProductoResponseDTO {
    @Schema(example = "1")
    private Long id;
    @Schema(example = "GAS-005")
    private String sku;
    @Schema(example = "Cilindro de Gas 5kg")
    private String nombre;
    @Schema(example = "Cilindro de gas licuado de petróleo 5 kg")
    private String descripcion;
    @Schema(example = "15990")
    private Integer precioResidencial;
    @Schema(example = "14990")
    private Integer precioComercial;
    @Schema(example = "15990")
    private Integer precio;
    @Schema(example = "Gas")
    private String categoria;
    @Schema(description = "Estado actual en vitrina", example = "true")
    private Boolean activo;
}