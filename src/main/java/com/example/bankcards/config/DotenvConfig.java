package com.example.bankcards.config;

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
        System.out.println("🔧 DotenvConfig activated for 'local' profile");
        
        try {
            Properties props = new Properties();
            props.load(new FileInputStream(".env"));
            
            System.out.println("✅ Found .env file, loading variables...");
            
            props.forEach((key, value) -> {
                String keyStr = key.toString();
                String valueStr = value.toString();
                
                // Skip comments and empty lines
                if (!keyStr.startsWith("#") && !keyStr.trim().isEmpty()) {
                    System.setProperty(keyStr, valueStr);
                    System.out.println("✅ Set " + keyStr + " = " + 
                        (keyStr.contains("PASSWORD") || keyStr.contains("SECRET") ? "***" : valueStr));
                }
            });
            
            System.out.println("🎯 .env file loaded successfully!");
            
        } catch (IOException e) {
            System.err.println("⚠️ Could not load .env file: " + e.getMessage());
            System.err.println("💡 Make sure .env file exists in project root");
        }
    }
}