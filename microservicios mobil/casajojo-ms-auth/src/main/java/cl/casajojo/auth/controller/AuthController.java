package cl.casajojo.auth.controller;

import cl.casajojo.auth.dto.LoginRequestDTO;
import cl.casajojo.auth.dto.MensajeDTO;
import cl.casajojo.auth.dto.RecuperarRequestDTO;
import cl.casajojo.auth.dto.RestablecerRequestDTO;
import cl.casajojo.auth.service.RecuperacionService;
import cl.casajojo.auth.dto.RegistroRequestDTO;
import cl.casajojo.auth.dto.UsuarioResponseDTO;
import cl.casajojo.auth.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@Tag(name = "Autenticación Casa JoJo", description = "Login y registro por correo para la app móvil")
public class AuthController {

    private final AuthService authService;
    private final RecuperacionService recuperacionService;

    public AuthController(AuthService authService, RecuperacionService recuperacionService) {
        this.authService = authService;
        this.recuperacionService = recuperacionService;
    }

    @Operation(summary = "Iniciar sesión", description = "Recibe correo y contraseña; devuelve los datos del cliente y su token JWT.")
    @SecurityRequirements()
    @PostMapping("/login")
    public ResponseEntity<UsuarioResponseDTO> login(@Valid @RequestBody LoginRequestDTO request) {
        return ResponseEntity.ok(authService.login(request));
    }

    @Operation(summary = "Registrar cliente", description = "Crea una cuenta de La Casa JoJo con nombre, correo, teléfono y contraseña.")
    @SecurityRequirements()
    @PostMapping("/register")
    public ResponseEntity<UsuarioResponseDTO> registrar(@Valid @RequestBody RegistroRequestDTO request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(authService.registrar(request));
    }

    @Operation(summary = "Recuperar contraseña",
            description = "Envía al correo un código de 6 dígitos (un solo uso, vence en 15 min). "
                    + "Responde igual exista o no la cuenta.")
    @SecurityRequirements()
    @PostMapping("/recuperar")
    public ResponseEntity<MensajeDTO> recuperar(@Valid @RequestBody RecuperarRequestDTO request) {
        recuperacionService.solicitar(request.getCorreo());
        return ResponseEntity.ok(new MensajeDTO(
                "Si el correo está registrado, te enviamos un código para restablecer tu contraseña"));
    }

    @Operation(summary = "Restablecer contraseña", description = "Recibe correo, código y nueva contraseña.")
    @SecurityRequirements()
    @PostMapping("/restablecer")
    public ResponseEntity<MensajeDTO> restablecer(@Valid @RequestBody RestablecerRequestDTO request) {
        recuperacionService.restablecer(request.getCorreo(), request.getCodigo(), request.getNuevaPassword());
        return ResponseEntity.ok(new MensajeDTO("Contraseña actualizada. Ya puedes iniciar sesión"));
    }
}
