package cl.elvolcan.ventas.exception;

import lombok.Data;
import java.time.LocalDateTime;
import java.util.Map;

@Data
public class ErrorResponse {
    private LocalDateTime timestamp;
    private int status;
    private String error;
    private String message;
    private Map<String, String> validationErrors;
    private String path;

    public ErrorResponse() {
        this.timestamp = LocalDateTime.now();
    }
}