package com.example.bankcards.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
@ConfigurationProperties(prefix = "jwt")
public class JwtConfig {
    
    /**
     * Секретный ключ для подписи JWT токенов.
     * Должен быть минимум 32 символа для HS256.
     */
    private String secret = "mySecretKey123456789012345678901234567890";
    
    /**
     * Время жизни access токена в миллисекундах.
     * По умолчанию 24 часа (86400000 мс).
     */
    private Long expiration = 86400000L;
    
    /**
     * Время жизни refresh токена в миллисекундах.
     * По умолчанию 7 дней (604800000 мс).
     */
    private Long refreshExpiration = 604800000L;
    
    /**
     * Issuer (издатель) токена.
     */
    private String issuer = "bankcards-api";
    
    /**
     * Audience (аудитория) токена.
     */
    private String audience = "bankcards-users";
}