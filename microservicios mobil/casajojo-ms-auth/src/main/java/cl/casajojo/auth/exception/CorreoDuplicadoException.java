package cl.casajojo.auth.exception;

/** M5: ya existe una cuenta con ese correo (o RUT). El GlobalExceptionHandler la traduce a 409. */
public class CorreoDuplicadoException extends RuntimeException {

    public CorreoDuplicadoException(String mensaje) {
        super(mensaje);
    }
}
