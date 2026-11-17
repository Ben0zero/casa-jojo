package cl.elvolcan.inventario.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "inventarios")
public class Inventario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "El SKU es obligatorio")
    @Column(unique = true, nullable = false)
    private String sku;

    @NotNull(message = "El stock actual es obligatorio")
    @Min(value = 0, message = "El stock no puede ser negativo")
    private Double stockActual;

    @NotNull(message = "El nivel crítico es obligatorio")
    @Min(value = 1, message = "El nivel crítico debe ser mayor a 0")
    private Double nivelCritico;

    @NotNull(message = "El nivel máximo es obligatorio")
    private Double nivelMaximo;

    @Column(name = "ultima_actualizacion")
    private LocalDateTime ultimaActualizacion;

    //  Se ejecuta automáticamente
    @PrePersist
    @PreUpdate
    public void actualizarFecha() {
        this.ultimaActualizacion = LocalDateTime.now();
    }

    // Getters y Setters

    public Long getId() { return id; }

    public String getSku() { return sku; }
    public void setSku(String sku) { this.sku = sku; }

    public Double getStockActual() { return stockActual; }
    public void setStockActual(Double stockActual) { this.stockActual = stockActual; }

    public Double getNivelCritico() { return nivelCritico; }
    public void setNivelCritico(Double nivelCritico) { this.nivelCritico = nivelCritico; }

    public Double getNivelMaximo() { return nivelMaximo; }
    public void setNivelMaximo(Double nivelMaximo) { this.nivelMaximo = nivelMaximo; }

    public LocalDateTime getUltimaActualizacion() { return ultimaActualizacion; }
}