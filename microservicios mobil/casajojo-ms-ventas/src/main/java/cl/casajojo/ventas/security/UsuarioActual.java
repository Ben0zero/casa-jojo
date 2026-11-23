package cl.casajojo.ventas.security;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;

import java.util.Locale;
import java.util.Set;

/**
 * A1: quién hace la petición, tomado del JWT validado por JwtAuthenticationFilter.
 *
 * @param correo  "sub" del token (el correo del cliente), en minúsculas.
 * @param personal true si tiene rol COCINA o ADMIN (personal de la tienda).
 */
public record UsuarioActual(String correo, boolean personal) {

    private static final Set<String> ROLES_PERSONAL = Set.of("ROLE_COCINA", "ROLE_ADMIN");

    public static UsuarioActual desde(Authentication auth) {
        if (auth == null || auth.getName() == null || !auth.isAuthenticated()) {
            throw new AccessDeniedException("Se requiere iniciar sesión");
        }
        boolean personal = auth.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(ROLES_PERSONAL::contains);
        return new UsuarioActual(auth.getName().trim().toLowerCase(Locale.ROOT), personal);
    }

    /** El personal puede ver cualquier pedido; un cliente solo los de su correo. */
    public boolean puedeVer(String correoPedido) {
        return personal || correo.equalsIgnoreCase(correoPedido == null ? "" : correoPedido.trim());
    }

    /** Lanza 403 si el usuario no puede operar sobre pedidos de ese correo. */
    public void exigirAcceso(String correoPedido) {
        if (!puedeVer(correoPedido)) {
            throw new AccessDeniedException("No puedes acceder a pedidos de otro cliente");
        }
    }
}
