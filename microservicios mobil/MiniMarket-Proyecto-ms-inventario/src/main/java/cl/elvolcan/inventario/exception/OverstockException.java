package cl.elvolcan.inventario.exception;

public class OverstockException extends RuntimeException {

    public OverstockException(String message) {
        super(message);
    }
}
