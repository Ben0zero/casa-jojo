package cl.casajojo.ventas.exception;

/**
 * M2: cambio de estado no permitido (por ejemplo ENTREGADO → RECIBIDO, o cancelar
 * fuera del plazo de 15 s). Extiende IllegalStateException, que el
 * GlobalExceptionHandler ya traduce a 409 Conflict.
 */
public class TransicionInvalidaException extends IllegalStateException {

    public TransicionInvalidaException(String mensaje) {
        super(mensaje);
    }
}
