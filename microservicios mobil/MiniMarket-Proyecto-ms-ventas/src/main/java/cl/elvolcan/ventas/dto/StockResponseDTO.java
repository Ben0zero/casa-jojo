package cl.elvolcan.ventas.dto;

import lombok.Data;

@Data
public class StockResponseDTO {
    private String sku;
    private Double stockActual;  // Era Integer — inventario devuelve Double
    private Boolean bajoStock;
}