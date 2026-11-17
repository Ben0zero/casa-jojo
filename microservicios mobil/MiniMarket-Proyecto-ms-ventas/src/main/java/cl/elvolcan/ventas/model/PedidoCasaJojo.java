package cl.elvolcan.ventas.model;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Pedido de La Casa JoJo. Tabla propia: no mezcla con las ventas de gas.
 */
@Entity
@Table(name = "pedidos_casajojo")
@Data
public class PedidoCasaJojo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Número correlativo que muestra la app (puede repetirse entre dispositivos). */
    @Column(nullable = false)
    private Integer numero;

    @Column(name = "cliente_correo", nullable = false)
    private String clienteCorreo;

    @Column(name = "nombre_cliente", nullable = false)
    private String nombreCliente;

    @Column(name = "hora_retiro", nullable = false)
    private String horaRetiro;

    @Column(nullable = false)
    private String origen;

    @Column(name = "forma_pago", nullable = false)
    private String formaPago;

    @Column(length = 1000)
    private String indicaciones;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado_pedido", nullable = false)
    private EstadoPedido estadoPedido;

    @Column(name = "estado_pago", nullable = false)
    private String estadoPago;

    @Column(nullable = false)
    private Integer total;

    @Column(name = "fecha_creacion", nullable = false)
    private LocalDateTime fechaCreacion;

    @OneToMany(mappedBy = "pedido", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<DetallePedidoCasaJojo> detalles = new ArrayList<>();

    @PrePersist
    protected void onCreate() {
        this.fechaCreacion = LocalDateTime.now();
        if (this.estadoPedido == null) {
            this.estadoPedido = EstadoPedido.RECIBIDO;
        }
        if (this.estadoPago == null || this.estadoPago.isBlank()) {
            this.estadoPago = "pendiente";
        }
    }

    public enum EstadoPedido {
        RECIBIDO, EN_PREPARACION, LISTO, ENTREGADO, CANCELADO
    }
}
