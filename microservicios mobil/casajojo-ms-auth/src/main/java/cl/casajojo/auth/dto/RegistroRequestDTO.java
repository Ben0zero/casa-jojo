package cl.casajojo.auth.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * Registro de clientes de La Casa JoJo.
 *
 * La app envía solo nombre, correo, teléfono y contraseña. Los campos del
 * esquema heredado de la tabla usuarios (rut, dirección, región, comuna, tipoCliente) se
 * conservan OPCIONALES para no romper Swagger ni las pruebas existentes; el
 * servicio los rellena con valores de Casa JoJo cuando no vienen.
 *
 * A2: ya NO existe el campo "nombreRol". El registro público siempre crea
 * cuentas CLIENTE; si el JSON trae "nombreRol", se ignora.
 */
@Data
@Schema(description = "Datos para crear una cuenta en La Casa JoJo")
public class RegistroRequestDTO {

    @Schema(description = "Nombre completo", example = "Camila González")
    @NotBlank(message = "El nombre es obligatorio")
    private String nombre;

    // M6: el correo es el usuario de login, así que debe venir y tener formato válido.
    // "username" se acepta como alias para clientes antiguos.
    @Schema(description = "Correo (será el usuario de login)", example = "camila@casajojo.cl")
    @JsonAlias("username")
    @NotBlank(message = "El correo es obligatorio")
    @Email(message = "El correo no tiene un formato válido")
    private String correo;

    // M6: opcional, pero si viene debe ser un celular de 9 dígitos que empiece con 9.
    @Schema(description = "Celular de 9 dígitos", example = "912345678")
    @Pattern(regexp = "^9\\d{8}$", message = "El teléfono debe tener 9 dígitos y empezar con 9")
    private String telefono;

    @Schema(description = "Contraseña de acceso", example = "Cliente123")
    @NotBlank(message = "La contraseña es obligatoria")
    @Size(min = 6, message = "La contraseña debe tener al menos 6 caracteres")
    private String password;

    // --- Campos heredados, opcionales (compatibilidad) ---

    @Schema(description = "RUT (opcional; se genera uno interno si no viene)", example = "12345678-9")
    private String rut;

    private String direccion;
    private String region;
    private String comuna;
    private String tipoCliente;
}
