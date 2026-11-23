package cl.casajojo.auth.config;

import cl.casajojo.auth.service.correo.EnviadorCorreo;
import cl.casajojo.auth.service.correo.EnviadorCorreoLog;
import cl.casajojo.auth.service.correo.EnviadorCorreoSmtp;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.mail.javamail.JavaMailSender;

/**
 * EFT: elige cómo se envían los correos. Con SMTP_HOST definido en el .env se usa
 * SMTP; si no, el modo "log" escribe el código en la consola (solo demo).
 */
@Configuration
public class CorreoConfig {

    private static final Logger log = LoggerFactory.getLogger(CorreoConfig.class);

    @Bean
    EnviadorCorreo enviadorCorreo(@Value("${spring.mail.host:}") String host,
                                  @Value("${casajojo.correo.remitente:no-responder@casajojo.cl}") String remitente,
                                  ObjectProvider<JavaMailSender> mailSender) {
        JavaMailSender sender = mailSender.getIfAvailable();
        EnviadorCorreo enviador = (host == null || host.isBlank() || sender == null)
                ? new EnviadorCorreoLog()
                : new EnviadorCorreoSmtp(sender, remitente);
        log.info("Recuperación de contraseña: envío de correos en modo '{}'", enviador.modo());
        return enviador;
    }
}
