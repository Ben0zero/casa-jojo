package cl.casajojo.ventas.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

// @Configuration le avisa a Spring que este archivo contiene configuraciones del sistema
@Configuration
// @EnableWebSecurity activa el soporte de seguridad web en este microservicio
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    // El constructor inyecta automáticamente el filtro que creamos en el Paso 1
    public SecurityConfig(JwtAuthenticationFilter jwtAuthenticationFilter) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
    }

    // @Bean expone esta configuración para que Spring la aplique globalmente
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            // Desactiva CSRF (Protección contra falsificación) ya que trabajamos con APIs REST y Tokens
            .csrf(AbstractHttpConfigurer::disable)

            // M1: CORS para la web (ver CorsConfig). El preflight OPTIONS pasa sin token.
            .cors(Customizer.withDefaults())

            // Configura las políticas de acceso a las URLs
            .authorizeHttpRequests(auth -> auth
                // Permitimos el acceso libre a la documentación de Swagger
                .requestMatchers(
                        "/v3/api-docs/**",
                        "/swagger-ui/**",
                        "/swagger-ui.html"
                ).permitAll()
                .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                // Las respuestas 403/404 pasan por /error: sin esto el usuario (ya sin
                // contexto en ese segundo despacho) recibía 401 en vez de 403.
                .requestMatchers("/error").permitAll()

                // A1: la API de pedidos de Casa JoJo ya NO es pública.
                // Cocina: listado general de pedidos y marcar pagado (caja), solo COCINA o ADMIN.
                .requestMatchers(HttpMethod.GET, "/api/pedidos").hasAnyRole("COCINA", "ADMIN")
                .requestMatchers(HttpMethod.PUT, "/api/pedidos/*/pagar").hasAnyRole("COCINA", "ADMIN")
                // El resto (crear, mis-pedidos, ultimo, estado, retirar) exige token; que el
                // pedido sea del correo del token lo revisa PedidoCasaJojoService (403 si no).
                .requestMatchers("/api/pedidos/**").authenticated()

                // Cualquier otra petición HTTP requerirá obligatoriamente que el usuario esté autenticado
                .anyRequest().authenticated()
            )

            // A1: sin token (o con token inválido) se responde 401, no 403.
            .exceptionHandling(e -> e.authenticationEntryPoint(new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED)))

            // Define que el servicio no guardará estados de sesión en el servidor (Arquitectura Stateless de Microservicios)
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

            // Agrega nuestro filtro personalizado justo antes del filtro por defecto
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        // Retorna la cadena de seguridad armada
        return http.build();
    }
}
