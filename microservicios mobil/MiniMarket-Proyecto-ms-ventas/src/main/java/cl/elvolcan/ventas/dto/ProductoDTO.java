package cl.elvolcan.ventas.dto;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;

@Data
@Getter
@Setter 
public class ProductoDTO {
    private Long id;
    private String sku;
    private String nombre;
    private Integer precio;
    private Integer precioResidencial;
    private Integer precioComercial;
    private String categoria;
    private Boolean activo;
}
