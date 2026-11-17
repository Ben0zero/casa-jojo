package cl.elvolcan.catalogo.security.jwt;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;

@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        // extraer el encabezado authorization
        String authHeader = request.getHeader(HttpHeaders.AUTHORIZATION);

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        String token = authHeader.substring(7);

        // si isTokenValid ocurre la magia
        if (jwtUtil.isTokenValid(token)) {
            String username = jwtUtil.extractUsername(token);
            
            // aca se extra  el rol del token (ej. "ADMIN")
            String rol = jwtUtil.extractRole(token); 

            // 1. Prevención de nulos por si el token viene alterado
            if (rol == null) {
                rol = "SIN_ROL";
            }

            // validacion: solo agrega "ROLE_" si NO lo tiene ya para evitar ROLE_ROLE_ADMIN y cause error
            SimpleGrantedAuthority authority = new SimpleGrantedAuthority(rol.startsWith("ROLE_") ? rol : "ROLE_" + rol);

            // se crea el pase VIP oficial incluyendo la autoridad (rol)
            UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                    username, null, Collections.singletonList(authority)
            );

            SecurityContextHolder.getContext().setAuthentication(authToken);
        }

        filterChain.doFilter(request, response);
    }
}
