package cl.casajojo.ventas.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

/**
 * Solicitud de pedido de La Casa JoJo (hora de retiro, origen y estados).
 *
 * M4: el servidor ya no confía a ciegas en lo que envía el cliente: valida
 * formatos y largos, y PedidoCasaJojoService comprueba que el total sea la
 * suma de los platos (si no cuadra → 400).
 */
@Data
@Schema(description = "Pedido enviado por la app móvil")
public class PedidoRequestDTO {

    @Min(value = 1, message = "El número de pedido debe ser positivo")
    @Schema(example = "7")
    private Integer numero;

    // M9 / A4: UUID del pedido generado por la app. Si ya existe un pedido con esta
    // referencia para el mismo correo, se devuelve ese en vez de crear otro (reenvío seguro).
    @Pattern(regexp = "^[A-Za-z0-9-]{8,64}$", message = "La referencia del pedido no es válida")
    @Schema(example = "3f1c2a9e-7b4d-4c1e-9a55-2f0d6c8b1e77")
    private String referencia;

    @NotBlank(message = "El nombre es obligatorio")
    @Size(max = 100, message = "El nombre no puede superar 100 caracteres")
    @Schema(example = "Camila González")
    private String nombre;

    // A1: debe coincidir con el correo del token (salvo personal COCINA/ADMIN).
    @NotBlank(message = "El correo es obligatorio")
    @Email(message = "El correo no tiene un formato válido")
    @Size(max = 150, message = "El correo es demasiado largo")
    @Schema(example = "camila@casajojo.cl")
    private String correo;

    @NotBlank(message = "La hora de retiro es obligatoria")
    @Pattern(regexp = "^([01]\\d|2[0-3]):[0-5]\\d$", message = "La hora de retiro debe tener formato HH:mm")
    @Schema(example = "13:00")
    private String horaRetiro;

    @NotBlank(message = "El origen es obligatorio")
    @Size(max = 30, message = "El origen no puede superar 30 caracteres")
    @Schema(example = "Mostrador", allowableValues = {"Mostrador", "Local", "Retiro rápido"})
    private String origen;

    @Size(max = 40, message = "La forma de pago no puede superar 40 caracteres")
    @Schema(example = "Transferencia")
    private String formaPago;

    @Size(max = 500, message = "Las indicaciones no pueden superar 500 caracteres")
    @Schema(example = "Sin cebolla")
    private String indicaciones;

    @NotEmpty(message = "El pedido debe tener al menos un plato")
    @Size(max = 20, message = "Un pedido no puede tener más de 20 platos")
    @Valid
    private List<ItemPedidoRequestDTO> items;

    @NotNull(message = "El total es obligatorio")
    @Positive(message = "El total debe ser mayor a 0")
    @Schema(example = "4800")
    private Integer total;
}
