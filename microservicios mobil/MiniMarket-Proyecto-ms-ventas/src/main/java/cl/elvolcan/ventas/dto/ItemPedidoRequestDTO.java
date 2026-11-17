package cl.elvolcan.ventas.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

/** Plato armado que viene desde el carrito de la app. */
@Data
@Schema(description = " Plato armado del carrito")
public class ItemPedidoRequestDTO {

    @NotBlank
    @Schema(example = "Pollo asado")
    private String nombre;

    @NotNull
    @Schema(example = "4800")
    private Integer precio;

    @Schema(example = "false")
    private Boolean solo = false;

    @Schema(example = "[\"Arroz\", \"Ensalada\"]")
    private List<String> acompanamientos;
}
