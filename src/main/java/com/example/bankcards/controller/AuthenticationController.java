package com.example.bankcards.controller;

import com.example.bankcards.dto.authentication.LoginRequest;
import com.example.bankcards.dto.authentication.RefreshTokenRequest;
import com.example.bankcards.dto.authentication.RegisterRequest;
import com.example.bankcards.dto.authentication.TokenResponse;
import com.example.bankcards.service.AuthenticationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller for authentication and token management.
 * Provides endpoints for registration, login, token refresh and validation.
 */
@Slf4j
@RestController
@RequestMapping("/auth")
@Tag(name = "Authentication", description = "Аутентификация и управление токенами")
public class AuthenticationController {

    private final AuthenticationService authenticationService;

    public AuthenticationController(AuthenticationService authenticationService) {
        this.authenticationService = authenticationService;
    }

    /**
     * User authentication by login and password.
     * Returns access and refresh tokens on successful authentication.
     *
     * @param loginRequest login data (login, password)
     * @return TokenResponse with access and refresh tokens
     */
    @Operation(
        summary = "Вход в систему",
        description = "Аутентификация пользователя по email и паролю. Возвращает JWT токены для доступа к API."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Успешная аутентификация",
            content = @Content(schema = @Schema(implementation = TokenResponse.class))
        ),
        @ApiResponse(
            responseCode = "401",
            description = "Неверные учетные данные"
        ),
        @ApiResponse(
            responseCode = "400",
            description = "Некорректные данные запроса"
        )
    })
    @PostMapping("/login")
    public ResponseEntity<TokenResponse> login(@Valid @RequestBody LoginRequest loginRequest) {
        return ResponseEntity.ok(authenticationService.login(loginRequest));
    }

    /**
     * Registration of a new user in auth-service.
     * Creates credentials in auth_db and immediately issues JWT tokens.
     * After registration, the user should create a profile in user-service,
     * using the received token (email will be extracted from token automatically).
     *
     * @param registerRequest registration data (login, password, role)
     * @return TokenResponse with access and refresh tokens
     */
    @Operation(
        summary = "Регистрация нового пользователя",
        description = "Создание нового пользователя в системе. Возвращает JWT токены для немедленного доступа к API."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "201",
            description = "Пользователь успешно зарегистрирован",
            content = @Content(schema = @Schema(implementation = TokenResponse.class))
        ),
        @ApiResponse(
            responseCode = "409",
            description = "Пользователь с таким email уже существует"
        ),
        @ApiResponse(
            responseCode = "400",
            description = "Некорректные данные запроса"
        )
    })
    @PostMapping("/register")
    public ResponseEntity<TokenResponse> register(@Valid @RequestBody RegisterRequest registerRequest) {
        log.info("Received registration request for email: {}", registerRequest.getEmail());
        TokenResponse tokenResponse = authenticationService.register(registerRequest);
        log.info("Registration successful for email: {}", registerRequest.getEmail());
        return ResponseEntity.status(201).body(tokenResponse);
    }

    /**
     * Refresh access token using refresh token.
     * Returns Ne w pair of access and refresh tokens.
     *
     * @param request request with refresh token
     * @return TokenResponse with new access and refresh tokens
     */
    @Operation(
        summary = "Обновление токена доступа",
        description = "Получение нового access токена с помощью refresh токена. Возвращает новую пару токенов."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Токен успешно обновлен",
            content = @Content(schema = @Schema(implementation = TokenResponse.class))
        ),
        @ApiResponse(
            responseCode = "401",
            description = "Недействительный refresh токен"
        ),
        @ApiResponse(
            responseCode = "400",
            description = "Некорректные данные запроса"
        )
    })
    @PostMapping("/refresh")
    public ResponseEntity<TokenResponse> refreshToken(@Valid @RequestBody RefreshTokenRequest request) {
        return ResponseEntity.ok(authenticationService.refreshToken(request.getRefreshToken()));
    }
}
