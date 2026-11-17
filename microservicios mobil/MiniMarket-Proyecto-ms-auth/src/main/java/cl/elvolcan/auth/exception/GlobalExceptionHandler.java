package cl.elvolcan.auth.exception;

import java.util.HashMap;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice  //intercepta las excepciones que ocurran en cualquier RestController
public class GlobalExceptionHandler {
    
    //1er flujo: aca atrapa el error por si sale RuntimeException por credenciales e inicios de sesion incorrectos, se ejecuta el metodo de abajo
    @ExceptionHandler(RuntimeException.class)
    //el responseEntity permite controlar tanto el cuerpo del msje y el codigo de estado HTTP
    // se usa un mapa para que Sprin lo convierta en JSON
    // el hashMap es una estruc de datos para guardar info en pared de llave y valor (aca serie la llave el msje y el valor "usuario incorrecto")
    public ResponseEntity<Map<String, String>> handleRuntimeException(RuntimeException ex) {
        Map<String, String> error = new HashMap<>();
        error.put("mensaje", ex.getMessage());

        //aca se devuelve un 401 (no autorizado) en vez de un 403 o 500
        return new ResponseEntity<>(error, HttpStatus.UNAUTHORIZED);
    }


    //2do flujo: usuarios logeados pero NO tienen permisos
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<Map<String, String>> handleAccessDeniedException(AccessDeniedException ex) {
        Map<String, String> error = new HashMap<>();
        error.put("error", "Acceso denegado (403)");
        error.put("mensaje", "No tiene los privilegios suficientes para realizar esta acción. Contacte al administrador del sistema.");

        // Devuelve un 403 + msje para Postman
        return new ResponseEntity<>(error, HttpStatus.FORBIDDEN);
    }


    // 3er Flujo: para errores de Login (Usuario no existe o mala contraseña) ---
    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<Map<String, String>> handleBadCredentials(BadCredentialsException ex) {
        Map<String, String> error = new HashMap<>();
        error.put("error", "Credenciales inválidas (401)");
        error.put("mensaje", "Usuario o contraseña incorrectos."); // mensaje generico por ciberseguridad

        return new ResponseEntity<>(error, HttpStatus.UNAUTHORIZED);
    }

    // 4to Flujo: para peticiones sin Body o JSON mal formado
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<Map<String, String>> handleMessageNotReadable(HttpMessageNotReadableException ex) {
        Map<String, String> error = new HashMap<>();
        
        // Usamos BAD_REQUEST (400) porque el error es de sintaxis del cliente, no de credenciales.
        error.put("error", "Petición incorrecta (400)"); 
        error.put("mensaje", "Faltan datos en la petición o el formato es incorrecto. Verifique la información enviada.");

        return new ResponseEntity<>(error, HttpStatus.BAD_REQUEST);
    }

}
