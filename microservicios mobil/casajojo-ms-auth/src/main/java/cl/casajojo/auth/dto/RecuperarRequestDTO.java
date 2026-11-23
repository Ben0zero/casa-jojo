package cl.casajojo.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/** EFT: pedir un código de recuperación de contraseña. */
@Data
@Schema(description = "Correo de la cuenta que olvidó su contraseña")
public class RecuperarRequestDTO {

    @Schema(example = "cliente@casajojo.cl")
    @NotBlank(message = "El correo es obligatorio")
    @Email(message = "El correo no tiene un formato válido")
    private String correo;
}
