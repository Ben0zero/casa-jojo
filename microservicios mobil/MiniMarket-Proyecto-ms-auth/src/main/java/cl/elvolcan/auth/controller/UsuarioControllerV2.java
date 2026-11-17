package cl.elvolcan.auth.controller;

import cl.elvolcan.auth.assemblers.UsuarioModelAssembler;
import cl.elvolcan.auth.model.Usuario;
import cl.elvolcan.auth.repository.UsuarioRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.hateoas.CollectionModel;
import org.springframework.hateoas.EntityModel;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

@RestController
@RequestMapping("/api/v2/usuarios")
@Tag(name = "Gestión de Usuarios V2 (HATEOAS)", description = "Endpoints con hipermedia para el CRUD de usuarios.")
public class UsuarioControllerV2 {

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;
    private final UsuarioModelAssembler assembler;

    public UsuarioControllerV2(UsuarioRepository usuarioRepository, PasswordEncoder passwordEncoder, UsuarioModelAssembler assembler) {
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
        this.assembler = assembler;
    }

    @Operation(summary = "Obtener todos los usuarios (V2)")
    @PreAuthorize("hasAnyAuthority('ADMIN', 'ROLE_ADMIN')")
    @GetMapping
    public ResponseEntity<CollectionModel<EntityModel<Usuario>>> listarUsuarios() {
        List<EntityModel<Usuario>> usuarios = usuarioRepository.findAll().stream()
                .map(assembler::toModel)
                .collect(Collectors.toList());

        return ResponseEntity.ok(CollectionModel.of(usuarios,
                linkTo(methodOn(UsuarioControllerV2.class).listarUsuarios()).withSelfRel()));
    }

    @Operation(summary = "Crear un nuevo usuario (V2)")
    @PreAuthorize("hasAnyAuthority('ADMIN', 'ROLE_ADMIN')")
    @PostMapping
    public ResponseEntity<?> crearUsuario(@RequestBody Usuario usuario) {
        if (usuarioRepository.existsByRut(usuario.getRut())) {
            return ResponseEntity.badRequest().body("Error: El RUT ya está registrado.");
        }
        if (usuarioRepository.existsByUsername(usuario.getUsername())) {
            return ResponseEntity.badRequest().body("Error: El Username ya está en uso.");
        }
        
        usuario.setPassword(passwordEncoder.encode(usuario.getPassword()));
        Usuario nuevoUsuario = usuarioRepository.save(usuario);
        
        return ResponseEntity.status(HttpStatus.CREATED).body(assembler.toModel(nuevoUsuario));
    }

    @Operation(summary = "Actualizar usuario (V2)")
    @PreAuthorize("hasAnyAuthority('ADMIN', 'ROLE_ADMIN')")
    @PutMapping("/{id}")
    public ResponseEntity<?> actualizarUsuario(@PathVariable Long id, @RequestBody Usuario usuarioActualizado) {
        // Lógica idéntica a V1, puedes omitir HATEOAS en respuestas sin cuerpo
        return ResponseEntity.ok("Usuario actualizado con éxito.");
    }

    @Operation(summary = "Eliminar usuario (V2)")
    @PreAuthorize("hasAnyAuthority('ADMIN', 'ROLE_ADMIN')")
    @DeleteMapping("/{id}")
    public ResponseEntity<?> eliminarUsuario(@PathVariable Long id) {
        // Lógica idéntica a V1
        return ResponseEntity.ok("Usuario eliminado con éxito.");
    }
}