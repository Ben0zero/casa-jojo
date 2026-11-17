package cl.elvolcan.catalogo.dto;

import lombok.Data;

@Data
public class InventarioInitDTO {
    private String sku;
    private Double stockActual;
    private Double nivelCritico;
    private Double nivelMaximo;
}