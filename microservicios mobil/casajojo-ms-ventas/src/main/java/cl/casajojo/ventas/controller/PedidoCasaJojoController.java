package cl.casajojo.ventas.controller;

import cl.casajojo.ventas.dto.PedidoEdicionDTO;
import cl.casajojo.ventas.dto.PedidoRequestDTO;
import cl.casajojo.ventas.dto.PedidoResponseDTO;
import cl.casajojo.ventas.security.UsuarioActual;
import cl.casajojo.ventas.service.PedidoCasaJojoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * API de pedidos que consume la app Android de La Casa JoJo.
 * A1: todas las rutas exigen JWT; el correo del token decide qué pedidos se pueden ver o tocar.
 */
@RestController
@RequestMapping("/api/pedidos")
@RequiredArgsConstructor
@Tag(name = "Pedidos Casa JoJo", description = "Pedidos con hora de retiro, origen y estados")
public class PedidoCasaJojoController {

    private final PedidoCasaJojoService service;

    @Operation(summary = "Crear pedido", description = "Recibe el carrito, la hora de retiro y el origen. El correo debe ser el del token.")
    @PostMapping
    public ResponseEntity<PedidoResponseDTO> crear(@Valid @RequestBody PedidoRequestDTO request, Authentication auth) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.crear(request, UsuarioActual.desde(auth)));
    }

    @Operation(summary = "Pedidos para cocina", description = "Solo COCINA o ADMIN. Sin ?estado= devuelve los activos.")
    @GetMapping
    public ResponseEntity<List<PedidoResponseDTO>> paraCocina(@RequestParam(required = false) String estado) {
        return ResponseEntity.ok(service.listarParaCocina(estado));
    }

    @Operation(summary = "Pedidos de un cliente", description = "Consulta por correo para sincronizar el historial local.")
    @GetMapping("/mis-pedidos")
    public ResponseEntity<List<PedidoResponseDTO>> misPedidos(@RequestParam String correo, Authentication auth) {
        return ResponseEntity.ok(service.listarPorCorreo(correo, UsuarioActual.desde(auth)));
    }

    @Operation(summary = "Último pedido de un cliente")
    @GetMapping("/ultimo")
    public ResponseEntity<PedidoResponseDTO> ultimo(@RequestParam String correo, Authentication auth) {
        return ResponseEntity.ok(service.ultimoPorCorreo(correo, UsuarioActual.desde(auth)));
    }

    @Operation(summary = "Cambiar estado",
            description = "RECIBIDO → EN_PREPARACION → LISTO → ENTREGADO (cocina). El cliente solo puede CANCELAR dentro de 15 s.")
    @PutMapping("/{id}/estado")
    public ResponseEntity<PedidoResponseDTO> cambiarEstado(@PathVariable Long id, @RequestParam String estado,
                                                           Authentication auth) {
        return ResponseEntity.ok(service.cambiarEstado(id, estado, UsuarioActual.desde(auth)));
    }

    @Operation(summary = "Editar pedido (RF-12)",
            description = "El dueño cambia nombre, hora de retiro, forma de pago e indicaciones mientras el pedido siga RECIBIDO (si no, 409).")
    @PutMapping("/{id}")
    public ResponseEntity<PedidoResponseDTO> editar(@PathVariable Long id, @Valid @RequestBody PedidoEdicionDTO datos,
                                                    Authentication auth) {
        return ResponseEntity.ok(service.editar(id, datos, UsuarioActual.desde(auth)));
    }

    @Operation(summary = "Marcar pago como pagado", description = "Solo COCINA o ADMIN (caja).")
    @PutMapping("/{id}/pagar")
    public ResponseEntity<PedidoResponseDTO> pagar(@PathVariable Long id) {
        return ResponseEntity.ok(service.marcarPagado(id));
    }

    @Operation(summary = "Retirar con el QR de la tienda",
            description = "Valida el código del QR; si el pedido está LISTO queda pagado y ENTREGADO.")
    @PutMapping("/{id}/retirar")
    public ResponseEntity<PedidoResponseDTO> retirar(@PathVariable Long id, @RequestParam String codigo,
                                                     Authentication auth) {
        return ResponseEntity.ok(service.retirar(id, codigo, UsuarioActual.desde(auth)));
    }
}
