package com.example.bankcards.dto.authentication;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * DTO for authentication response containing JWT tokens.
 * Used to return access and refresh tokens after successful login or token refresh.
 * Contains token information including expiration time and token type.
 */
@NoArgsConstructor
@Getter
@Setter
public class TokenResponse {
    private String accessToken;
    private String refreshToken;
    private String type = "Bearer";
    private Long expiresIn;

    /**
     * Constructor for creating token response with access token, refresh token and expiration time.
     * Token type is automatically set to "Bearer".
     * 
     * @param accessToken JWT access token for API authentication
     * @param refreshToken JWT refresh token for obtaining new access tokens
     * @param expiresIn access token expiration time in milliseconds
     */
    public TokenResponse(String accessToken, String refreshToken, Long expiresIn) {
        this.accessToken = accessToken;
        this.refreshToken = refreshToken;
        this.expiresIn = expiresIn;
    }
}
