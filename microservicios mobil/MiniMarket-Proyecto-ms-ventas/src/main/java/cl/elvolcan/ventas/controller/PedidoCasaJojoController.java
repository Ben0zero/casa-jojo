package cl.elvolcan.ventas.controller;

import cl.elvolcan.ventas.dto.PedidoRequestDTO;
import cl.elvolcan.ventas.dto.PedidoResponseDTO;
import cl.elvolcan.ventas.service.PedidoCasaJojoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** API de pedidos que consume la app Android de La Casa JoJo. */
@RestController
@RequestMapping("/api/pedidos")
@RequiredArgsConstructor
@Tag(name = "Pedidos Casa JoJo", description = "Pedidos con hora de retiro, origen y estados")
public class PedidoCasaJojoController {

    private final PedidoCasaJojoService service;

    @Operation(summary = "Crear pedido", description = "Recibe el carrito, la hora de retiro y el origen.")
    @PostMapping
    public ResponseEntity<PedidoResponseDTO> crear(@Valid @RequestBody PedidoRequestDTO request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.crear(request));
    }

    @Operation(summary = "Pedidos de un cliente", description = "Consulta por correo para sincronizar el historial local.")
    @GetMapping("/mis-pedidos")
    public ResponseEntity<List<PedidoResponseDTO>> misPedidos(@RequestParam String correo) {
        return ResponseEntity.ok(service.listarPorCorreo(correo));
    }

    @Operation(summary = "Último pedido de un cliente")
    @GetMapping("/ultimo")
    public ResponseEntity<PedidoResponseDTO> ultimo(@RequestParam String correo) {
        return ResponseEntity.ok(service.ultimoPorCorreo(correo));
    }

    @Operation(summary = "Cambiar estado", description = "RECIBIDO → EN_PREPARACION → LISTO → ENTREGADO.")
    @PutMapping("/{id}/estado")
    public ResponseEntity<PedidoResponseDTO> cambiarEstado(@PathVariable Long id, @RequestParam String estado) {
        return ResponseEntity.ok(service.cambiarEstado(id, estado));
    }

    @Operation(summary = "Marcar pago como pagado")
    @PutMapping("/{id}/pagar")
    public ResponseEntity<PedidoResponseDTO> pagar(@PathVariable Long id) {
        return ResponseEntity.ok(service.marcarPagado(id));
    }
}
