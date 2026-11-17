package cl.elvolcan.catalogo.controller;

import cl.elvolcan.catalogo.assemblers.ProductoModelAssembler;
import cl.elvolcan.catalogo.dto.ProductoRequestDTO;
import cl.elvolcan.catalogo.dto.ProductoResponseDTO;
import cl.elvolcan.catalogo.model.Producto;
import cl.elvolcan.catalogo.repository.ProductoRepository;
import cl.elvolcan.catalogo.service.ProductoService;
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

@RestController
@RequestMapping("/api/v2/productos")
@RequiredArgsConstructor
@Tag(name = "Catálogo de Productos V2 (HATEOAS)", description = "Endpoints con hipermedia para el catálogo.")
public class ProductoControllerV2 {

    private final ProductoService productoService;
    private final ProductoRepository productoRepository; // Lo inyectamos para obtener las Entidades y pasarlas al Assembler
    private final ProductoModelAssembler assembler;

    @Operation(summary = "Crear un nuevo producto (V2)")
    @PreAuthorize("hasAnyAuthority('ADMIN', 'ROLE_ADMIN', 'SUPERVISOR', 'ROLE_SUPERVISOR')")
    @PostMapping
    public ResponseEntity<EntityModel<ProductoResponseDTO>> crear(@Valid @RequestBody ProductoRequestDTO request) {
        // Aprovechamos tu servicio existente
        productoService.crearProducto(request);
        
        // Buscamos la entidad recién creada para el assembler
        Producto nuevo = productoRepository.findBySku(request.getSku()).orElseThrow();
        
        return ResponseEntity.ok(assembler.toModel(nuevo));
    }

    @Operation(summary = "Listar productos activos (V2)")
    @GetMapping
    public ResponseEntity<CollectionModel<EntityModel<ProductoResponseDTO>>> listarTodos() {
        List<EntityModel<ProductoResponseDTO>> productos = productoRepository.findByActivoTrue().stream()
                .map(assembler::toModel)
                .collect(Collectors.toList());

        return ResponseEntity.ok(CollectionModel.of(productos,
                linkTo(methodOn(ProductoControllerV2.class).listarTodos()).withSelfRel()));
    }

    @Operation(summary = "Buscar producto por SKU (V2)")
    @GetMapping("/{sku}")
    public ResponseEntity<EntityModel<ProductoResponseDTO>> obtenerPorSku(@PathVariable String sku) {
        Producto producto = productoRepository.findBySku(sku)
                .orElseThrow(() -> new RuntimeException("Producto no encontrado"));
                
        return ResponseEntity.ok(assembler.toModel(producto));
    }

    @Operation(summary = "Desactivar producto (V2)")
    @PreAuthorize("hasAnyAuthority('ADMIN', 'ROLE_ADMIN', 'SUPERVISOR', 'ROLE_SUPERVISOR')")
    @DeleteMapping("/{sku}")
    public ResponseEntity<Void> desactivar(@PathVariable String sku) {
        productoService.desactivarProducto(sku);
        return ResponseEntity.noContent().build();
    }
}