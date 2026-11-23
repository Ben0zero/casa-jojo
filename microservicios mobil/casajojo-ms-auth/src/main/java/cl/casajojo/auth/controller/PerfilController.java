package cl.casajojo.auth.controller;

import cl.casajojo.auth.dto.ActualizarPerfilDTO;
import cl.casajojo.auth.dto.CambiarPasswordDTO;
import cl.casajojo.auth.dto.MensajeDTO;
import cl.casajojo.auth.dto.UsuarioResponseDTO;
import cl.casajojo.auth.service.PerfilService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Locale;

/**
 * EFT: CRUD del perfil del propio cliente. Todas las rutas exigen su JWT; el correo
 * se toma del token (sub), así un cliente solo puede tocar su propia cuenta.
 */
@RestController
@RequestMapping("/api/usuarios/me")
@Tag(name = "Mi perfil", description = "Leer, editar, cambiar contraseña y eliminar la cuenta propia")
public class PerfilController {

    private final PerfilService perfil;

    public PerfilController(PerfilService perfil) {
        this.perfil = perfil;
    }

    @Operation(summary = "Ver mi perfil")
    @GetMapping
    public ResponseEntity<UsuarioResponseDTO> obtener(Authentication auth) {
        return ResponseEntity.ok(perfil.obtener(correo(auth)));
    }

    @Operation(summary = "Editar nombre y teléfono")
    @PutMapping
    public ResponseEntity<UsuarioResponseDTO> actualizar(@Valid @RequestBody ActualizarPerfilDTO datos,
                                                         Authentication auth) {
        return ResponseEntity.ok(perfil.actualizar(correo(auth), datos));
    }

    @Operation(summary = "Cambiar contraseña", description = "Pide la contraseña actual y la nueva.")
    @PutMapping("/password")
    public ResponseEntity<MensajeDTO> cambiarPassword(@Valid @RequestBody CambiarPasswordDTO datos,
                                                      Authentication auth) {
        perfil.cambiarPassword(correo(auth), datos.getPasswordActual(), datos.getNuevaPassword());
        return ResponseEntity.ok(new MensajeDTO("Contraseña actualizada"));
    }

    @Operation(summary = "Eliminar mi cuenta", description = "Borra la cuenta del cliente del token.")
    @DeleteMapping
    public ResponseEntity<Void> eliminar(Authentication auth) {
        perfil.eliminar(correo(auth));
        return ResponseEntity.noContent().build();
    }

    private String correo(Authentication auth) {
        if (auth == null || auth.getName() == null) {
            throw new AccessDeniedException("Se requiere iniciar sesión");
        }
        return auth.getName().trim().toLowerCase(Locale.ROOT);
    }
}
