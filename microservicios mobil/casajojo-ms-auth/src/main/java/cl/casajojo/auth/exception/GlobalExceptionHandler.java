package cl.casajojo.auth.exception;

import java.util.HashMap;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Traduce las excepciones a respuestas JSON {"error", "mensaje"} con el código HTTP correcto.
 *
 * M5: antes TODA RuntimeException respondía 401 (incluso un correo duplicado o un
 * error de base de datos). Ahora cada caso tiene su código:
 *  - 400 datos inválidos      - 401 credenciales incorrectas
 *  - 403 sin permisos         - 409 correo/RUT ya registrado
 *  - 500 error inesperado (sin exponer detalles internos)
 */
@RestControllerAdvice  //intercepta las excepciones que ocurran en cualquier RestController
public class GlobalExceptionHandler {

    private ResponseEntity<Map<String, String>> respuesta(HttpStatus estado, String error, String mensaje) {
        Map<String, String> cuerpo = new HashMap<>();
        cuerpo.put("error", error);
        cuerpo.put("mensaje", mensaje);
        return new ResponseEntity<>(cuerpo, estado);
    }

    // 401: login con correo o contraseña incorrectos (mensaje genérico por ciberseguridad).
    @ExceptionHandler({CredencialesInvalidasException.class, BadCredentialsException.class})
    public ResponseEntity<Map<String, String>> handleCredenciales(RuntimeException ex) {
        return respuesta(HttpStatus.UNAUTHORIZED, "Credenciales inválidas (401)",
                "Usuario o contraseña incorrectos.");
    }

    // 409: el correo (o el RUT) ya está registrado.
    @ExceptionHandler(CorreoDuplicadoException.class)
    public ResponseEntity<Map<String, String>> handleDuplicado(CorreoDuplicadoException ex) {
        return respuesta(HttpStatus.CONFLICT, "Registro duplicado (409)", ex.getMessage());
    }

    // 404 (EFT): la cuenta del token ya no existe (por ejemplo, se eliminó).
    @ExceptionHandler(UsuarioNoEncontradoException.class)
    public ResponseEntity<Map<String, String>> handleUsuarioNoEncontrado(UsuarioNoEncontradoException ex) {
        return respuesta(HttpStatus.NOT_FOUND, "No encontrado (404)", ex.getMessage());
    }

    // 400: datos inválidos detectados en el servicio.
    @ExceptionHandler(SolicitudInvalidaException.class)
    public ResponseEntity<Map<String, String>> handleSolicitudInvalida(SolicitudInvalidaException ex) {
        return respuesta(HttpStatus.BAD_REQUEST, "Petición incorrecta (400)", ex.getMessage());
    }

    // 400: falla alguna validación del DTO (@NotBlank, @Email, @Pattern...).
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, String>> handleValidacion(MethodArgumentNotValidException ex) {
        String mensaje = ex.getBindingResult().getFieldErrors().isEmpty()
                ? "Datos inválidos"
                : ex.getBindingResult().getFieldErrors().get(0).getDefaultMessage();
        return respuesta(HttpStatus.BAD_REQUEST, "Datos inválidos (400)", mensaje);
    }

    // 400: petición sin body o con JSON mal formado.
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<Map<String, String>> handleMessageNotReadable(HttpMessageNotReadableException ex) {
        return respuesta(HttpStatus.BAD_REQUEST, "Petición incorrecta (400)",
                "Faltan datos en la petición o el formato es incorrecto. Verifique la información enviada.");
    }

    // 403: usuarios logeados pero sin permisos.
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<Map<String, String>> handleAccessDeniedException(AccessDeniedException ex) {
        return respuesta(HttpStatus.FORBIDDEN, "Acceso denegado (403)",
                "No tiene los privilegios suficientes para realizar esta acción. Contacte al administrador del sistema.");
    }

    // 500: cualquier otro error. No se envía ex.getMessage() para no filtrar detalles internos.
    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<Map<String, String>> handleRuntimeException(RuntimeException ex) {
        return respuesta(HttpStatus.INTERNAL_SERVER_ERROR, "Error interno (500)",
                "Ocurrió un error inesperado en el servicio de autenticación.");
    }
}
