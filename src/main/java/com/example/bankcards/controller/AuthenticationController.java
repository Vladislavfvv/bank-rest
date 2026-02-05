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
@Tag(name = "Authentication", description = "Authentication and token management")
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
        summary = "User login",
        description = "User authentication by email and password. Returns JWT tokens for API access."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Successful authentication",
            content = @Content(schema = @Schema(implementation = TokenResponse.class))
        ),
        @ApiResponse(
            responseCode = "401",
            description = "Invalid credentials"
        ),
        @ApiResponse(
            responseCode = "400",
            description = "Invalid request data"
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
        summary = "Register new user",
        description = "Create new user in the system. Returns JWT tokens for immediate API access."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "201",
            description = "User successfully registered",
            content = @Content(schema = @Schema(implementation = TokenResponse.class))
        ),
        @ApiResponse(
            responseCode = "409",
            description = "User with this email already exists"
        ),
        @ApiResponse(
            responseCode = "400",
            description = "Invalid request data"
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
        summary = "Refresh access token",
        description = "Get new access token using refresh token. Returns new pair of tokens."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Token successfully refreshed",
            content = @Content(schema = @Schema(implementation = TokenResponse.class))
        ),
        @ApiResponse(
            responseCode = "401",
            description = "Invalid refresh token"
        ),
        @ApiResponse(
            responseCode = "400",
            description = "Invalid request data"
        )
    })
    @PostMapping("/refresh")
    public ResponseEntity<TokenResponse> refreshToken(@Valid @RequestBody RefreshTokenRequest request) {
        return ResponseEntity.ok(authenticationService.refreshToken(request.getRefreshToken()));
    }
}
