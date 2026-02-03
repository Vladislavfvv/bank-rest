package com.example.bankcards.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import jakarta.annotation.PostConstruct;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;

/**
 * Configuration for loading environment variables from .env file during local development.
 * Only active for 'local' profile to avoid conflicts with Docker Compose or production environments.
 * Reads .env file from project root and sets system properties for Spring Boot configuration.
 */
@Slf4j
@Configuration
@Profile("local")
public class DotenvConfig {

    /**
     * Loads environment variables from .env file after bean construction.
     * Parses .env file and sets system properties for each key-value pair.
     * Masks sensitive values (passwords, secrets) in console output for security.
     * Gracefully handles missing .env file with informative error message.
     */
    @PostConstruct
    public void loadDotenv() {
        log.info("🔧 DotenvConfig activated for 'local' profile");
        
        try (FileInputStream fileInputStream = new FileInputStream(".env")) {
            Properties props = new Properties();
            props.load(fileInputStream);
            
            log.info("✅ Found .env file, loading variables...");
            
            props.forEach((key, value) -> {
                String keyStr = key.toString();
                String valueStr = value.toString();
                
                // Skip comments and empty lines
                if (!keyStr.startsWith("#") && !keyStr.trim().isEmpty()) {
                    System.setProperty(keyStr, valueStr);
                    log.info("✅ Set {} = {}", keyStr, 
                        (keyStr.contains("PASSWORD") || keyStr.contains("SECRET") ? "***" : valueStr));
                }
            });
            
            log.info("🎯 .env file loaded successfully!");
            
        } catch (IOException e) {
            log.warn("⚠️ Could not load .env file: {}", e.getMessage());
            log.info("💡 Make sure .env file exists in project root");
        }
    }
}