package com.example.bankcards.controller;

import com.example.bankcards.dto.authentication.LoginRequest;
import com.example.bankcards.dto.authentication.RefreshTokenRequest;
import com.example.bankcards.dto.authentication.RegisterRequest;
import com.example.bankcards.dto.authentication.TokenResponse;
import com.example.bankcards.service.AuthenticationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

/**
 * Unit tests for AuthenticationController.
 * Tests authentication endpoints without Spring context.
 */
@ExtendWith(MockitoExtension.class)
class AuthenticationControllerTest {

    @Mock
    private AuthenticationService authenticationService;

    @InjectMocks
    private AuthenticationController authenticationController;

    private LoginRequest loginRequest;
    private RegisterRequest registerRequest;
    private RefreshTokenRequest refreshTokenRequest;
    private TokenResponse tokenResponse;

    @BeforeEach
    void setUp() {
        // Setup LoginRequest
        loginRequest = new LoginRequest();
        loginRequest.setEmail("test@example.com");
        loginRequest.setPassword("password123");

        // Setup RegisterRequest
        registerRequest = new RegisterRequest();
        registerRequest.setFirstName("Ivan");
        registerRequest.setLastName("Ivanov");
        registerRequest.setBirthDate(LocalDate.of(1990, 1, 1));
        registerRequest.setEmail("ivan.ivanov@example.com");
        registerRequest.setPhoneNumber("+1234567890");
        registerRequest.setPassword("Password123");

        // Setup RefreshTokenRequest
        refreshTokenRequest = new RefreshTokenRequest();
        refreshTokenRequest.setRefreshToken("refresh-token-123");

        // Setup TokenResponse
        tokenResponse = new TokenResponse();
        tokenResponse.setAccessToken("access-token-123");
        tokenResponse.setRefreshToken("refresh-token-123");
        tokenResponse.setType("Bearer");
        tokenResponse.setExpiresIn(3600L);
    }

    // ================= POST /api/v1/auth/login Tests =================

    @Test
    @DisplayName("POST /api/v1/auth/login - Success")
    void login_Success() {
        // given
        when(authenticationService.login(any(LoginRequest.class))).thenReturn(tokenResponse);

        // when
        ResponseEntity<TokenResponse> response = authenticationController.login(loginRequest);

        // then
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("access-token-123", response.getBody().getAccessToken());
        assertEquals("refresh-token-123", response.getBody().getRefreshToken());
        assertEquals("Bearer", response.getBody().getType());
        assertEquals(3600L, response.getBody().getExpiresIn());

        verify(authenticationService).login(loginRequest);
    }

    @Test
    @DisplayName("POST /api/v1/auth/login - Service returns different token")
    void login_DifferentToken() {
        // given
        TokenResponse differentToken = new TokenResponse();
        differentToken.setAccessToken("different-access-token");
        differentToken.setRefreshToken("different-refresh-token");
        differentToken.setType("Bearer");
        differentToken.setExpiresIn(7200L);

        when(authenticationService.login(any(LoginRequest.class))).thenReturn(differentToken);

        // when
        ResponseEntity<TokenResponse> response = authenticationController.login(loginRequest);

        // then
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("different-access-token", response.getBody().getAccessToken());
        assertEquals("different-refresh-token", response.getBody().getRefreshToken());
        assertEquals(7200L, response.getBody().getExpiresIn());

        verify(authenticationService).login(loginRequest);
    }

    @Test
    @DisplayName("POST /api/v1/auth/login - Service method called with correct parameters")
    void login_ServiceCalledWithCorrectParameters() {
        // given
        LoginRequest specificRequest = new LoginRequest();
        specificRequest.setEmail("specific@example.com");
        specificRequest.setPassword("specificPassword");

        when(authenticationService.login(any(LoginRequest.class))).thenReturn(tokenResponse);

        // when
        authenticationController.login(specificRequest);

        // then
        verify(authenticationService).login(specificRequest);
        verify(authenticationService, times(1)).login(any(LoginRequest.class));
    }

    // ================= POST /api/v1/auth/register Tests =================

    @Test
    @DisplayName("POST /api/v1/auth/register - Success")
    void register_Success() {
        // given
        when(authenticationService.register(any(RegisterRequest.class))).thenReturn(tokenResponse);

        // when
        ResponseEntity<TokenResponse> response = authenticationController.register(registerRequest);

        // then
        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("access-token-123", response.getBody().getAccessToken());
        assertEquals("refresh-token-123", response.getBody().getRefreshToken());
        assertEquals("Bearer", response.getBody().getType());
        assertEquals(3600L, response.getBody().getExpiresIn());

        verify(authenticationService).register(registerRequest);
    }

    @Test
    @DisplayName("POST /api/v1/auth/register - Returns 201 Created status")
    void register_Returns201Status() {
        // given
        when(authenticationService.register(any(RegisterRequest.class))).thenReturn(tokenResponse);

        // when
        ResponseEntity<TokenResponse> response = authenticationController.register(registerRequest);

        // then
        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        verify(authenticationService).register(registerRequest);
    }

    @Test
    @DisplayName("POST /api/v1/auth/register - Service method called with correct parameters")
    void register_ServiceCalledWithCorrectParameters() {
        // given
        RegisterRequest specificRequest = new RegisterRequest();
        specificRequest.setFirstName("Natasha");
        specificRequest.setLastName("Rostova");
        specificRequest.setBirthDate(LocalDate.of(1985, 5, 15));
        specificRequest.setEmail("natasha.rostova@example.com");
        specificRequest.setPhoneNumber("+9876543210");
        specificRequest.setPassword("SecurePass123");

        when(authenticationService.register(any(RegisterRequest.class))).thenReturn(tokenResponse);

        // when
        authenticationController.register(specificRequest);

        // then
        verify(authenticationService).register(specificRequest);
        verify(authenticationService, times(1)).register(any(RegisterRequest.class));
    }

    @Test
    @DisplayName("POST /api/v1/auth/register - Service returns new user token")
    void register_ServiceReturnsNewUserToken() {
        // given
        TokenResponse newUserToken = new TokenResponse();
        newUserToken.setAccessToken("new-user-access-token");
        newUserToken.setRefreshToken("new-user-refresh-token");
        newUserToken.setType("Bearer");
        newUserToken.setExpiresIn(3600L);

        when(authenticationService.register(any(RegisterRequest.class))).thenReturn(newUserToken);

        // when
        ResponseEntity<TokenResponse> response = authenticationController.register(registerRequest);

        // then
        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("new-user-access-token", response.getBody().getAccessToken());
        assertEquals("new-user-refresh-token", response.getBody().getRefreshToken());

        verify(authenticationService).register(registerRequest);
    }

    // ================= POST /api/v1/auth/refresh Tests =================

    @Test
    @DisplayName("POST /api/v1/auth/refresh - Success")
    void refreshToken_Success() {
        // given
        TokenResponse refreshedToken = new TokenResponse();
        refreshedToken.setAccessToken("new-access-token-456");
        refreshedToken.setRefreshToken("new-refresh-token-456");
        refreshedToken.setType("Bearer");
        refreshedToken.setExpiresIn(3600L);

        when(authenticationService.refreshToken("refresh-token-123")).thenReturn(refreshedToken);

        // when
        ResponseEntity<TokenResponse> response = authenticationController.refreshToken(refreshTokenRequest);

        // then
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("new-access-token-456", response.getBody().getAccessToken());
        assertEquals("new-refresh-token-456", response.getBody().getRefreshToken());
        assertEquals("Bearer", response.getBody().getType());
        assertEquals(3600L, response.getBody().getExpiresIn());

        verify(authenticationService).refreshToken("refresh-token-123");
    }

    @Test
    @DisplayName("POST /api/v1/auth/refresh - Service method called with correct token")
    void refreshToken_ServiceCalledWithCorrectToken() {
        // given
        String specificRefreshToken = "specific-refresh-token-789";
        RefreshTokenRequest specificRequest = new RefreshTokenRequest();
        specificRequest.setRefreshToken(specificRefreshToken);

        when(authenticationService.refreshToken(specificRefreshToken)).thenReturn(tokenResponse);

        // when
        authenticationController.refreshToken(specificRequest);

        // then
        verify(authenticationService).refreshToken(specificRefreshToken);
        verify(authenticationService, times(1)).refreshToken(anyString());
    }

    @Test
    @DisplayName("POST /api/v1/auth/refresh - Different refresh token produces different result")
    void refreshToken_DifferentTokenProducesDifferentResult() {
        // given
        String refreshToken1 = "refresh-token-1";
        String refreshToken2 = "refresh-token-2";

        TokenResponse response1 = new TokenResponse();
        response1.setAccessToken("access-token-1");
        response1.setRefreshToken("new-refresh-token-1");

        TokenResponse response2 = new TokenResponse();
        response2.setAccessToken("access-token-2");
        response2.setRefreshToken("new-refresh-token-2");

        when(authenticationService.refreshToken(refreshToken1)).thenReturn(response1);
        when(authenticationService.refreshToken(refreshToken2)).thenReturn(response2);

        RefreshTokenRequest request1 = new RefreshTokenRequest();
        request1.setRefreshToken(refreshToken1);

        RefreshTokenRequest request2 = new RefreshTokenRequest();
        request2.setRefreshToken(refreshToken2);

        // when
        ResponseEntity<TokenResponse> result1 = authenticationController.refreshToken(request1);
        ResponseEntity<TokenResponse> result2 = authenticationController.refreshToken(request2);

        // then
        assertNotNull(result1.getBody());
        assertNotNull(result2.getBody());
        assertEquals("access-token-1", result1.getBody().getAccessToken());
        assertEquals("access-token-2", result2.getBody().getAccessToken());

        verify(authenticationService).refreshToken(refreshToken1);
        verify(authenticationService).refreshToken(refreshToken2);
    }

    // ================= Edge Cases and Validation Tests =================

    @Test
    @DisplayName("All endpoints return proper HTTP status codes")
    void allEndpoints_ReturnProperStatusCodes() {
        // given
        when(authenticationService.login(any(LoginRequest.class))).thenReturn(tokenResponse);
        when(authenticationService.register(any(RegisterRequest.class))).thenReturn(tokenResponse);
        when(authenticationService.refreshToken(anyString())).thenReturn(tokenResponse);

        // when & then
        ResponseEntity<TokenResponse> loginResponse = authenticationController.login(loginRequest);
        assertEquals(HttpStatus.OK, loginResponse.getStatusCode());

        ResponseEntity<TokenResponse> registerResponse = authenticationController.register(registerRequest);
        assertEquals(HttpStatus.CREATED, registerResponse.getStatusCode());

        ResponseEntity<TokenResponse> refreshResponse = authenticationController.refreshToken(refreshTokenRequest);
        assertEquals(HttpStatus.OK, refreshResponse.getStatusCode());
    }

    @Test
    @DisplayName("Controller properly delegates to AuthenticationService")
    void controller_ProperlyDelegatesToService() {
        // given
        when(authenticationService.login(any(LoginRequest.class))).thenReturn(tokenResponse);
        when(authenticationService.register(any(RegisterRequest.class))).thenReturn(tokenResponse);
        when(authenticationService.refreshToken(anyString())).thenReturn(tokenResponse);

        // when
        authenticationController.login(loginRequest);
        authenticationController.register(registerRequest);
        authenticationController.refreshToken(refreshTokenRequest);

        // then
        verify(authenticationService).login(loginRequest);
        verify(authenticationService).register(registerRequest);
        verify(authenticationService).refreshToken(refreshTokenRequest.getRefreshToken());

        // Verify no other interactions
        verifyNoMoreInteractions(authenticationService);
    }

    @Test
    @DisplayName("Controller handles null responses from service gracefully")
    void controller_HandlesNullResponsesGracefully() {
        // given
        when(authenticationService.login(any(LoginRequest.class))).thenReturn(null);

        // when
        ResponseEntity<TokenResponse> response = authenticationController.login(loginRequest);

        // then
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNull(response.getBody());

        verify(authenticationService).login(loginRequest);
    }
}