package cl.casajojo.auth.service.correo;

/**
 * EFT: abstracción del envío de correos (polimorfismo): hay una implementación
 * SMTP y otra de "log" para la demo. CorreoConfig decide cuál usar según el .env.
 */
public interface EnviadorCorreo {

    void enviar(String destinatario, String asunto, String cuerpo);

    /** Nombre del modo, para mostrarlo en la consola al arrancar ("smtp" o "log"). */
    String modo();
}
