package cl.elvolcan.ventas.model;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "ventas")
@Data 
public class Venta {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "usuario")
    private String usuario;

    @Column(nullable = false)
    private Double total;

    // Usaremos un Enum para evitar errores de tipeo en los estados
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EstadoVenta estado;

    @Column(name = "fecha_venta")
    private LocalDateTime fechaVenta;

    // Relación 1 a Muchos: Una venta tiene muchos detalles (productos)
    @OneToMany(mappedBy = "venta", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<DetalleVenta> detalles = new ArrayList<>();

    @PrePersist
    protected void onCreate() {
        this.fechaVenta = LocalDateTime.now();
        if (this.estado == null) {
            this.estado = EstadoVenta.PENDIENTE_PAGO; // Estado por defecto
        }
    }

    public enum EstadoVenta {
        PENDIENTE_PAGO,
        PAGADA,
        CANCELADA
    }
}