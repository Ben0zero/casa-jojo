
package cl.elvolcan.catalogo.controller;

import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import cl.elvolcan.catalogo.dto.ProductoRequestDTO;
import cl.elvolcan.catalogo.dto.ProductoResponseDTO;
import cl.elvolcan.catalogo.service.ProductoService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/productos")
@RequiredArgsConstructor
@Tag(name = "Catálogo de Productos", description = "Endpoints para gestionar los productos de la tienda. Requieren token JWT.")
public class ProductoController {

    private final ProductoService productoService;

    @Operation(summary = "Crear un nuevo producto", description = "Registra un producto en el catálogo y notifica al microservicio de Inventario. Requiere rol ADMIN o SUPERVISOR.")
    @ApiResponse(responseCode = "200", description = "Producto creado exitosamente")
    @ApiResponse(responseCode = "400", description = "Datos inválidos o SKU duplicado")
    @PreAuthorize("hasAnyAuthority('ADMIN', 'ROLE_ADMIN', 'SUPERVISOR', 'ROLE_SUPERVISOR')")
    // CREAR: Solo ADMIN y SUPERVISOR
    @PostMapping
    public ResponseEntity<ProductoResponseDTO> crear(@Valid @RequestBody ProductoRequestDTO request) {
        return ResponseEntity.ok(productoService.crearProducto(request));
    }

    @Operation(summary = "Listar productos activos", description = "Retorna todos los productos que están marcados como activos para la venta. Requiere estar autenticado.")
    // LISTAR TODOS: Cualquier usuario autenticado (con token válido)
    @GetMapping
    public ResponseEntity<List<ProductoResponseDTO>> listarTodos() {
        return ResponseEntity.ok(productoService.obtenerTodosActivos());
    }

    @Operation(summary = "Buscar producto por SKU", description = "Busca los detalles de un producto específico mediante su código SKU. Requiere estar autenticado.")
    @ApiResponse(responseCode = "200", description = "Producto encontrado")
    @ApiResponse(responseCode = "400", description = "Producto no encontrado")
    @GetMapping("/{sku}")
    // BUSCAR POR SKU: Cualquier usuario autenticado (con token válido)
    public ResponseEntity<ProductoResponseDTO> obtenerPorSku(@PathVariable String sku) {
        return ResponseEntity.ok(productoService.obtenerPorSku(sku));
    }

    @Operation(summary = "Buscar producto por ID", description = "Busca los detalles de un producto mediante su ID numérico.")
    @ApiResponse(responseCode = "200", description = "Producto encontrado")
    @ApiResponse(responseCode = "404", description = "Producto no encontrado")
    @GetMapping("/id/{id}")
    public ResponseEntity<ProductoResponseDTO> obtenerPorId(@PathVariable Long id) {
        // Aquí asumo que en tu servicio tienes un método llamado obtenerPorId(id)
        return ResponseEntity.ok(productoService.obtenerPorId(id));
    }

    @Operation(summary = "Desactivar producto (Borrado Lógico)", description = "Pausa las ventas de un producto sin eliminarlo de la base de datos. Requiere rol ADMIN o SUPERVISOR.")
    @ApiResponse(responseCode = "204", description = "Producto desactivado con éxito")
    @PreAuthorize("hasAnyAuthority('ADMIN', 'ROLE_ADMIN', 'SUPERVISOR', 'ROLE_SUPERVISOR')")
    @DeleteMapping("/{sku}")
    // DESACTIVAR (Eliminar lógico): Solo ADMIN y SUPERVISOR
    public ResponseEntity<Void> desactivar(@PathVariable String sku) {
        productoService.desactivarProducto(sku);
        return ResponseEntity.noContent().build();
    }
}

