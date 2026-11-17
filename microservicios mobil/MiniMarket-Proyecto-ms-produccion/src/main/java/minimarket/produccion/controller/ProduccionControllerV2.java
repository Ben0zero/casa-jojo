package minimarket.produccion.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import minimarket.produccion.assembler.ProduccionModelAssembler;
import minimarket.produccion.dto.request.ProduccionRequestDTO;
import minimarket.produccion.dto.response.ProduccionResponseDTO;
import minimarket.produccion.service.ProduccionService;
import org.springframework.hateoas.CollectionModel;
import org.springframework.hateoas.EntityModel;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

@Tag(name = "Producción V2 (HATEOAS)", description = "Gestión de lotes de producción con hipermedia HATEOAS")
@RestController
@RequestMapping("/api/v2/produccion")
@RequiredArgsConstructor
public class ProduccionControllerV2 {

    private final ProduccionService service;
    private final ProduccionModelAssembler assembler;

    @Operation(summary = "Crear lote de producción (V2)", description = "Registra un nuevo lote y devuelve el DTO con enlaces")
    @PostMapping
    public ResponseEntity<EntityModel<ProduccionResponseDTO>> crear(@Valid @RequestBody ProduccionRequestDTO dto) {
        ProduccionResponseDTO response = service.crearLote(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(assembler.toModel(response));
    }

    @Operation(summary = "Listar lotes (V2)", description = "Obtiene todos los lotes envueltos en una colección HATEOAS")
    @GetMapping
    public ResponseEntity<CollectionModel<EntityModel<ProduccionResponseDTO>>> listar() {
        List<EntityModel<ProduccionResponseDTO>> lotes = service.obtenerTodos().stream()
                .map(assembler::toModel)
                .collect(Collectors.toList());

        return ResponseEntity.ok(CollectionModel.of(lotes,
                linkTo(methodOn(ProduccionControllerV2.class).listar()).withSelfRel()));
    }

    @Operation(summary = "Buscar lote por ID (V2)")
    @GetMapping("/{id}")
    public ResponseEntity<EntityModel<ProduccionResponseDTO>> buscarPorId(@PathVariable Long id) {
        ProduccionResponseDTO response = service.buscarPorId(id);
        return ResponseEntity.ok(assembler.toModel(response));
    }

    @Operation(summary = "Eliminar lote (V2)")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {
        service.eliminar(id);
        return ResponseEntity.noContent().build();
    }
}