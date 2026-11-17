package cl.elvolcan.ventas.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

/** Solicitud de pedido de La Casa JoJo (hora de retiro, origen y estados). */
@Data
@Schema(description = "Pedido enviado por la app móvil")
public class PedidoRequestDTO {

    @Schema(example = "7")
    private Integer numero;

    @NotBlank
    @Schema(example = "Camila González")
    private String nombre;

    @NotBlank
    @Schema(example = "camila@casajojo.cl")
    private String correo;

    @NotBlank
    @Schema(example = "13:00")
    private String horaRetiro;

    @NotBlank
    @Schema(example = "Mostrador", allowableValues = {"Mostrador", "Local", "Retiro rápido"})
    private String origen;

    @Schema(example = "Transferencia")
    private String formaPago;

    @Schema(example = "Sin cebolla")
    private String indicaciones;

    @NotEmpty
    @Valid
    private List<ItemPedidoRequestDTO> items;

    @NotNull
    @Schema(example = "4800")
    private Integer total;
}
