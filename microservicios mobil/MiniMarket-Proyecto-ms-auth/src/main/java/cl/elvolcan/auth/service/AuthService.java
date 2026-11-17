package cl.elvolcan.auth.service;

import java.util.Locale;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.security.crypto.password.PasswordEncoder;

import cl.elvolcan.auth.dto.LoginRequestDTO;
import cl.elvolcan.auth.dto.RegistroRequestDTO;
import cl.elvolcan.auth.dto.UsuarioResponseDTO;
import cl.elvolcan.auth.model.Rol;
import cl.elvolcan.auth.model.Usuario;
import cl.elvolcan.auth.repository.RolRepository;
import cl.elvolcan.auth.repository.UsuarioRepository;
import cl.elvolcan.auth.security.jwt.JwtUtil;

/**
 * Autenticación adaptada a La Casa JoJo.
 * El correo hace las veces de "username" (la tabla y el UserDetailsService se
 * mantienen iguales para no migrar la BD Oracle compartida).
 */
@Service
public class AuthService {

    private final UsuarioRepository usuarioRepository;
    private final RolRepository rolRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    public AuthService(UsuarioRepository usuarioRepository, RolRepository rolRepository,
                       PasswordEncoder passwordEncoder, JwtUtil jwtUtil) {
        this.usuarioRepository = usuarioRepository;
        this.rolRepository = rolRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil = jwtUtil;
    }

    public UsuarioResponseDTO login(LoginRequestDTO request) {
        String correo = normalizar(request.getCorreo());

        Optional<Usuario> usuarioOpt = usuarioRepository.findByUsername(correo);
        if (usuarioOpt.isEmpty()) {
            throw new RuntimeException("Error: Usuario o contraseña incorrectos");
        }

        Usuario usuario = usuarioOpt.get();
        if (!passwordEncoder.matches(request.getPassword(), usuario.getPassword())) {
            throw new RuntimeException("Error: Usuario o contraseña incorrectos");
        }

        String token = jwtUtil.generateToken(usuario.getUsername(), usuario.getRol().getNombreRol());

        UsuarioResponseDTO response = new UsuarioResponseDTO();
        response.setId(usuario.getId());
        response.setNombre(usuario.getNombre());
        response.setCorreo(usuario.getUsername());
        response.setRut(usuario.getRut());
        response.setUsername(usuario.getUsername());
        response.setNombreRol(usuario.getRol().getNombreRol());
        response.setDireccion(usuario.getDireccion());
        response.setRegion(usuario.getRegion());
        response.setComuna(usuario.getComuna());
        response.setTipoCliente(usuario.getTipoCliente());
        response.setTelefono(usuario.getTelefono());
        response.setToken(token);
        return response;
    }

    public UsuarioResponseDTO registrar(RegistroRequestDTO request) {
        // La app envía "correo". Si viene el username viejo, lo aprovechamos.
        String correo = normalizar(
                request.getCorreo() != null ? request.getCorreo() : request.getUsername());
        if (correo == null || correo.isBlank()) {
            throw new RuntimeException("Error: El correo es obligatorio");
        }

        // El RUT es NOT NULL y único en la tabla heredada. Para Casa JoJo no se
        // pide al cliente: generamos un identificador interno determinístico.
        String rut = request.getRut() != null && !request.getRut().isBlank()
                ? request.getRut()
                : "CJ-" + correo;

        if (usuarioRepository.existsByRut(rut)) {
            throw new RuntimeException("Error: Ya existe un usuario registrado con este RUT");
        }
        if (usuarioRepository.existsByUsername(correo)) {
            throw new RuntimeException("Error: El nombre de usuario ya está en uso");
        }

        String rolNombre = request.getNombreRol() != null && !request.getNombreRol().isBlank()
                ? request.getNombreRol().toUpperCase(Locale.ROOT)
                : "CLIENTE";
        Rol rolAsignado = rolRepository.findByNombreRol(rolNombre)
                .orElseThrow(() -> new RuntimeException("Error: El rol especificado no existe"));

        Usuario nuevo = new Usuario();
        nuevo.setNombre(request.getNombre());
        nuevo.setRut(rut);
        nuevo.setUsername(correo);
        nuevo.setPassword(passwordEncoder.encode(request.getPassword()));
        nuevo.setRol(rolAsignado);
        nuevo.setDireccion(request.getDireccion());
        nuevo.setRegion(request.getRegion());
        nuevo.setComuna(request.getComuna());
        nuevo.setTipoCliente(request.getTipoCliente() != null ? request.getTipoCliente() : "CLIENTE");
        nuevo.setTelefono(request.getTelefono());

        Usuario guardado = usuarioRepository.save(nuevo);
        return aResponse(guardado, null);
    }

    private UsuarioResponseDTO aResponse(Usuario u, String token) {
        UsuarioResponseDTO response = new UsuarioResponseDTO();
        response.setId(u.getId());
        response.setNombre(u.getNombre());
        response.setCorreo(u.getUsername());
        response.setRut(u.getRut());
        response.setUsername(u.getUsername());
        response.setNombreRol(u.getRol() != null ? u.getRol().getNombreRol() : null);
        response.setDireccion(u.getDireccion());
        response.setRegion(u.getRegion());
        response.setComuna(u.getComuna());
        response.setTipoCliente(u.getTipoCliente());
        response.setTelefono(u.getTelefono());
        response.setToken(token);
        return response;
    }

    private String normalizar(String valor) {
        return valor == null ? null : valor.trim().toLowerCase(Locale.ROOT);
    }
}
