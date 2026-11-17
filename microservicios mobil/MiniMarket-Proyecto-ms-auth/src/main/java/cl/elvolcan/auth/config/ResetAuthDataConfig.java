package cl.elvolcan.auth.config;

import cl.elvolcan.auth.repository.RolRepository;
import cl.elvolcan.auth.repository.UsuarioRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

/**
 * Utilidad TEMPORAL de mantenimiento (Fase B).
 * Borra los usuarios y roles del template MiniMarket heredados en la BD Oracle compartida,
 * para que DataInitializer siembre la semilla de El Volcan.
 *
 * Se activa con:  ./mvnw spring-boot:run -Dspring-boot.run.arguments=--elvolcan.reset-auth=true
 * NO se activa en condiciones normales (propiedad por defecto = false).
 */
@Profile("reset-auth")
@Configuration
public class ResetAuthDataConfig {

    @Bean
    CommandLineRunner resetAuthData(UsuarioRepository usuarioRepository, RolRepository rolRepository) {
        return args -> {
            System.out.println("=== [RESET-AUTH] Borrando datos del template MiniMarket ===");
            usuarioRepository.deleteAllInBatch(); // hijos (FK) primero
            rolRepository.deleteAllInBatch();      // luego los roles
            System.out.println("=== [RESET-AUTH] Datos borrados. Usuarios restantes: "
                    + usuarioRepository.count() + ", roles restantes: " + rolRepository.count() + " ===");
        };
    }
}
