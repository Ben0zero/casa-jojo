package cl.casajojo.ventas.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/** Respuesta que la app guarda como caché del último pedido. */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Pedido confirmado de La Casa JoJo")
public class PedidoResponseDTO {

    private Long id;
    private Integer numero;
    private String nombre;
    private String correo;
    private String horaRetiro;
    private String origen;
    private String formaPago;
    private String indicaciones;
    /** RECIBIDO, EN_PREPARACION, LISTO, ENTREGADO o CANCELADO. */
    private String estadoPedido;
    private String estadoPago;
    private Integer total;
    private LocalDateTime fechaCreacion;
    private List<ItemPedidoResponseDTO> items;
    /** M9: UUID que generó la app para este pedido (null en pedidos antiguos). */
    private String referencia;
}
