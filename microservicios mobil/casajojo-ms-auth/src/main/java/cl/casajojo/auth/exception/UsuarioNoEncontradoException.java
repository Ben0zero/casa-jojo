package cl.casajojo.auth.exception;

/** EFT: la cuenta del token ya no existe. El GlobalExceptionHandler la traduce a 404. */
public class UsuarioNoEncontradoException extends RuntimeException {

    public UsuarioNoEncontradoException() {
        super("La cuenta no existe");
    }
}
