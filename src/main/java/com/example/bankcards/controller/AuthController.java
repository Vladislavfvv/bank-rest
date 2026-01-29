package com.example.bankcards.controller;

import com.example.bankcards.dto.authentication.LoginRequest;
import com.example.bankcards.dto.authentication.RefreshTokenRequest;
import com.example.bankcards.dto.authentication.RegisterRequest;
import com.example.bankcards.dto.authentication.TokenResponse;
import com.example.bankcards.service.AuthenticationService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST контроллер для аутентификации и управления токенами.
 * Предоставляет endpoints для регистрации, входа, обновления токенов и валидации.
 */
@RestController
@RequestMapping("api/v1/auth")
public class AuthController {

    private final AuthenticationService authenticationService;

    public AuthController(AuthenticationService authenticationService) {
        this.authenticationService = authenticationService;
    }

    /**
     * Аутентификация пользователя по логину и паролю.
     * Возвращает access и refresh токены при успешной аутентификации.
     *
     * @param loginRequest данные для входа (login, password)
     * @return TokenResponse с access и refresh токенами
     */
    @PostMapping("/login")
    public ResponseEntity<TokenResponse> login(@Valid @RequestBody LoginRequest loginRequest) {
        return ResponseEntity.ok(authenticationService.login(loginRequest));
    }

    /**
     * Регистрация нового пользователя в auth-service.
     * Создает учетные данные в auth_db и сразу выдает JWT токены.
     * После регистрации пользователь должен самостоятельно создать профиль в user-service,
     * используя полученный токен (email будет извлечен из токена автоматически).
     *
     * @param registerRequest данные для регистрации (login, password, role)
     * @return TokenResponse с access и refresh токенами
     */
    @PostMapping("/register")
    public ResponseEntity<TokenResponse> register(@Valid @RequestBody RegisterRequest registerRequest) {
        TokenResponse tokenResponse = authenticationService.register(registerRequest);
        return ResponseEntity.status(201).body(tokenResponse);
    }

    /**
     * Обновление access токена с помощью refresh токена.
     * Возвращает новую пару access и refresh токенов.
     *
     * @param request запрос с refresh токеном
     * @return TokenResponse с новыми access и refresh токенами
     */
    @PostMapping("/refresh")
    public ResponseEntity<TokenResponse> refreshToken(@Valid @RequestBody RefreshTokenRequest request) {
        return ResponseEntity.ok(authenticationService.refreshToken(request.getRefreshToken()));
    }
}
