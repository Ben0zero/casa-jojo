package cl.elvolcan.inventario.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SwaggerConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        final String securitySchemeName = "bearerAuth";

        return new OpenAPI()
                .info(
                        new Info()
                                .title("MiniMarket - Inventario Service")
                                .version("1.0")
                                .description("""
                                        Microservicio encargado de la gestión de inventario.

                                        Funcionalidades:
                                        • Crear inventario
                                        • Consultar stock
                                        • Aumentar stock
                                        • Disminuir stock
                                        • Stock crítico

                                        Proyecto Full Stack - Duoc UC
                                        """)
                )
                // Configuración global para habilitar el botón "Authorize" en Swagger
                .addSecurityItem(new SecurityRequirement().addList(securitySchemeName))
                .components(new Components()
                        .addSecuritySchemes(securitySchemeName, new SecurityScheme()
                                .name(securitySchemeName)
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")));
    }
}