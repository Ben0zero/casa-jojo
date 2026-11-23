package cl.casajojo.auth.config;

import cl.casajojo.auth.model.Rol;
import cl.casajojo.auth.model.Usuario;
import cl.casajojo.auth.repository.RolRepository;
import cl.casajojo.auth.repository.UsuarioRepository;
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
            Rol rolCocina = crearRolSiNoExiste(rolRepository, "COCINA");

            crearUsuario(usuarioRepository, passwordEncoder, "admin@casajojo.cl", "Admin123", rolAdmin,
                    "Administrador Casa JoJo", "CJ-ADMIN", "+56900000000");
            crearUsuario(usuarioRepository, passwordEncoder, "cliente@casajojo.cl", "Cliente123", rolCliente,
                    "Cliente de prueba", "CJ-CLIENTE", "+56911223344");
            // Fase 2: cuenta de la cocina (rol COCINA) para el listado de pedidos y
            // los cambios de estado. Contraseña de demostración: cámbiala en producción.
            crearUsuario(usuarioRepository, passwordEncoder, "cocina@casajojo.cl", "Cocina123", rolCocina,
                    "Cocina Casa JoJo", "CJ-COCINA", "+56900000001");
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
