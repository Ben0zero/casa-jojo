package cl.casajojo.catalogo.exception;

/**
 * B1: el plato pedido no existe o no pertenece al menú Casa JoJo.
 * Extiende IllegalArgumentException por compatibilidad con el código y las
 * pruebas existentes; el GlobalExceptionHandler la traduce a 404.
 */
public class PlatoNoEncontradoException extends IllegalArgumentException {

    public PlatoNoEncontradoException(String mensaje) {
        super(mensaje);
    }
}
