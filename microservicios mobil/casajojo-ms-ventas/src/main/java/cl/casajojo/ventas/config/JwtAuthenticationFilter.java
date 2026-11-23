package cl.casajojo.ventas.config;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.crypto.SecretKey;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    // A7: el secreto se lee de la variable de entorno JWT_SECRET (propiedad jwt.secret).
    // Debe ser el MISMO en todos los microservicios y tener al menos 32 caracteres.
    private static final int LARGO_MINIMO_SECRETO = 32;
    private final SecretKey SECRET_KEY;

    public JwtAuthenticationFilter(@Value("${jwt.secret}") String secreto) {
        if (secreto == null || secreto.getBytes(StandardCharsets.UTF_8).length < LARGO_MINIMO_SECRETO) {
            throw new IllegalStateException(
                    "JWT_SECRET no está definido o tiene menos de " + LARGO_MINIMO_SECRETO + " caracteres");
        }
        this.SECRET_KEY = Keys.hmacShaKeyFor(secreto.getBytes(StandardCharsets.UTF_8));
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        String authHeader = request.getHeader("Authorization");

        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);
            if (!token.isBlank()) {
                try {
                    Claims claims = Jwts.parserBuilder()
                            .setSigningKey(SECRET_KEY)
                            .build()
                            .parseClaimsJws(token)
                            .getBody();
                    String username = claims.getSubject();
                    if (username != null) {
                        List<SimpleGrantedAuthority> authorities = new ArrayList<>();
                        Object rol = claims.get("rol");
                        if (rol != null) {
                            String rolStr = rol.toString();
                            if (!rolStr.startsWith("ROLE_")) {
                                rolStr = "ROLE_" + rolStr;
                            }
                            authorities.add(new SimpleGrantedAuthority(rolStr));
                        }
                        UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                                username, null, authorities);
                        SecurityContextHolder.getContext().setAuthentication(authentication);
                    }
                } catch (Exception e) {
                    // Si falla la validación, seguimos sin autenticar (Spring lo bloqueará si es necesario)
                }
            }
        }

        filterChain.doFilter(request, response);
    }
}