package cl.elvolcan.ventas.security;

import feign.RequestInterceptor;
import feign.RequestTemplate;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Configuration
public class FeignInterceptor implements RequestInterceptor {

    @Override
    public void apply(RequestTemplate template) {
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        
        if (attributes != null) {
            String authHeader = attributes.getRequest().getHeader("Authorization");
            if (authHeader != null) {
                // Si lo encuentra, lo imprime en amarillo en la consola y lo inyecta
                System.out.println("✅ [FEIGN] Enviando Token a otro microservicio: " + authHeader.substring(0, 20) + "...");
                template.header("Authorization", authHeader);
            } else {
                System.out.println("❌ [FEIGN] ADVERTENCIA: La petición llegó a Ventas sin Token.");
            }
        } else {
            System.out.println("❌ [FEIGN] ADVERTENCIA: No se pudo leer el contexto de la petición.");
        }
    }
}