package cl.casajojo.auth.service;

import java.util.Locale;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.security.crypto.password.PasswordEncoder;

import cl.casajojo.auth.dto.LoginRequestDTO;
import cl.casajojo.auth.dto.RegistroRequestDTO;
import cl.casajojo.auth.dto.UsuarioResponseDTO;
import cl.casajojo.auth.exception.CorreoDuplicadoException;
import cl.casajojo.auth.exception.CredencialesInvalidasException;
import cl.casajojo.auth.exception.SolicitudInvalidaException;
import cl.casajojo.auth.model.Rol;
import cl.casajojo.auth.model.Usuario;
import cl.casajojo.auth.repository.RolRepository;
import cl.casajojo.auth.repository.UsuarioRepository;
import cl.casajojo.auth.security.jwt.JwtUtil;

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

    /** A2: único rol que puede obtener una cuenta creada desde el registro público. */
    static final String ROL_REGISTRO_PUBLICO = "CLIENTE";

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
            throw new CredencialesInvalidasException(); // M5: 401
        }

        Usuario usuario = usuarioOpt.get();
        if (!passwordEncoder.matches(request.getPassword(), usuario.getPassword())) {
            throw new CredencialesInvalidasException(); // M5: 401
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
        // La app envía "correo" (el DTO también acepta el alias "username").
        String correo = normalizar(request.getCorreo());
        if (correo == null || correo.isBlank()) {
            throw new SolicitudInvalidaException("Error: El correo es obligatorio"); // M5: 400
        }

        // El RUT es NOT NULL y único en la tabla heredada. Para Casa JoJo no se
        // pide al cliente: generamos un identificador interno determinístico.
        String rut = request.getRut() != null && !request.getRut().isBlank()
                ? request.getRut()
                : "CJ-" + correo;

        if (usuarioRepository.existsByRut(rut)) {
            throw new CorreoDuplicadoException("Error: Ya existe un usuario registrado con este RUT"); // M5: 409
        }
        if (usuarioRepository.existsByUsername(correo)) {
            throw new CorreoDuplicadoException("Error: El nombre de usuario ya está en uso"); // M5: 409
        }

        // A2: el registro público SIEMPRE crea cuentas CLIENTE. Antes se aceptaba
        // "nombreRol" desde el JSON y cualquiera podía registrarse como ADMIN.
        // Las cuentas ADMIN/COCINA se crean desde /api/usuarios (requiere token ADMIN).
        Rol rolAsignado = rolRepository.findByNombreRol(ROL_REGISTRO_PUBLICO)
                .orElseThrow(() -> new IllegalStateException("Falta el rol CLIENTE en la base de datos")); // 500

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
        // Fase 2: el registro ya deja la sesión iniciada; la app guarda este token
        // y lo envía en Authorization: Bearer a ms-ventas.
        String token = jwtUtil.generateToken(guardado.getUsername(), rolAsignado.getNombreRol());
        return aResponse(guardado, token);
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
