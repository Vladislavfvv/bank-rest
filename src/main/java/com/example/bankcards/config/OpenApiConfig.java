package com.example.bankcards.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * OpenAPI (Swagger) configuration for API documentation.
 * Configures API information, servers, security schemes, and JWT authentication.
 * Provides interactive API documentation accessible via Swagger UI.
 */
@Configuration
public class OpenApiConfig {

    @Value("${server.port:8080}")
    private String serverPort;

    @Value("${server.servlet.context-path:/api/v1}")
    private String contextPath;

    /**
     * Custom OpenAPI configuration bean.
     * Defines API metadata, server information, and JWT security scheme.
     * Configures Bearer token authentication for protected endpoints.
     * 
     * @return configured OpenAPI instance
     */
    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("API Управления Банковскими Картами")
                        .description("REST API для управления банковскими картами, пользователями и переводами")
                        .version("1.0.0")
                        .contact(new Contact()
                                .name("Команда Bank Cards")
                                .email("support@bankcards.com"))
                        .license(new License()
                                .name("MIT License")
                                .url("https://opensource.org/licenses/MIT")))
                .servers(List.of(
                        new Server()
                                .url("http://localhost:" + serverPort + contextPath)
                                .description("Локальный сервер разработки"),
                        new Server()
                                .url("https://api.bankcards.com" + contextPath)
                                .description("Продакшн сервер")))
                .addSecurityItem(new SecurityRequirement().addList("bearerAuth"))
                .components(new io.swagger.v3.oas.models.Components()
                        .addSecuritySchemes("bearerAuth",
                                new SecurityScheme()
                                        .type(SecurityScheme.Type.HTTP)
                                        .scheme("bearer")
                                        .bearerFormat("JWT")
                                        .description("Введите JWT токен в формате: Bearer {token}")));
    }
}