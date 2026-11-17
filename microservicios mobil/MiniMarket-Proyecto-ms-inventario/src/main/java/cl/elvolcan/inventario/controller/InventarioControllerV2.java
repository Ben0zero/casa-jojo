package cl.elvolcan.inventario.controller;

import cl.elvolcan.inventario.assembler.InventarioModelAssembler;
import cl.elvolcan.inventario.dto.request.StockRequestDTO;
import cl.elvolcan.inventario.dto.response.StockResponseDTO;
import cl.elvolcan.inventario.service.InventarioService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.hateoas.CollectionModel;
import org.springframework.hateoas.EntityModel;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

@Tag(name = "Inventario V2 (HATEOAS)", description = "Gestión de stock con hipermedia HATEOAS")
@RestController
@RequestMapping("/api/v2/inventario")
@RequiredArgsConstructor
public class InventarioControllerV2 {

    private final InventarioService service;
    private final InventarioModelAssembler assembler;

    @Operation(summary = "Listar inventario (V2)")
    @GetMapping
    public ResponseEntity<CollectionModel<EntityModel<StockResponseDTO>>> listarTodo() {
        List<EntityModel<StockResponseDTO>> inventario = service.obtenerTodos().stream()
                .map(assembler::toModel)
                .collect(Collectors.toList());

        return ResponseEntity.ok(CollectionModel.of(inventario,
                linkTo(methodOn(InventarioControllerV2.class).listarTodo()).withSelfRel()));
    }

    @Operation(summary = "Crear inventario (V2)")
    @PreAuthorize("hasAnyAuthority('ADMIN', 'ROLE_ADMIN', 'VENDEDOR', 'ROLE_VENDEDOR')")
    @PostMapping("/crear")
    public ResponseEntity<EntityModel<StockResponseDTO>> crearProducto(@Valid @RequestBody StockRequestDTO request) {
        StockResponseDTO response = service.crearProducto(request);
        return ResponseEntity.ok(assembler.toModel(response));
    }

    @Operation(summary = "Aumentar stock (V2)")
    @PreAuthorize("hasAnyAuthority('ADMIN', 'ROLE_ADMIN', 'VENDEDOR', 'ROLE_VENDEDOR')")
    @PutMapping("/aumentar")
    public ResponseEntity<EntityModel<StockResponseDTO>> aumentarStock(@Valid @RequestBody StockRequestDTO request) {
        StockResponseDTO response = service.aumentarStock(request);
        return ResponseEntity.ok(assembler.toModel(response));
    }

    @Operation(summary = "Disminuir stock (V2)")
    @PreAuthorize("hasAnyAuthority('ADMIN', 'ROLE_ADMIN', 'VENDEDOR', 'ROLE_VENDEDOR')")
    @PutMapping("/disminuir")
    public ResponseEntity<EntityModel<StockResponseDTO>> disminuirStock(@Valid @RequestBody StockRequestDTO request) {
        StockResponseDTO response = service.disminuirStock(request);
        return ResponseEntity.ok(assembler.toModel(response));
    }

    @Operation(summary = "Buscar producto por SKU (V2)")
    @GetMapping("/{sku}")
    public ResponseEntity<EntityModel<StockResponseDTO>> obtenerPorSku(@PathVariable String sku) {
        StockResponseDTO response = service.obtenerPorSku(sku);
        return ResponseEntity.ok(assembler.toModel(response));
    }

    @Operation(summary = "Consultar stock crítico (V2)")
    @GetMapping("/critico")
    public ResponseEntity<CollectionModel<EntityModel<StockResponseDTO>>> obtenerStockCritico() {
        List<EntityModel<StockResponseDTO>> critico = service.obtenerStockCritico().stream()
                .map(assembler::toModel)
                .collect(Collectors.toList());

        return ResponseEntity.ok(CollectionModel.of(critico,
                linkTo(methodOn(InventarioControllerV2.class).obtenerStockCritico()).withSelfRel()));
    }

    @Operation(summary = "Sumar stock desde producción (V2)")
    @PreAuthorize("hasAnyAuthority('ADMIN', 'ROLE_ADMIN', 'VENDEDOR', 'ROLE_VENDEDOR', 'PRODUCCION')")
    @PutMapping("/sumar-stock/{sku}/{cantidad}")
    public ResponseEntity<String> sumarStock(@PathVariable String sku, @PathVariable int cantidad) {
        // Al ser una simple cadena de texto de confirmación, se mantiene la respuesta estándar
        return ResponseEntity.ok(service.sumarStock(sku, cantidad));
    }
}