package cl.casajojo.auth.service;

import cl.casajojo.auth.exception.SolicitudInvalidaException;
import cl.casajojo.auth.model.CodigoRecuperacion;
import cl.casajojo.auth.model.Usuario;
import cl.casajojo.auth.repository.CodigoRecuperacionRepository;
import cl.casajojo.auth.repository.UsuarioRepository;
import cl.casajojo.auth.service.correo.EnviadorCorreo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Locale;
import java.util.Optional;

/**
 * EFT: recuperación de contraseña con un código de un solo uso.
 *
 * 1) POST /api/auth/recuperar: genera un código de 6 dígitos que vence en 15 minutos,
 *    guarda solo su hash y lo envía por correo. Responde lo mismo exista o no la cuenta
 *    (así no se puede averiguar qué correos están registrados).
 * 2) POST /api/auth/restablecer: valida el código y cambia la contraseña. Tras 5
 *    intentos fallidos el código deja de servir.
 */
@Service
public class RecuperacionService {

    private static final Logger log = LoggerFactory.getLogger(RecuperacionService.class);

    static final int MAX_INTENTOS = 5;
    static final String CODIGO_INVALIDO = "El código no es válido o ya venció";

    private final UsuarioRepository usuarios;
    private final CodigoRecuperacionRepository codigos;
    private final PasswordEncoder encoder;
    private final EnviadorCorreo correo;
    private final int minutosVigencia;
    private final Clock reloj;
    private final SecureRandom aleatorio = new SecureRandom();

    @Autowired
    public RecuperacionService(UsuarioRepository usuarios, CodigoRecuperacionRepository codigos,
                               PasswordEncoder encoder, EnviadorCorreo correo,
                               @Value("${casajojo.recuperacion.minutos:15}") int minutosVigencia) {
        this(usuarios, codigos, encoder, correo, minutosVigencia, Clock.systemDefaultZone());
    }

    /** Constructor completo: permite fijar el reloj en las pruebas del vencimiento. */
    public RecuperacionService(UsuarioRepository usuarios, CodigoRecuperacionRepository codigos,
                               PasswordEncoder encoder, EnviadorCorreo correo, int minutosVigencia,
                               Clock reloj) {
        this.usuarios = usuarios;
        this.codigos = codigos;
        this.encoder = encoder;
        this.correo = correo;
        this.minutosVigencia = minutosVigencia;
        this.reloj = reloj;
    }

    @Transactional
    public void solicitar(String correoIngresado) {
        String correoNormal = normalizar(correoIngresado);
        Optional<Usuario> usuario = usuarios.findByUsername(correoNormal);
        if (usuario.isEmpty()) {
            log.info("Recuperación pedida para un correo sin cuenta (no se envía nada)");
            return;
        }
        // Un código nuevo invalida los anteriores: solo sirve el último enviado.
        codigos.findByCorreoAndUsadoFalse(correoNormal).forEach(c -> {
            c.setUsado(true);
            codigos.save(c);
        });

        String codigo = String.format("%06d", aleatorio.nextInt(1_000_000));
        CodigoRecuperacion registro = new CodigoRecuperacion();
        registro.setCorreo(correoNormal);
        registro.setCodigoHash(encoder.encode(codigo));
        registro.setExpira(LocalDateTime.now(reloj).plusMinutes(minutosVigencia));
        registro.setUsado(false);
        registro.setIntentos(0);
        codigos.save(registro);

        correo.enviar(correoNormal, "La Casa JoJo: código para restablecer tu contraseña",
                "Hola " + usuario.get().getNombre() + ",\n\n"
                        + "Tu código para restablecer la contraseña es: " + codigo + "\n"
                        + "Vence en " + minutosVigencia + " minutos y sirve una sola vez.\n\n"
                        + "Si no lo pediste, ignora este correo.\n\nLa Casa JoJo");
    }

    /** noRollbackFor: los intentos fallidos se guardan aunque se responda 400. */
    @Transactional(noRollbackFor = SolicitudInvalidaException.class)
    public void restablecer(String correoIngresado, String codigo, String nuevaPassword) {
        String correoNormal = normalizar(correoIngresado);
        CodigoRecuperacion registro = codigos.findFirstByCorreoAndUsadoFalseOrderByIdDesc(correoNormal)
                .orElseThrow(() -> new SolicitudInvalidaException(CODIGO_INVALIDO));

        if (registro.getExpira().isBefore(LocalDateTime.now(reloj))
                || registro.getIntentos() >= MAX_INTENTOS) {
            registro.setUsado(true);
            codigos.save(registro);
            throw new SolicitudInvalidaException(CODIGO_INVALIDO);
        }
        if (!encoder.matches(codigo == null ? "" : codigo.trim(), registro.getCodigoHash())) {
            registro.setIntentos(registro.getIntentos() + 1);
            if (registro.getIntentos() >= MAX_INTENTOS) {
                registro.setUsado(true);
            }
            codigos.save(registro);
            throw new SolicitudInvalidaException(CODIGO_INVALIDO);
        }

        Usuario usuario = usuarios.findByUsername(correoNormal)
                .orElseThrow(() -> new SolicitudInvalidaException(CODIGO_INVALIDO));
        usuario.setPassword(encoder.encode(nuevaPassword));
        usuarios.save(usuario);

        registro.setUsado(true); // un solo uso
        codigos.save(registro);
    }

    private String normalizar(String valor) {
        return valor == null ? "" : valor.trim().toLowerCase(Locale.ROOT);
    }
}
