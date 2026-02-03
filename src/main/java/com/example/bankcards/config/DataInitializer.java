package com.example.bankcards.config;

import com.example.bankcards.exception.UserAlreadyExistsException;
import com.example.bankcards.service.AuthenticationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

/**
 * Data initialization component that runs on application startup.
 * Implements CommandLineRunner to execute initialization logic after Spring context is loaded.
 * Creates default admin user if it doesn't exist in the database.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final AuthenticationService authenticationService;

    /**
     * Executes initialization logic on application startup.
     * Creates default admin user with predefined credentials if it doesn't exist.
     * Logs success or skips creation if admin already exists.
     * 
     * @param args command line arguments (not used)
     * @throws Exception if initialization fails
     */
    @SuppressWarnings("RedundantThrows")
    @Override
    public void run(String... args) throws Exception {
        // Create default admin if it doesn't exist yet
        try {
            authenticationService.createAdmin(
                "admin@bankcards.com",
                "Admin123!",
                "System",
                "Administrator"
            );
            log.info("Default admin created: admin@bankcards.com / Admin123!");
        } catch (UserAlreadyExistsException e) {
            log.debug("Default admin already exists: admin@bankcards.com");
        }
    }
}