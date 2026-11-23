package cl.casajojo.catalogo.controller;

import cl.casajojo.catalogo.dto.DisponibilidadDTO;
import cl.casajojo.catalogo.dto.PlatoMenuDTO;
import cl.casajojo.catalogo.service.MenuService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Menú de La Casa JoJo. Lectura pública (app Android); cambiar la disponibilidad
 * requiere rol ADMIN o COCINA (ver SecurityConfig).
 */
@RestController
@RequestMapping("/api/menu")
@RequiredArgsConstructor
@Tag(name = "Menú Casa JoJo", description = "Platos disponibles para la app del cliente")
public class MenuController {

    private final MenuService menuService;

    @Operation(summary = "Listar el menú", description = "Devuelve proteínas y acompañamientos con su precio y disponibilidad.")
    @GetMapping
    public ResponseEntity<List<PlatoMenuDTO>> listar() {
        return ResponseEntity.ok(menuService.obtenerMenu());
    }

    @Operation(summary = "Buscar un plato por id")
    @GetMapping("/{id}")
    public ResponseEntity<PlatoMenuDTO> porId(@PathVariable Long id) {
        return ResponseEntity.ok(menuService.obtenerPorId(id));
    }

    @Operation(summary = "Marcar disponible o agotado", description = "Solo ADMIN o COCINA. Body: {\"disponible\": false}")
    @PutMapping("/{id}/disponible")
    public ResponseEntity<PlatoMenuDTO> cambiarDisponibilidad(@PathVariable Long id,
                                                              @Valid @RequestBody DisponibilidadDTO body) {
        return ResponseEntity.ok(menuService.cambiarDisponibilidad(id, body.getDisponible()));
    }
}
