package cl.elvolcan.catalogo.controller;

import cl.elvolcan.catalogo.dto.PlatoMenuDTO;
import cl.elvolcan.catalogo.service.MenuService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** Menú de La Casa JoJo consumido por la app Android (sin token, solo lectura). */
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
}
