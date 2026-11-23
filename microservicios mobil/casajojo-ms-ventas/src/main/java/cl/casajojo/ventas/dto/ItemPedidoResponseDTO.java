package cl.casajojo.ventas.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/** Plato devuelto al cliente para poder reconstruir su pedido. */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = " Plato de un pedido confirmado")
public class ItemPedidoResponseDTO {

    private String nombre;
    private Integer precio;
    private Boolean solo;
    private List<String> acompanamientos;
    private Integer cantidad;
}
