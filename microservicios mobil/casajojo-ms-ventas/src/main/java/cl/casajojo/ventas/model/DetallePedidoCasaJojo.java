package cl.casajojo.ventas.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Data;

/** Una línea del pedido: proteína + acompañamientos elegidos por el cliente. */
@Entity
@Table(name = "detalles_pedido_casajojo")
@Data
public class DetallePedidoCasaJojo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "pedido_id", nullable = false)
    @JsonIgnore
    private PedidoCasaJojo pedido;

    @Column(name = "nombre_plato", nullable = false)
    private String nombrePlato;

    @Column(nullable = false)
    private Integer precio;

    @Column(nullable = false)
    private Boolean solo;

    /** Acompañamientos unidos por ", " para no crear otra tabla. */
    @Column(length = 1000)
    private String acompanamientos;

    @Column(nullable = false)
    private Integer cantidad = 1;
}
