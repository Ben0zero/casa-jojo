package cl.elvolcan.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * Registro de clientes de La Casa JoJo.
 *
 * La app envía solo nombre, correo, teléfono y contraseña. Los campos del
 * antiguo formulario de gas (rut, dirección, región, comuna, tipoCliente) se
 * conservan OPCIONALES para no romper Swagger ni las pruebas existentes; el
 * servicio los rellena con valores de Casa JoJo cuando no vienen.
 */
@Data
@Schema(description = "Datos para crear una cuenta en La Casa JoJo")
public class RegistroRequestDTO {

    @Schema(description = "Nombre completo", example = "Camila González")
    @NotBlank(message = "El nombre es obligatorio")
    private String nombre;

    @Schema(description = "Correo (será el usuario de login)", example = "camila@casajojo.cl")
    private String correo;

    @Schema(description = "Celular de 9 dígitos", example = "912345678")
    private String telefono;

    @Schema(description = "Contraseña de acceso", example = "Cliente123")
    @NotBlank(message = "La contraseña es obligatoria")
    @Size(min = 6, message = "La contraseña debe tener al menos 6 caracteres")
    private String password;

    // --- Campos heredados, opcionales (compatibilidad) ---

    @Schema(description = "RUT (opcional; se genera uno interno si no viene)", example = "12345678-9")
    private String rut;

    @Schema(description = "Usuario anterior (opcional; se usa el correo)", example = "camila@casajojo.cl")
    private String username;

    @Schema(description = "Rol solicitado (opcional, por defecto CLIENTE)", example = "CLIENTE")
    private String nombreRol;

    private String direccion;
    private String region;
    private String comuna;
    private String tipoCliente;
}
