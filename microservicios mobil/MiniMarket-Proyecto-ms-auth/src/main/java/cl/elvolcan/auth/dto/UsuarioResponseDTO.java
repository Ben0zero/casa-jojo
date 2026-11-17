package cl.elvolcan.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Schema(description = "Datos de respuesta del usuario autenticado o registrado")
public class UsuarioResponseDTO {
    
    @Schema(example = "1")
    private Long id;
    @Schema(example = "Juan Perez")
    private String nombre;
    @Schema(description = "Correo del cliente (la app lo usa como identificador)", example = "camila@casajojo.cl")
    private String correo;
    @Schema(example = "12345678-9")
    private String rut;
    @Schema(example = "jperez")
    private String username;
    @Schema(example = "ADMIN")
    private String nombreRol;
    @Schema(description = "Direccion del usuario", example = "Av. Independencia 123")
    private String direccion;
    @Schema(example = "Region de Nuble")
    private String region;
    @Schema(example = "Chillan")
    private String comuna;
    @Schema(example = "Residencial")
    private String tipoCliente;
    @Schema(example = "+56912345678")
    private String telefono;
    @Schema(description = "Token JWT generado", example = "eyJhbGciOiJIUzI1NiJ9...")
    private String token;
}
