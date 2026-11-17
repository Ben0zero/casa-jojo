package cl.elvolcan.catalogo.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    // 1er Flujo: Errores de negocio (Ej: "Producto no encontrado con SKU: URB-001")
    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<Map<String, String>> handleRuntimeException(RuntimeException ex) {
        Map<String, String> response = new HashMap<>();
        // Atrapa los throw new RuntimeException que tienes en tu ProductoService
        response.put("error", "Error de procesamiento (400)");
        response.put("mensaje", ex.getMessage()); 
        
        return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
    }

    // 2do Flujo: Usuarios logeados pero SIN permisos (Ej: Panadero intentando crear producto)
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<Map<String, String>> handleAccessDeniedException(AccessDeniedException ex) {
        Map<String, String> error = new HashMap<>();
        error.put("error", "Acceso denegado (403)");
        error.put("mensaje", "No tiene los privilegios suficientes para modificar el catálogo de productos.");

        return new ResponseEntity<>(error, HttpStatus.FORBIDDEN);
    }

    // 3er Flujo: JSON mal formado, vacío, o con errores de sintaxis
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<Map<String, String>> handleMessageNotReadable(HttpMessageNotReadableException ex) {
        Map<String, String> error = new HashMap<>();
        error.put("error", "Petición incorrecta (400)");
        error.put("mensaje", "El formato del JSON es incorrecto o faltan datos. Verifique la información enviada.");

        return new ResponseEntity<>(error, HttpStatus.BAD_REQUEST);
    }

    // 4to Flujo: Validaciones de campos (Falla el @Valid de tu ProductoRequestDTO)
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, String>> handleValidationExceptions(MethodArgumentNotValidException ex) {
        Map<String, String> error = new HashMap<>();
        error.put("error", "Datos inválidos (400)");
        
        // Captura el mensaje específico del campo que falló en la validación
        String mensajeValidacion = ex.getBindingResult().getFieldErrors().get(0).getDefaultMessage();
        error.put("mensaje", "Validación fallida: " + mensajeValidacion);

        return new ResponseEntity<>(error, HttpStatus.BAD_REQUEST);
    }
}
