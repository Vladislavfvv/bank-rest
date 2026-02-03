package com.example.bankcards.dto.authentication;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * DTO for token validation response.
 * Used to return validation results when checking JWT token validity.
 * Contains validation status and extracted user information from token.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class TokenValidationResponse {
    private boolean valid;
    private String username;
    private String role;
}
