package cl.casajojo.auth.security.jwt;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
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

public String generateToken(String username, String rol){
        Map<String, Object> claims = new HashMap<>();
        // forzar a que el token guarde el rol con el formato que Spring espera, ya que arrojaba errores al comunicarse antes
        if (!rol.startsWith("ROLE_")) {
            rol = "ROLE_" + rol;
        }
        claims.put("rol", rol);

        return Jwts.builder()       
                .setClaims(claims)
                .setSubject(username)
                .setIssuedAt(new Date(System.currentTimeMillis()))
                .setExpiration(new Date(System.currentTimeMillis() + 1000 * 60 * 60 * 10))
                .signWith(SECRET_KEY, SignatureAlgorithm.HS256)
                .compact();
    }

    public String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    private Claims extractAllClaims(String token) {
        return Jwts.parserBuilder() // Cambiado aquí (parserBuilder)
                .setSigningKey(SECRET_KEY)
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    public boolean isTokenValid(String token, UserDetails userDetails) {
        final String username = extractUsername(token);
        return (username.equals(userDetails.getUsername()) && !isTokenExpired(token));
    }

    private boolean isTokenExpired(String token) {
        return extractClaim(token, Claims::getExpiration).before(new Date());
    }
}
