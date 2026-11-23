package cl.casajojo.auth.service;

import cl.casajojo.auth.dto.LoginRequestDTO;
import cl.casajojo.auth.dto.RegistroRequestDTO;
import cl.casajojo.auth.dto.UsuarioResponseDTO;
import cl.casajojo.auth.exception.CorreoDuplicadoException;
import cl.casajojo.auth.exception.CredencialesInvalidasException;
import cl.casajojo.auth.model.Rol;
import cl.casajojo.auth.model.Usuario;
import cl.casajojo.auth.repository.RolRepository;
import cl.casajojo.auth.repository.UsuarioRepository;
import cl.casajojo.auth.security.jwt.JwtUtil;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private RolRepository rolRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtUtil jwtUtil;

    @InjectMocks
    private AuthService authService;

    @Test
    @DisplayName("login debe retornar UsuarioResponseDTO con token cuando credenciales son correctas")
    void loginDebeRetornarDtoConTokenCuandoCredencialesCorrectas() {
        // Given
        LoginRequestDTO request = new LoginRequestDTO();
        request.setUsername("admin");
        request.setPassword("admin123");

        Rol rol = new Rol();
        rol.setNombreRol("ADMIN");

        Usuario usuario = new Usuario();
        usuario.setId(1L);
        usuario.setUsername("admin");
        usuario.setPassword("hash_de_contraseña");
        usuario.setRol(rol);

        when(usuarioRepository.findByUsername("admin")).thenReturn(Optional.of(usuario));
        when(passwordEncoder.matches("admin123", "hash_de_contraseña")).thenReturn(true);
        when(jwtUtil.generateToken("admin", "ADMIN")).thenReturn("token_falso_123");

        // When
        UsuarioResponseDTO resultado = authService.login(request);

        // Then
        assertNotNull(resultado);
        assertEquals("admin", resultado.getUsername());
        assertEquals("token_falso_123", resultado.getToken());
        
        verify(usuarioRepository).findByUsername("admin");
        verify(passwordEncoder).matches("admin123", "hash_de_contraseña");
        verify(jwtUtil).generateToken("admin", "ADMIN");
    }

    @Test
    @DisplayName("login debe lanzar excepción cuando la contraseña es incorrecta")
    void loginDebeLanzarExcepcionCuandoContrasenaIncorrecta() {
        // Given
        LoginRequestDTO request = new LoginRequestDTO();
        request.setUsername("admin");
        request.setPassword("clave_mala");

        Usuario usuario = new Usuario();
        usuario.setUsername("admin");
        usuario.setPassword("hash_de_contraseña");

        when(usuarioRepository.findByUsername("admin")).thenReturn(Optional.of(usuario));
        when(passwordEncoder.matches("clave_mala", "hash_de_contraseña")).thenReturn(false);

        // When + Then
        CredencialesInvalidasException exception = assertThrows(
                CredencialesInvalidasException.class,
                () -> authService.login(request)
        );

        assertEquals("Error: Usuario o contraseña incorrectos", exception.getMessage());
        verify(usuarioRepository).findByUsername("admin");
        verify(passwordEncoder).matches("clave_mala", "hash_de_contraseña");
        verify(jwtUtil, never()).generateToken(anyString(), anyString()); // Verifica que NO se genere token
    }

    @Test
    @DisplayName("registrar debe guardar usuario exitosamente cuando los datos son válidos")
    void registrarDebeGuardarUsuarioCuandoDatosValidos() {
        // Given
        RegistroRequestDTO request = new RegistroRequestDTO();
        request.setNombre("Nuevo Usuario");
        request.setRut("11222333-4");
        request.setCorreo("nuevo_user");
        request.setPassword("123456");

        Rol rolCliente = new Rol();
        rolCliente.setNombreRol("CLIENTE");

        Usuario usuarioGuardado = new Usuario();
        usuarioGuardado.setId(2L);
        usuarioGuardado.setNombre("Nuevo Usuario");
        usuarioGuardado.setRut("11222333-4");
        usuarioGuardado.setUsername("nuevo_user");
        usuarioGuardado.setRol(rolCliente);

        when(usuarioRepository.existsByRut("11222333-4")).thenReturn(false);
        when(usuarioRepository.existsByUsername("nuevo_user")).thenReturn(false);
        when(rolRepository.findByNombreRol("CLIENTE")).thenReturn(Optional.of(rolCliente));
        when(passwordEncoder.encode("123456")).thenReturn("hash_nuevo");
        when(usuarioRepository.save(any(Usuario.class))).thenReturn(usuarioGuardado);

        // When
        UsuarioResponseDTO resultado = authService.registrar(request);

        // Then
        assertNotNull(resultado);
        assertEquals(2L, resultado.getId());
        assertEquals("nuevo_user", resultado.getUsername());
        assertEquals("CLIENTE", resultado.getNombreRol());

        verify(usuarioRepository).existsByRut("11222333-4");
        verify(usuarioRepository).existsByUsername("nuevo_user");
        verify(rolRepository).findByNombreRol("CLIENTE");
        verify(passwordEncoder).encode("123456");
        verify(usuarioRepository).save(any(Usuario.class));
    }

    @Test
    @DisplayName("registrar debe lanzar excepción cuando el username ya existe")
    void registrarDebeLanzarExcepcionCuandoUsernameExiste() {
        // Given
        RegistroRequestDTO request = new RegistroRequestDTO();
        request.setRut("11222333-4");
        request.setCorreo("user_duplicado");

        when(usuarioRepository.existsByRut(anyString())).thenReturn(false);
        when(usuarioRepository.existsByUsername("user_duplicado")).thenReturn(true);

        // When + Then (M5: excepción propia que el handler traduce a 409)
        CorreoDuplicadoException exception = assertThrows(
                CorreoDuplicadoException.class,
                () -> authService.registrar(request)
        );

        assertEquals("Error: El nombre de usuario ya está en uso", exception.getMessage());
        verify(usuarioRepository, never()).save(any(Usuario.class)); // Asegura que nunca llega a guardar
    }

    @Test
    @DisplayName("A2: registrar siempre asigna CLIENTE y nunca consulta otro rol")
    void registrarSiempreAsignaRolCliente() {
        // Given: aunque el JSON original trajera "nombreRol": "ADMIN", el DTO ya no
        // tiene ese campo, así que el servicio solo puede pedir el rol CLIENTE.
        RegistroRequestDTO request = new RegistroRequestDTO();
        request.setNombre("Intruso");
        request.setCorreo("Intruso@CasaJojo.cl ");
        request.setPassword("Clave123");

        Rol rolCliente = new Rol();
        rolCliente.setNombreRol("CLIENTE");

        when(usuarioRepository.existsByRut("CJ-intruso@casajojo.cl")).thenReturn(false);
        when(usuarioRepository.existsByUsername("intruso@casajojo.cl")).thenReturn(false);
        when(rolRepository.findByNombreRol("CLIENTE")).thenReturn(Optional.of(rolCliente));
        when(passwordEncoder.encode("Clave123")).thenReturn("hash");
        when(usuarioRepository.save(any(Usuario.class))).thenAnswer(inv -> inv.getArgument(0));
        when(jwtUtil.generateToken("intruso@casajojo.cl", "CLIENTE")).thenReturn("token-cliente");

        // When
        UsuarioResponseDTO resultado = authService.registrar(request);

        // Then
        assertEquals("CLIENTE", resultado.getNombreRol());
        assertEquals("intruso@casajojo.cl", resultado.getCorreo());
        verify(rolRepository).findByNombreRol("CLIENTE");
        verify(rolRepository, never()).findByNombreRol("ADMIN");
        // Fase 2: el registro devuelve un token firmado con el rol CLIENTE.
        assertEquals("token-cliente", resultado.getToken());
    }

    @Test
    @DisplayName("M5: registrar con RUT duplicado lanza CorreoDuplicadoException (409)")
    void registrarConRutDuplicadoLanza409() {
        RegistroRequestDTO request = new RegistroRequestDTO();
        request.setCorreo("otro@casajojo.cl");
        request.setRut("11222333-4");

        when(usuarioRepository.existsByRut("11222333-4")).thenReturn(true);

        assertThrows(CorreoDuplicadoException.class, () -> authService.registrar(request));
        verify(usuarioRepository, never()).save(any(Usuario.class));
    }

    @Test
    @DisplayName("M5: login con correo inexistente lanza CredencialesInvalidasException (401)")
    void loginConCorreoInexistenteLanza401() {
        LoginRequestDTO request = new LoginRequestDTO();
        request.setCorreo("nadie@casajojo.cl");
        request.setPassword("x");

        when(usuarioRepository.findByUsername("nadie@casajojo.cl")).thenReturn(Optional.empty());

        assertThrows(CredencialesInvalidasException.class, () -> authService.login(request));
    }
}
