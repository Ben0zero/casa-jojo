package cl.casajojo.catalogo.security.jwt;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.function.Function;

@Component
public class JwtUtil {
    
    // A7: el secreto se lee de la variable de entorno JWT_SECRET (propiedad jwt.secret).
    // Debe ser el MISMO en todos los microservicios y tener al menos 32 caracteres.
    private static final int LARGO_MINIMO_SECRETO = 32;
    private final SecretKey SECRET_KEY;

    public JwtUtil(@Value("${jwt.secret}") String secreto) {
        if (secreto == null || secreto.getBytes(StandardCharsets.UTF_8).length < LARGO_MINIMO_SECRETO) {
            throw new IllegalStateException(
                    "JWT_SECRET no está definido o tiene menos de " + LARGO_MINIMO_SECRETO + " caracteres");
        }
        this.SECRET_KEY = Keys.hmacShaKeyFor(secreto.getBytes(StandardCharsets.UTF_8));
    }

    public String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    // NUEVO: Necesitamos este método para saber si es ADMIN, CAJERO, etc.
    public String extractRole(String token) {
        return extractAllClaims(token).get("rol", String.class);
    }

    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    private Claims extractAllClaims(String token) {
        return Jwts.parserBuilder() 
                .setSigningKey(SECRET_KEY)
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    // NUEVO: Validación simplificada (solo revisa si es legítimo y no ha expirado)
    public boolean isTokenValid(String token) {
        try {
            return !isTokenExpired(token);
        } catch (Exception e) {
            return false; // Si el token está mal formado o fue alterado, lo rechaza
        }
    }

    private boolean isTokenExpired(String token) {
        return extractClaim(token, Claims::getExpiration).before(new Date());
    }
}
