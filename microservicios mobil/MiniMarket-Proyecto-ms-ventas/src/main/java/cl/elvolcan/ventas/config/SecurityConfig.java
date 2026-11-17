package cl.elvolcan.ventas.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
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

            // Configura las políticas de acceso a las URLs
            .authorizeHttpRequests(auth -> auth
                // Permitimos el acceso libre a la documentación de Swagger
                .requestMatchers(
                        "/v3/api-docs/**",
                        "/swagger-ui/**",
                        "/swagger-ui.html"
                ).permitAll()

                // API de pedidos de la app Casa JoJo (tablas y endpoints propios)
                .requestMatchers("/api/pedidos/**").permitAll()

                // Solo ADMIN y VENDEDOR pueden ver TODAS las ventas
                .requestMatchers(org.springframework.http.HttpMethod.GET, "/api/ventas").hasAnyRole("ADMIN", "VENDEDOR")
                // Cualquier usuario autenticado puede ver SUS propias compras
                .requestMatchers(org.springframework.http.HttpMethod.GET, "/api/ventas/mis-compras").hasAnyRole("ADMIN", "VENDEDOR", "CLIENTE")
                // Cualquier usuario autenticado puede crear una venta
                .requestMatchers(org.springframework.http.HttpMethod.POST, "/api/ventas").hasAnyRole("ADMIN", "VENDEDOR", "CLIENTE")
                // Cualquier usuario autenticado puede confirmar el pago de su venta
                .requestMatchers(org.springframework.http.HttpMethod.PUT, "/api/ventas/*/confirmar").hasAnyRole("ADMIN", "VENDEDOR", "CLIENTE")

                // Cualquier otra petición HTTP requerirá obligatoriamente que el usuario esté autenticado
                .anyRequest().authenticated()
            )

            // Define que el servicio no guardará estados de sesión en el servidor (Arquitectura Stateless de Microservicios)
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

            // Agrega nuestro filtro personalizado justo antes del filtro por defecto
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        // Retorna la cadena de seguridad armada
        return http.build();
    }
}