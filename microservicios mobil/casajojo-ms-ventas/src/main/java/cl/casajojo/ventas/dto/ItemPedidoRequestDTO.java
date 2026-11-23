package cl.casajojo.ventas.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

/** Plato armado que viene desde el carrito de la app. */
@Data
@Schema(description = " Plato armado del carrito")
public class ItemPedidoRequestDTO {

    @NotBlank(message = "Cada plato debe tener nombre")
    @Size(max = 100, message = "El nombre del plato no puede superar 100 caracteres")
    @Schema(example = "Pollo asado")
    private String nombre;

    // M4: un plato siempre cuesta algo (la proteína sola vale $3.000).
    @NotNull(message = "Cada plato debe tener precio")
    @Positive(message = "El precio de cada plato debe ser mayor a 0")
    @Schema(example = "4800")
    private Integer precio;

    @Schema(example = "false")
    private Boolean solo = false;

    @Size(max = 10, message = "Un plato no puede tener más de 10 acompañamientos")
    @Schema(example = "[\"Arroz\", \"Ensalada\"]")
    private List<@NotBlank(message = "Los acompañamientos no pueden venir vacíos")
                 @Size(max = 60, message = "Nombre de acompañamiento demasiado largo") String> acompanamientos;
}
