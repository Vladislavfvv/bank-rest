package com.example.bankcards.config;

import com.example.bankcards.service.AuthenticationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final AuthenticationService authenticationService;

    @Override
    public void run(String... args) throws Exception {
        try {
            // Создаем админа по умолчанию, если его еще нет
            authenticationService.createAdmin(
                "admin@bankcards.com",
                "Admin123!",
                "System",
                "Administrator"
            );
            log.info("Default admin created: admin@bankcards.com / Admin123!");
        } catch (Exception e) {
            log.info("Default admin already exists or creation failed: {}", e.getMessage());
        }
    }
}