package cl.casajojo.auth.service;

import cl.casajojo.auth.dto.ActualizarPerfilDTO;
import cl.casajojo.auth.dto.UsuarioResponseDTO;
import cl.casajojo.auth.exception.SolicitudInvalidaException;
import cl.casajojo.auth.exception.UsuarioNoEncontradoException;
import cl.casajojo.auth.model.Usuario;
import cl.casajojo.auth.repository.CodigoRecuperacionRepository;
import cl.casajojo.auth.repository.UsuarioRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * EFT: perfil del propio cliente (/api/usuarios/me). El correo sale SIEMPRE del JWT,
 * nunca del cuerpo de la petición: nadie puede leer ni modificar la cuenta de otro.
 */
@Service
public class PerfilService {

    static final String PASSWORD_ACTUAL_INCORRECTA = "La contraseña actual no es correcta";
    static final String PASSWORD_REPETIDA = "La nueva contraseña debe ser distinta de la actual";

    private final UsuarioRepository usuarios;
    private final CodigoRecuperacionRepository codigos;
    private final PasswordEncoder encoder;

    public PerfilService(UsuarioRepository usuarios, CodigoRecuperacionRepository codigos,
                         PasswordEncoder encoder) {
        this.usuarios = usuarios;
        this.codigos = codigos;
        this.encoder = encoder;
    }

    public UsuarioResponseDTO obtener(String correo) {
        return aResponse(buscar(correo));
    }

    @Transactional
    public UsuarioResponseDTO actualizar(String correo, ActualizarPerfilDTO datos) {
        Usuario usuario = buscar(correo);
        usuario.setNombre(datos.getNombre().trim());
        usuario.setTelefono(datos.getTelefono().trim());
        return aResponse(usuarios.save(usuario));
    }

    @Transactional
    public void cambiarPassword(String correo, String actual, String nueva) {
        Usuario usuario = buscar(correo);
        if (!encoder.matches(actual, usuario.getPassword())) {
            throw new SolicitudInvalidaException(PASSWORD_ACTUAL_INCORRECTA);
        }
        if (encoder.matches(nueva, usuario.getPassword())) {
            throw new SolicitudInvalidaException(PASSWORD_REPETIDA);
        }
        usuario.setPassword(encoder.encode(nueva));
        usuarios.save(usuario);
    }

    @Transactional
    public void eliminar(String correo) {
        Usuario usuario = buscar(correo);
        codigos.deleteByCorreo(usuario.getUsername());
        usuarios.delete(usuario);
    }

    private Usuario buscar(String correo) {
        return usuarios.findByUsername(correo).orElseThrow(UsuarioNoEncontradoException::new);
    }

    private UsuarioResponseDTO aResponse(Usuario u) {
        UsuarioResponseDTO r = new UsuarioResponseDTO();
        r.setId(u.getId());
        r.setNombre(u.getNombre());
        r.setCorreo(u.getUsername());
        r.setUsername(u.getUsername());
        r.setNombreRol(u.getRol() != null ? u.getRol().getNombreRol() : null);
        r.setTelefono(u.getTelefono());
        return r;
    }
}
