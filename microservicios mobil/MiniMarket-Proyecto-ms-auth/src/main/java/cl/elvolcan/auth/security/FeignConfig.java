package cl.elvolcan.auth.security;

import feign.RequestInterceptor;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Configuration
public class FeignConfig {

    @Bean
    public RequestInterceptor requestInterceptor() {
        return requestTemplate -> {
            
            // capturamos la petición web actual
            ServletRequestAttributes attributes =
                    (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();

            if (attributes != null) {
                // obtenemos el request original que entro a nuestro microservicio
                HttpServletRequest request = attributes.getRequest();
                
                // sacamos el token JWT del encabezado
                String token = request.getHeader("Authorization");

                // si hay un token, se lo inyectamos a la nueva petición que va a hacer Feign
                if (token != null) {
                    requestTemplate.header("Authorization", token);
                }
            }
        };
    }
}