package com.example.bankcards.security;

/**
 * Константы для системы безопасности.
 */
public final class SecurityConstants {
    
    // JWT константы
    public static final String JWT_TOKEN_PREFIX = "Bearer ";
    public static final String JWT_HEADER_STRING = "Authorization";
    
    // Роли
    public static final String ROLE_USER = "ROLE_USER";
    public static final String ROLE_ADMIN = "ROLE_ADMIN";
    
    // Публичные endpoints
    public static final String[] PUBLIC_URLS = {
        "/api/v1/auth/**",
        "/swagger-ui/**",
        "/api-docs/**",
        "/swagger-ui.html",
        "/actuator/health",
        "/h2-console/**"
    };
    
    private SecurityConstants() {
        // Утилитный класс - запрещаем создание экземпляров
    }
}