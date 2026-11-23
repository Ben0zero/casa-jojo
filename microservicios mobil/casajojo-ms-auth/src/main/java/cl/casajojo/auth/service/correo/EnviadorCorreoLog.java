package cl.casajojo.auth.service.correo;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Modo "log": sin SMTP configurado, el correo se escribe en la consola del servicio.
 * Sirve para la demo (el código aparece en `docker compose logs ms-auth`).
 */
public class EnviadorCorreoLog implements EnviadorCorreo {

    private static final Logger log = LoggerFactory.getLogger(EnviadorCorreoLog.class);

    @Override
    public void enviar(String destinatario, String asunto, String cuerpo) {
        log.warn("[CORREO modo log] Para: {} | Asunto: {}\n{}", destinatario, asunto, cuerpo);
    }

    @Override
    public String modo() {
        return "log";
    }
}
