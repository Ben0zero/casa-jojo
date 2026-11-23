package cl.casajojo.auth.service;

import cl.casajojo.auth.dto.ActualizarPerfilDTO;
import cl.casajojo.auth.dto.UsuarioResponseDTO;
import cl.casajojo.auth.exception.SolicitudInvalidaException;
import cl.casajojo.auth.exception.UsuarioNoEncontradoException;
import cl.casajojo.auth.model.Rol;
import cl.casajojo.auth.model.Usuario;
import cl.casajojo.auth.repository.CodigoRecuperacionRepository;
import cl.casajojo.auth.repository.UsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/** EFT: CRUD del perfil propio (/api/usuarios/me). */
class PerfilServiceTest {

    private UsuarioRepository usuarios;
    private CodigoRecuperacionRepository codigos;
    private final PasswordEncoder encoder = new BCryptPasswordEncoder(4);
    private PerfilService service;
    private Usuario usuario;

    @BeforeEach
    void setUp() {
        usuarios = mock(UsuarioRepository.class);
        codigos = mock(CodigoRecuperacionRepository.class);
        service = new PerfilService(usuarios, codigos, encoder);
        Rol rol = new Rol();
        rol.setNombreRol("CLIENTE");
        usuario = new Usuario();
        usuario.setId(7L);
        usuario.setNombre("Camila");
        usuario.setUsername("camila@casajojo.cl");
        usuario.setTelefono("912345678");
        usuario.setPassword(encoder.encode("Clave123"));
        usuario.setRol(rol);
        when(usuarios.findByUsername("camila@casajojo.cl")).thenReturn(Optional.of(usuario));
        when(usuarios.save(any())).thenAnswer(i -> i.getArgument(0));
    }

    @Test
    @DisplayName("obtener devuelve los datos sin token ni contraseña")
    void obtener() {
        UsuarioResponseDTO r = service.obtener("camila@casajojo.cl");
        assertEquals("Camila", r.getNombre());
        assertEquals("camila@casajojo.cl", r.getCorreo());
        assertEquals("912345678", r.getTelefono());
        assertEquals("CLIENTE", r.getNombreRol());
        assertNull(r.getToken());
    }

    @Test
    @DisplayName("obtener una cuenta que no existe lanza 404")
    void obtenerInexistente() {
        when(usuarios.findByUsername("otro@casajojo.cl")).thenReturn(Optional.empty());
        assertThrows(UsuarioNoEncontradoException.class, () -> service.obtener("otro@casajojo.cl"));
    }

    @Test
    @DisplayName("actualizar cambia nombre y teléfono")
    void actualizar() {
        ActualizarPerfilDTO datos = new ActualizarPerfilDTO();
        datos.setNombre("  Camila Rojas ");
        datos.setTelefono("987654321");
        UsuarioResponseDTO r = service.actualizar("camila@casajojo.cl", datos);
        assertEquals("Camila Rojas", r.getNombre());
        assertEquals("987654321", usuario.getTelefono());
    }

    @Test
    @DisplayName("cambiarPassword con la actual correcta guarda la nueva cifrada")
    void cambiarPasswordOk() {
        service.cambiarPassword("camila@casajojo.cl", "Clave123", "Nueva456");
        assertTrue(encoder.matches("Nueva456", usuario.getPassword()));
    }

    @Test
    @DisplayName("cambiarPassword con la actual incorrecta responde 400")
    void cambiarPasswordActualIncorrecta() {
        var ex = assertThrows(SolicitudInvalidaException.class,
                () -> service.cambiarPassword("camila@casajojo.cl", "Mala999", "Nueva456"));
        assertEquals(PerfilService.PASSWORD_ACTUAL_INCORRECTA, ex.getMessage());
        verify(usuarios, never()).save(any());
    }

    @Test
    @DisplayName("cambiarPassword rechaza repetir la misma contraseña")
    void cambiarPasswordRepetida() {
        var ex = assertThrows(SolicitudInvalidaException.class,
                () -> service.cambiarPassword("camila@casajojo.cl", "Clave123", "Clave123"));
        assertEquals(PerfilService.PASSWORD_REPETIDA, ex.getMessage());
    }

    @Test
    @DisplayName("eliminar borra la cuenta y sus códigos de recuperación")
    void eliminar() {
        service.eliminar("camila@casajojo.cl");
        verify(codigos).deleteByCorreo("camila@casajojo.cl");
        verify(usuarios).delete(usuario);
    }

    @Test
    @DisplayName("eliminar una cuenta inexistente lanza 404 y no borra nada")
    void eliminarInexistente() {
        when(usuarios.findByUsername(anyString())).thenReturn(Optional.empty());
        assertThrows(UsuarioNoEncontradoException.class, () -> service.eliminar("x@casajojo.cl"));
        verify(usuarios, never()).delete(any());
    }
}
