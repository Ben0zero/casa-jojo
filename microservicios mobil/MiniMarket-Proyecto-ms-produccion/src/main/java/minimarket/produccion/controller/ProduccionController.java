package minimarket.produccion.controller;



import minimarket.produccion.dto.request.ProduccionRequestDTO;
import minimarket.produccion.dto.response.ProduccionResponseDTO;
import minimarket.produccion.service.ProduccionService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;

import java.util.List;

@Tag(
    name = "Producción",
    description = "Gestión de lotes de producción y sincronización con inventario"
)
@RestController
@RequestMapping("/api/produccion")
public class ProduccionController {

    @Autowired
    private ProduccionService service;

    // Crear lote de producción
    @Operation(
    summary = "Crear lote de producción",
    description = "Registra un nuevo lote de producción y actualiza el inventario"
    )
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Lote creado correctamente"),
        @ApiResponse(responseCode = "401", description = "No autorizado")
    })
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ProduccionResponseDTO crear(
            @RequestBody ProduccionRequestDTO dto) {

        return service.crearLote(dto);
    }

    // Obtener todos los lotes
    @Operation(
    summary = "Listar lotes",
    description = "Obtiene todos los lotes de producción registrados"
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Listado obtenido correctamente"),
        @ApiResponse(responseCode = "401", description = "No autorizado")
    })
    @GetMapping
    public List<ProduccionResponseDTO> listar() {
        return service.obtenerTodos();
    }

    // Buscar lote por ID
    @Operation(
    summary = "Buscar lote por ID",
    description = "Obtiene la información de un lote específico"
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Lote encontrado"),
        @ApiResponse(responseCode = "404", description = "Lote no encontrado")
    })
    @GetMapping("/{id}")
    public ProduccionResponseDTO buscarPorId(
            @PathVariable Long id) {

        return service.buscarPorId(id);
    }

    // Eliminar lote
    @Operation(
    summary = "Eliminar lote",
    description = "Elimina un lote de producción existente"
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Lote eliminado correctamente"),
        @ApiResponse(responseCode = "404", description = "Lote no encontrado"),
        @ApiResponse(responseCode = "401", description = "No autorizado")
    })
    @DeleteMapping("/{id}")
    public void eliminar(@PathVariable Long id) {
        service.eliminar(id);
    }
}