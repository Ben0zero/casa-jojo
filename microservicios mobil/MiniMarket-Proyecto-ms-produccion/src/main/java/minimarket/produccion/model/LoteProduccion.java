package minimarket.produccion.model;


import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "lotes_produccion")
public class LoteProduccion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    private String productoSku;

    @NotNull
    @Min(1)
    private Integer cantidad;

    private LocalDateTime fecha;

    @PrePersist
    public void prePersist() {
        this.fecha = LocalDateTime.now();
    }

    // GETTERS

public Long getId() {
    return id;
}

public String getProductoSku() {
    return productoSku;
}

public Integer getCantidad() {
    return cantidad;
}

public LocalDateTime getFecha() {
    return fecha;
}

// SETTERS

public void setProductoSku(String productoSku) {
    this.productoSku = productoSku;
}

public void setCantidad(Integer cantidad) {
    this.cantidad = cantidad;
}

public void setFecha(LocalDateTime fecha) {
    this.fecha = fecha;
}
}