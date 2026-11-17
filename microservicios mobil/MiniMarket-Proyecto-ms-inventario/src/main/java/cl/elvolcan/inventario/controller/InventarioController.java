package cl.elvolcan.inventario.controller;

import cl.elvolcan.inventario.dto.request.StockRequestDTO;
import cl.elvolcan.inventario.dto.response.StockResponseDTO;
import cl.elvolcan.inventario.service.InventarioService;

import jakarta.validation.Valid;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize; 
import org.springframework.web.bind.annotation.*;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;


import java.util.List;

@Tag(
    name = "Inventario",
    description = "Gestión de stock y control de inventario de productos"
)


@RestController
@RequestMapping("/api/inventario")
public class InventarioController {

    @Autowired
    private InventarioService service;

    // --- NUEVO ENDPOINT: Listar todo el inventario ---
    // Accesible para cualquier rol autenticado

    @Operation(
    summary = "Listar inventario",
    description = "Obtiene todos los registros de inventario disponibles"
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Inventario obtenido correctamente"),
        @ApiResponse(responseCode = "401", description = "No autorizado")
    })

    @GetMapping
    public ResponseEntity<List<StockResponseDTO>> listarTodo() {
        return ResponseEntity.ok(service.obtenerTodos());
    }

    // Crear producto: Solo ADMIN y VENDEDOR

    @Operation(
    summary = "Crear inventario",
    description = "Crea un nuevo registro de inventario para un producto existente"
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Inventario creado correctamente"),
        @ApiResponse(responseCode = "400", description = "Datos inválidos"),
        @ApiResponse(responseCode = "401", description = "No autorizado")
    })


    @PreAuthorize("hasAnyAuthority('ADMIN', 'ROLE_ADMIN', 'VENDEDOR', 'ROLE_VENDEDOR')")
    @PostMapping("/crear")
    public ResponseEntity<StockResponseDTO> crearProducto(
            @Valid @RequestBody StockRequestDTO request) {
        return ResponseEntity.ok(service.crearProducto(request));
    }

    // Aumentar stock manual: Solo ADMIN y VENDEDOR

    @Operation(
    summary = "Aumentar stock",
    description = "Incrementa la cantidad disponible de un producto"
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Operación realizada correctamente"),
        @ApiResponse(responseCode = "401", description = "No autorizado"),
        @ApiResponse(responseCode = "404", description = "Producto no encontrado")
    })

    @PreAuthorize("hasAnyAuthority('ADMIN', 'ROLE_ADMIN', 'VENDEDOR', 'ROLE_VENDEDOR')")
    @PutMapping("/aumentar")
    public ResponseEntity<StockResponseDTO> aumentarStock(
            @Valid @RequestBody StockRequestDTO request) {
        return ResponseEntity.ok(service.aumentarStock(request));
    }

    // Disminuir stock manual: Solo ADMIN y VENDEDOR

    @Operation(
    summary = "Disminuir stock",
    description = "Disminuye la cantidad disponible de un producto"
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Operación realizada correctamente"),
        @ApiResponse(responseCode = "401", description = "No autorizado"),
        @ApiResponse(responseCode = "404", description = "Producto no encontrado")
    })

    @PreAuthorize("hasAnyAuthority('ADMIN', 'ROLE_ADMIN', 'VENDEDOR', 'ROLE_VENDEDOR')")
    @PutMapping("/disminuir")
    public ResponseEntity<StockResponseDTO> disminuirStock(
            @Valid @RequestBody StockRequestDTO request) {
        return ResponseEntity.ok(service.disminuirStock(request));
    }

    // Buscar por SKU: Cualquier rol autenticado (Cliente, Vendedor, Admin)

    @Operation(
    summary = "Buscar producto por SKU",
    description = "Obtiene la información de inventario asociada a un SKU específico"
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Producto encontrado"),
        @ApiResponse(responseCode = "404", description = "Producto no encontrado"),
        @ApiResponse(responseCode = "401", description = "No autorizado")
    })

    @GetMapping("/{sku}")
    public ResponseEntity<StockResponseDTO> obtenerPorSku(@PathVariable String sku) {
        return ResponseEntity.ok(service.obtenerPorSku(sku));
    }

    // Stock crítico: Cualquier rol autenticado necesita ver alertas

    @Operation(
    summary = "Consultar stock crítico",
    description = "Obtiene los productos con stock bajo el mínimo permitido"
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Inventario obtenido correctamente"),
        @ApiResponse(responseCode = "401", description = "No autorizado")
    })

    @GetMapping("/critico")
    public ResponseEntity<List<StockResponseDTO>> obtenerStockCritico() {
        return ResponseEntity.ok(service.obtenerStockCritico());
    }

    // Sumar stock desde producción: Permitir a ADMIN, VENDEDOR y al proceso automático

    @Operation(
    summary = "Sumar stock desde producción",
    description = "Incrementa automáticamente el stock al registrar un lote de producción"
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Operación realizada correctamente"),
        @ApiResponse(responseCode = "401", description = "No autorizado"),
        @ApiResponse(responseCode = "404", description = "Producto no encontrado")
    })

    @PreAuthorize("hasAnyAuthority('ADMIN', 'ROLE_ADMIN', 'VENDEDOR', 'ROLE_VENDEDOR', 'PRODUCCION')")
    @PutMapping("/sumar-stock/{sku}/{cantidad}")
    public ResponseEntity<String> sumarStock(
            @PathVariable String sku,
            @PathVariable int cantidad
    ) {
        return ResponseEntity.ok(service.sumarStock(sku, cantidad));
    }
}