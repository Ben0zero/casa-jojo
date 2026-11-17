package minimarket.produccion.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@ControllerAdvice
public class GlobalExceptionHandler {

    // Captura errores de Seguridad (Acceso denegado) 
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<Map<String, Object>> manejarAccessDenied(AccessDeniedException ex) {
        return buildResponse("No tiene los privilegios suficientes para realizar esta acción.", HttpStatus.FORBIDDEN);
    }

    // Captura errores de Autenticación (Token inválido o falta de token) 
    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<Map<String, Object>> manejarAuthentication(AuthenticationException ex) {
        return buildResponse("Token inválido o expirado. Por favor inicie sesión nuevamente.", HttpStatus.UNAUTHORIZED);
    }

    // Manejo de recurso no encontrado
    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<Map<String, Object>> manejarNotFound(ResourceNotFoundException ex) {
        return buildResponse(ex.getMessage(), HttpStatus.NOT_FOUND);
    }

    // Manejo general (Cualquier otro error desconocido)
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> manejarGeneral(Exception ex) {
        return buildResponse("Error interno: " + ex.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
    }

    // Método auxiliar para evitar repetir código
    private ResponseEntity<Map<String, Object>> buildResponse(String mensaje, HttpStatus status) {
        Map<String, Object> error = new HashMap<>();
        error.put("timestamp", LocalDateTime.now());
        error.put("mensaje", mensaje);
        error.put("status", status.value());
        return new ResponseEntity<>(error, status);
    }
}
