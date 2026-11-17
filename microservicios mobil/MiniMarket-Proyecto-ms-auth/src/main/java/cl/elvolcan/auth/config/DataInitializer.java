package cl.elvolcan.auth.config;

import cl.elvolcan.auth.model.Rol;
import cl.elvolcan.auth.model.Usuario;
import cl.elvolcan.auth.repository.RolRepository;
import cl.elvolcan.auth.repository.UsuarioRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

/** Semilla de cuentas de La Casa JoJo (el correo es el usuario de login). */
@Configuration
public class DataInitializer {

    @Bean
    CommandLineRunner initDatabase(UsuarioRepository usuarioRepository, RolRepository rolRepository,
                                   PasswordEncoder passwordEncoder) {
        return args -> {
            Rol rolAdmin = crearRolSiNoExiste(rolRepository, "ADMIN");
            Rol rolCliente = crearRolSiNoExiste(rolRepository, "CLIENTE");
            crearRolSiNoExiste(rolRepository, "COCINA");
            crearRolSiNoExiste(rolRepository, "VENDEDOR"); // compatibilidad con servicios heredados

            crearUsuario(usuarioRepository, passwordEncoder, "admin@casajojo.cl", "Admin123", rolAdmin,
                    "Administrador Casa JoJo", "CJ-ADMIN", "+56900000000");
            crearUsuario(usuarioRepository, passwordEncoder, "cliente@casajojo.cl", "Cliente123", rolCliente,
                    "Cliente de prueba", "CJ-CLIENTE", "+56911223344");
        };
    }

    private Rol crearRolSiNoExiste(RolRepository repository, String nombreRol) {
        return repository.findByNombreRol(nombreRol).orElseGet(() -> {
            Rol nuevoRol = new Rol();
            nuevoRol.setNombreRol(nombreRol);
            return repository.save(nuevoRol);
        });
    }

    private void crearUsuario(UsuarioRepository repo, PasswordEncoder encoder, String correo, String password,
                              Rol rol, String nombre, String rut, String telefono) {
        if (repo.existsByUsername(correo)) {
            return;
        }
        Usuario usuario = new Usuario();
        usuario.setUsername(correo);
        usuario.setPassword(encoder.encode(password));
        usuario.setRol(rol);
        usuario.setNombre(nombre);
        usuario.setRut(rut);
        usuario.setTipoCliente("CLIENTE");
        usuario.setTelefono(telefono);
        repo.save(usuario);
    }
}
