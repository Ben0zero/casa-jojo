package cl.casajojo.auth.service.correo;

import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

/** Modo "smtp": envía el correo real con el servidor configurado en el .env. */
public class EnviadorCorreoSmtp implements EnviadorCorreo {

    private final JavaMailSender mailSender;
    private final String remitente;

    public EnviadorCorreoSmtp(JavaMailSender mailSender, String remitente) {
        this.mailSender = mailSender;
        this.remitente = remitente;
    }

    @Override
    public void enviar(String destinatario, String asunto, String cuerpo) {
        SimpleMailMessage mensaje = new SimpleMailMessage();
        mensaje.setFrom(remitente);
        mensaje.setTo(destinatario);
        mensaje.setSubject(asunto);
        mensaje.setText(cuerpo);
        mailSender.send(mensaje);
    }

    @Override
    public String modo() {
        return "smtp";
    }
}
