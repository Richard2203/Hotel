package org.lopez.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuración de Swagger UI / OpenAPI 3.
 *
 * Acceso: http://localhost:8080/swagger-ui.html
 *
 * Para endpoints protegidos:
 *  1. Llama a POST /api/auth/login
 *  2. Copia el token JWT de la respuesta
 *  3. Presiona "Authorize" en Swagger UI
 *  4. Ingresa:  Bearer <token>
 */
@Configuration
public class OpenApiConfig {

    private static final String SECURITY_SCHEME = "bearerAuth";

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                // ── Info general ──────────────────────────────────────
                .info(new Info()
                        .title("Hotel API — ESCOM IPN")
                        .description(
                            "API REST para la gestión de un sistema hotelero. " +
                            "Permite administrar habitaciones, reservaciones y usuarios.\n\n" +
                            "## Autenticación\n" +
                            "Esta API usa **JWT Bearer Token**. Para acceder a endpoints protegidos:\n" +
                            "1. Ejecuta `POST /api/auth/login` con tus credenciales.\n" +
                            "2. Copia el `token` de la respuesta.\n" +
                            "3. Presiona el botón **Authorize** (🔒) e ingresa: `Bearer <token>`.\n\n" +
                            "**Alumno:** López García Ricardo  \n" +
                            "**Materia:** Web Client and Backend Development Framework  \n" +
                            "**Profesor:** M. en C. José Asunción Enríquez Zárate"
                        )
                        .version("1.0.0")
                        .contact(new Contact()
                                .name("Ricardo López García")
                                .email("logari.1805@gmail.com"))
                        .license(new License()
                                .name("ESCOM IPN — Uso académico")
                                .url("https://www.escom.ipn.mx/"))
                )
                // ── Esquema de seguridad JWT ──────────────────────────
                .addSecurityItem(new SecurityRequirement().addList(SECURITY_SCHEME))
                .components(new Components()
                        .addSecuritySchemes(SECURITY_SCHEME,
                                new SecurityScheme()
                                        .name(SECURITY_SCHEME)
                                        .type(SecurityScheme.Type.HTTP)
                                        .scheme("bearer")
                                        .bearerFormat("JWT")
                                        .description("Ingresa el token JWT obtenido en /api/auth/login")
                        )
                );
    }
}
