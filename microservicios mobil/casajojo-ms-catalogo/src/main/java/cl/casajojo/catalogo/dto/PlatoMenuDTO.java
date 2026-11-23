package cl.casajojo.catalogo.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Plato del menú de La Casa JoJo, tal como lo consume la app Android. */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Plato del menú (proteína o acompañamiento)")
public class PlatoMenuDTO {

    @Schema(example = "1")
    private Long id;

    @Schema(example = "CJ-PROT-01")
    private String sku;

    @Schema(example = "Pollo asado")
    private String nombre;

    @Schema(example = "Pollo asado con papas y ensalada")
    private String descripcion;

    @Schema(example = "4800")
    private Integer precio;

    /** "proteina" o "acompanamiento" (mismos valores de la app). */
    @Schema(example = "proteina")
    private String tipo;

    @Schema(example = "true")
    private Boolean disponible;
}
