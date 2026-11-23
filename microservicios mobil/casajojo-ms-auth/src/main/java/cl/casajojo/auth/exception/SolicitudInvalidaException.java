package cl.casajojo.auth.exception;

/** M5: datos de entrada inválidos detectados en el servicio. El GlobalExceptionHandler la traduce a 400. */
public class SolicitudInvalidaException extends RuntimeException {

    public SolicitudInvalidaException(String mensaje) {
        super(mensaje);
    }
}
