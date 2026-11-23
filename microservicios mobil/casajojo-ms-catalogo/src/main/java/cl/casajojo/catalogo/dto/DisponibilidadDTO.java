package cl.casajojo.catalogo.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/** Cuerpo de PUT /api/menu/{id}/disponible: marca un plato como disponible o agotado. */
@Data
@Schema(description = "Disponibilidad de un plato")
public class DisponibilidadDTO {

    @NotNull(message = "Indica si el plato está disponible (true) o agotado (false)")
    @Schema(example = "false")
    private Boolean disponible;
}
