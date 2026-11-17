package cl.elvolcan.ventas.controller;

import cl.elvolcan.ventas.dto.VentaRequestDTO;
import cl.elvolcan.ventas.dto.VentaResponseDTO; // NUEVO IMPORTE
import cl.elvolcan.ventas.service.VentaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/ventas")
@RequiredArgsConstructor
@Tag(name = "Ventas", description = "Operaciones relacionadas con el registro y confirmación de ventas")
public class VentaController {

    private final VentaService ventaService;

    @Operation(summary = "Listar todas las ventas", description = "Obtiene el historial completo de todas las ventas registradas en el sistema")
    @GetMapping
    public ResponseEntity<List<VentaResponseDTO>> listarTodas() {
        return ResponseEntity.ok(ventaService.listarTodas());
    }

    @Operation(summary = "Mis compras", description = "Obtiene el historial de compras del usuario autenticado")
    @GetMapping("/mis-compras")
    public ResponseEntity<List<VentaResponseDTO>> misCompras() {
        return ResponseEntity.ok(ventaService.listarMisCompras());
    }

    @Operation(summary = "Registrar un nuevo pedido", description = "Crea una venta de productos de gas y consulta catálogo e inventario")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "201", description = "Venta creada exitosamente"),
        @ApiResponse(responseCode = "400", description = "Datos inválidos o stock insuficiente"),
        @ApiResponse(responseCode = "404", description = "Producto no encontrado")
    })
    @PostMapping
    public ResponseEntity<VentaResponseDTO> registrarPedido(@Valid @RequestBody VentaRequestDTO ventaRequestDTO) {
        VentaResponseDTO nuevoPedido = ventaService.crearPedido(ventaRequestDTO);
        return new ResponseEntity<>(nuevoPedido, HttpStatus.CREATED);
    }

    @Operation(summary = "Confirmar pago de venta", description = "Cambia el estado de una venta existente a PAGADO")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Pago confirmado exitosamente"),
        @ApiResponse(responseCode = "404", description = "Venta no encontrada")
    })
    @PutMapping("/{id}/confirmar")
    public ResponseEntity<VentaResponseDTO> confirmarPago(
            @Parameter(description = "ID numérico de la venta a confirmar", example = "1") 
            @PathVariable("id") Long id) {
        VentaResponseDTO ventaPagada = ventaService.confirmarPago(id);
        return ResponseEntity.ok(ventaPagada);
    }
}
