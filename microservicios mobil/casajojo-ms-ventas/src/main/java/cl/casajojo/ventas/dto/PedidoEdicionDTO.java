package cl.casajojo.ventas.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * EFT / RF-12: datos del retiro que el cliente puede editar mientras el pedido
 * sigue RECIBIDO (la cocina aún no empieza a prepararlo).
 */
@Data
@Schema(description = "Datos editables de un pedido en estado RECIBIDO")
public class PedidoEdicionDTO {

    @NotBlank(message = "El nombre es obligatorio")
    @Size(max = 100, message = "El nombre no puede superar 100 caracteres")
    @Schema(example = "Camila González")
    private String nombre;

    @NotBlank(message = "La hora de retiro es obligatoria")
    @Pattern(regexp = "^([01]\\d|2[0-3]):[0-5]\\d$", message = "La hora de retiro debe tener formato HH:mm")
    @Schema(example = "13:30")
    private String horaRetiro;

    @NotBlank(message = "La forma de pago es obligatoria")
    @Size(max = 40, message = "La forma de pago no puede superar 40 caracteres")
    @Schema(example = "Pagar en tienda")
    private String formaPago;

    @Size(max = 500, message = "Las indicaciones no pueden superar 500 caracteres")
    @Schema(example = "Sin ají")
    private String indicaciones;
}
