package com.example.bankcards.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * JWT configuration properties.
 * Binds JWT-related configuration from application.yml to Java objects.
 * Contains token secrets, expiration times, and other JWT settings.
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "jwt")
public class JwtConfig {
    
    /**
     * Secret key for signing JWT tokens.
     * Must be at least 32 characters for HS256.
     */
    private String secret = "mySecretKey123456789012345678901234567890";
    
    /**
     * Access token lifetime in milliseconds.
     * Default is 24 hours (86400000 ms).
     */
    private Long expiration = 86400000L;
    
    /**
     * Refresh token lifetime in milliseconds.
     * Default is 7 days (604800000 ms).
     */
    private Long refreshExpiration = 604800000L;
    
    /**
     * Token issuer.
     */
    private String issuer = "bankcards-api";
    
    /**
     * Token audience.
     */
    private String audience = "bankcards-users";
}