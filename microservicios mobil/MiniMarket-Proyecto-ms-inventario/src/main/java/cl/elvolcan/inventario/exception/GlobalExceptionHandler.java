package cl.elvolcan.inventario.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice // Intercepta las excepciones globales en los controladores
public class GlobalExceptionHandler {

    // FLUJO 1: Excepciones Personalizadas de Negocio (Inventario) 

    // Error 404: Cuando el SKU buscado no existe en las tablas de inventario
    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<Map<String, String>> handleNotFound(ResourceNotFoundException ex) {
        Map<String, String> error = new HashMap<>();
        error.put("error", "Recurso no encontrado (404)");
        error.put("mensaje", ex.getMessage());
        return new ResponseEntity<>(error, HttpStatus.NOT_FOUND);
    }

    // Error 400: Cuando intentan sacar más stock del que hay disponible
    @ExceptionHandler(InsufficientStockException.class)
    public ResponseEntity<Map<String, String>> handleInsufficient(InsufficientStockException ex) {
        Map<String, String> error = new HashMap<>();
        error.put("error", "Stock insuficiente (400)");
        error.put("mensaje", ex.getMessage());
        return new ResponseEntity<>(error, HttpStatus.BAD_REQUEST);
    }

    // Error 409: Conflicto cuando se intenta superar el nivel máximo permitido
    @ExceptionHandler(OverstockException.class)
    public ResponseEntity<Map<String, String>> handleOverstock(OverstockException ex) {
        Map<String, String> error = new HashMap<>();
        error.put("error", "Conflicto de sobre-stock (409)");
        error.put("mensaje", ex.getMessage());
        return new ResponseEntity<>(error, HttpStatus.CONFLICT);
    }


    // FLUJO 2: Seguridad y Sintaxis General (Estandarizado con Login/Catálogo) 

    // Error 403: Usuarios autenticados (con token legítimo) pero que NO tienen el rol permitido
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<Map<String, String>> handleAccessDeniedException(AccessDeniedException ex) {
        Map<String, String> error = new HashMap<>();
        error.put("error", "Acceso denegado (403)");
        error.put("mensaje", "No tiene los privilegios suficientes para modificar los registros de inventario. Operación restringida.");
        return new ResponseEntity<>(error, HttpStatus.FORBIDDEN);
    }

    // Error 400: Peticiones con JSON vacío o mal estructurado en Postman
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<Map<String, String>> handleMessageNotReadable(HttpMessageNotReadableException ex) {
        Map<String, String> error = new HashMap<>();
        error.put("error", "Petición incorrecta (400)");
        error.put("mensaje", "El formato del JSON es incorrecto o faltan datos esenciales. Verifique la información enviada.");
        return new ResponseEntity<>(error, HttpStatus.BAD_REQUEST);
    }

    // Error 500/Otros: Un capturador genérico por si salta cualquier otro RuntimeException imprevisto
    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<Map<String, String>> handleRuntimeException(RuntimeException ex) {
        Map<String, String> error = new HashMap<>();
        error.put("error", "Error interno de procesamiento (400)");
        error.put("mensaje", ex.getMessage());
        return new ResponseEntity<>(error, HttpStatus.BAD_REQUEST);
    }
}
