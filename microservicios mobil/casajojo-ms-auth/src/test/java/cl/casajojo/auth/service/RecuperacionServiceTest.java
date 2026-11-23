package cl.casajojo.auth.service;

import cl.casajojo.auth.exception.SolicitudInvalidaException;
import cl.casajojo.auth.model.CodigoRecuperacion;
import cl.casajojo.auth.model.Usuario;
import cl.casajojo.auth.repository.CodigoRecuperacionRepository;
import cl.casajojo.auth.repository.UsuarioRepository;
import cl.casajojo.auth.service.correo.EnviadorCorreo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/** EFT: recuperación de contraseña con código de un solo uso (15 min). */
class RecuperacionServiceTest {

    private static final ZoneId ZONA = ZoneId.of("America/Santiago");
    private static final Instant AHORA = Instant.parse("2026-11-23T15:00:00Z");

    private UsuarioRepository usuarios;
    private CodigoRecuperacionRepository codigos;
    private EnviadorCorreo correo;
    private final PasswordEncoder encoder = new BCryptPasswordEncoder(4);
    private RecuperacionService service;
    private Usuario usuario;

    @BeforeEach
    void setUp() {
        usuarios = mock(UsuarioRepository.class);
        codigos = mock(CodigoRecuperacionRepository.class);
        correo = mock(EnviadorCorreo.class);
        service = new RecuperacionService(usuarios, codigos, encoder, correo, 15, Clock.fixed(AHORA, ZONA));
        usuario = new Usuario();
        usuario.setNombre("Camila");
        usuario.setUsername("camila@casajojo.cl");
        usuario.setPassword(encoder.encode("Vieja123"));
    }

    private CodigoRecuperacion codigoVigente(String codigo, int minutosRestantes) {
        CodigoRecuperacion c = new CodigoRecuperacion();
        c.setCorreo("camila@casajojo.cl");
        c.setCodigoHash(encoder.encode(codigo));
        c.setExpira(LocalDateTime.now(Clock.fixed(AHORA, ZONA)).plusMinutes(minutosRestantes));
        return c;
    }

    @Test
    @DisplayName("solicitar guarda el hash del código, vence en 15 min y lo envía por correo")
    void solicitarEnviaCodigo() {
        when(usuarios.findByUsername("camila@casajojo.cl")).thenReturn(Optional.of(usuario));
        CodigoRecuperacion anterior = codigoVigente("111111", 5);
        when(codigos.findByCorreoAndUsadoFalse("camila@casajojo.cl")).thenReturn(List.of(anterior));

        service.solicitar(" Camila@CasaJojo.cl ");

        assertTrue(anterior.isUsado(), "el código anterior queda invalidado");
        ArgumentCaptor<String> cuerpo = ArgumentCaptor.forClass(String.class);
        verify(correo).enviar(eq("camila@casajojo.cl"), anyString(), cuerpo.capture());
        Matcher m = Pattern.compile("(\\d{6})").matcher(cuerpo.getValue());
        assertTrue(m.find());

        ArgumentCaptor<CodigoRecuperacion> guardado = ArgumentCaptor.forClass(CodigoRecuperacion.class);
        verify(codigos, atLeastOnce()).save(guardado.capture());
        CodigoRecuperacion nuevo = guardado.getAllValues().get(guardado.getAllValues().size() - 1);
        assertNotEquals(m.group(1), nuevo.getCodigoHash(), "no se guarda en texto plano");
        assertTrue(encoder.matches(m.group(1), nuevo.getCodigoHash()));
        assertEquals(LocalDateTime.now(Clock.fixed(AHORA, ZONA)).plusMinutes(15), nuevo.getExpira());
    }

    @Test
    @DisplayName("solicitar con un correo sin cuenta no envía nada (no revela si existe)")
    void solicitarCorreoInexistente() {
        when(usuarios.findByUsername(anyString())).thenReturn(Optional.empty());
        service.solicitar("nadie@casajojo.cl");
        verifyNoInteractions(correo);
        verify(codigos, never()).save(any());
    }

    @Test
    @DisplayName("restablecer con el código correcto cambia la contraseña y lo marca usado")
    void restablecerOk() {
        CodigoRecuperacion c = codigoVigente("482913", 10);
        when(codigos.findFirstByCorreoAndUsadoFalseOrderByIdDesc("camila@casajojo.cl")).thenReturn(Optional.of(c));
        when(usuarios.findByUsername("camila@casajojo.cl")).thenReturn(Optional.of(usuario));

        service.restablecer("camila@casajojo.cl", "482913", "Nueva123");

        assertTrue(encoder.matches("Nueva123", usuario.getPassword()));
        assertTrue(c.isUsado());
    }

    @Test
    @DisplayName("restablecer con un código vencido responde 400")
    void restablecerVencido() {
        CodigoRecuperacion c = codigoVigente("482913", -1);
        when(codigos.findFirstByCorreoAndUsadoFalseOrderByIdDesc(anyString())).thenReturn(Optional.of(c));

        var ex = assertThrows(SolicitudInvalidaException.class,
                () -> service.restablecer("camila@casajojo.cl", "482913", "Nueva123"));
        assertEquals(RecuperacionService.CODIGO_INVALIDO, ex.getMessage());
        assertTrue(c.isUsado());
        verify(usuarios, never()).save(any());
    }

    @Test
    @DisplayName("restablecer con un código incorrecto suma un intento; al quinto se invalida")
    void restablecerIncorrecto() {
        CodigoRecuperacion c = codigoVigente("482913", 10);
        c.setIntentos(RecuperacionService.MAX_INTENTOS - 1);
        when(codigos.findFirstByCorreoAndUsadoFalseOrderByIdDesc(anyString())).thenReturn(Optional.of(c));

        assertThrows(SolicitudInvalidaException.class,
                () -> service.restablecer("camila@casajojo.cl", "000000", "Nueva123"));
        assertEquals(RecuperacionService.MAX_INTENTOS, c.getIntentos());
        assertTrue(c.isUsado());
    }

    @Test
    @DisplayName("restablecer sin código pendiente responde 400")
    void restablecerSinCodigo() {
        when(codigos.findFirstByCorreoAndUsadoFalseOrderByIdDesc(anyString())).thenReturn(Optional.empty());
        assertThrows(SolicitudInvalidaException.class,
                () -> service.restablecer("camila@casajojo.cl", "482913", "Nueva123"));
    }

    @Test
    @DisplayName("restablecer con el máximo de intentos ya alcanzado responde 400 aunque el código sea correcto")
    void restablecerBloqueado() {
        CodigoRecuperacion c = codigoVigente("482913", 10);
        c.setIntentos(RecuperacionService.MAX_INTENTOS);
        when(codigos.findFirstByCorreoAndUsadoFalseOrderByIdDesc(anyString())).thenReturn(Optional.of(c));
        assertThrows(SolicitudInvalidaException.class,
                () -> service.restablecer("camila@casajojo.cl", "482913", "Nueva123"));
    }
}
