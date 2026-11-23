package cl.casajojo.auth.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * Login de La Casa JoJo.
 *
 * La app envía {"correo": "...", "password": "..."}.
 * El campo "username" se conserva para no romper clientes antiguos: en la BD
 * el correo es el username (así funciona CustomUserDetailsService).
 */
@Data
@Schema(description = "Credenciales de acceso de un cliente de La Casa JoJo")
public class LoginRequestDTO {

    @Schema(description = "Correo del cliente", example = "cliente@casajojo.cl")
    @JsonAlias("username")
    @NotBlank(message = "El correo no puede estar vacío")
    private String correo;

    @Schema(description = "Contraseña del cliente", example = "Cliente123")
    @NotBlank(message = "La contraseña no puede estar vacía")
    private String password;

    /** Compatibilidad con el DTO anterior y con las pruebas existentes. */
    public String getUsername() {
        return correo;
    }

    public void setUsername(String username) {
        this.correo = username;
    }
}
