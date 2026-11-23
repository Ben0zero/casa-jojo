package cl.casajojo.auth.exception;

/** M5: correo o contraseña incorrectos. El GlobalExceptionHandler la traduce a 401. */
public class CredencialesInvalidasException extends RuntimeException {

    public CredencialesInvalidasException() {
        // Mensaje genérico a propósito: no revela si el correo existe.
        super("Error: Usuario o contraseña incorrectos");
    }
}
