package cl.casajojo.auth.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Respuesta simple {"mensaje": "..."}. */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class MensajeDTO {
    private String mensaje;
}
