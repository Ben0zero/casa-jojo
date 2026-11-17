package cl.elvolcan.inventario.dto.response;

public class StockResponseDTO {

    private String sku;
    private Double stockActual;
    private Boolean bajoStock;

    public StockResponseDTO(String sku, Double stockActual, Boolean bajoStock) {
        this.sku = sku;
        this.stockActual = stockActual;
        this.bajoStock = bajoStock;
    }

    // Getters

    public String getSku() { return sku; }
    public Double getStockActual() { return stockActual; }
    public Boolean getBajoStock() { return bajoStock; }
}
