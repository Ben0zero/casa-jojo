package cl.casajojo.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

/** EFT: cambio de contraseña con la sesión iniciada. */
@Data
@Schema(description = "Contraseña actual y nueva contraseña")
public class CambiarPasswordDTO {

    @Schema(example = "Cliente123")
    @NotBlank(message = "La contraseña actual es obligatoria")
    private String passwordActual;

    @Schema(example = "Nueva123")
    @NotBlank(message = "La nueva contraseña es obligatoria")
    @Size(min = 6, max = 72, message = "La contraseña debe tener entre 6 y 72 caracteres")
    @Pattern(regexp = "^(?=.*[A-Za-z])(?=.*\\d).+$", message = "La contraseña debe tener letras y números")
    private String nuevaPassword;
}
