package cl.elvolcan.catalogo.model;


import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
@Entity
@Table(name = "productos")
public class Producto {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    //sku debe ser unico si no, gg
    @NotBlank(message = "El SKU es obligatorio")
    @Column(unique = true, nullable = false)
    private String sku;

    @NotBlank(message = "El nombre del producto es obligatorio")
    private String nombre;

    private String descripcion;

    // Precios por tipo de cliente para Distribuidora de Gas El Volcan
    @Min(value = 0, message = "El precio no puede ser negativo")
    @Column(name = "precio_residencial")
    private Integer precioResidencial;

    @Min(value = 0, message = "El precio no puede ser negativo")
    @Column(name = "precio_comercial")
    private Integer precioComercial;

    // Compatibilidad con versiones anteriores
    private Integer precio;

    private String categoria;

    private Boolean activo;  // esto pausa ventas sin borrar el producto

    // se ejecuta automatic al guardar por primera vez
    @PrePersist
    public void PrePersist(){
        if (this.activo == null) {
            this.activo = true;
        }
        if (this.precioResidencial != null && this.precioComercial == null) {
            this.precioComercial = this.precioResidencial;
        }
        if (this.precioComercial != null && this.precioResidencial == null) {
            this.precioResidencial = this.precioComercial;
        }
        if (this.precio == null && this.precioResidencial != null) {
            this.precio = this.precioResidencial;
        }
    }
}
