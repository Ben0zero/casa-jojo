package minimarket.produccion.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public class ProduccionRequestDTO {

    @NotBlank(message = "El SKU del producto es obligatorio")
    private String productoSku;

    @NotNull(message = "La cantidad es obligatoria")
    @Min(value = 1, message = "Debe producir al menos 1 unidad")
    private Integer cantidad;

    // Getters y Setters
    public String getProductoSku() {
        return productoSku;
    }

    public void setProductoSku(String productoSku) {
        this.productoSku = productoSku;
    }

    public Integer getCantidad() {
        return cantidad;
    }

    public void setCantidad(Integer cantidad) {
        this.cantidad = cantidad;
    }
}