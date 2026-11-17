package cl.elvolcan.inventario.dto.request;

import jakarta.validation.constraints.*;

public class StockRequestDTO {

    @NotBlank(message = "El SKU es obligatorio")
    private String sku;

    @NotNull(message = "El stock actual es obligatorio")
    private Double stockActual; 

    // se quita los @NotNull de aqui abajo para que no estorben al hacer PUT
    private Double nivelCritico;
    private Double nivelMaximo;

    // Getters y Setters
    public String getSku() { return sku; }
    public void setSku(String sku) { this.sku = sku; }

    public Double getStockActual() { return stockActual; }
    public void setStockActual(Double stockActual) { this.stockActual = stockActual; }

    public Double getNivelCritico() { return nivelCritico; }
    public void setNivelCritico(Double nivelCritico) { this.nivelCritico = nivelCritico; }

    public Double getNivelMaximo() { return nivelMaximo; }
    public void setNivelMaximo(Double nivelMaximo) { this.nivelMaximo = nivelMaximo; }
}
