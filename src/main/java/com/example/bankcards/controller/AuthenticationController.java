package com.example.bankcards.controller;

import com.example.bankcards.dto.authentication.LoginRequest;
import com.example.bankcards.dto.authentication.RefreshTokenRequest;
import com.example.bankcards.dto.authentication.RegisterRequest;
import com.example.bankcards.dto.authentication.TokenResponse;
import com.example.bankcards.service.AuthenticationService;
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
    @PostMapping("/refresh")
    public ResponseEntity<TokenResponse> refreshToken(@Valid @RequestBody RefreshTokenRequest request) {
        return ResponseEntity.ok(authenticationService.refreshToken(request.getRefreshToken()));
    }
}
