package com.example.bankcards.util;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

/**
 * Utility class for working with Spring Security and access rights checking.
 */
public class SecurityUtils {
    
    /**
     * Private constructor to prevent instantiation of utility class.
     */
    private SecurityUtils() {
        // Utility class - prohibit instance creation
    }
    
    /**
     * Checks if user is administrator.
     * Returns true if user has ADMIN role.
     */
    public static boolean isAdmin(Authentication authentication) {
        if (authentication == null) {
            return false;
        }
        return authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(authority -> authority.equals("ROLE_ADMIN"));
    }

    /**
     * Extracts user email from JWT token ("sub" claim) or from Authentication principal.
     * Throws IllegalStateException if token doesn't contain email.
     */
    public static String getEmailFromToken(Authentication authentication) {
        if (authentication == null) {
            throw new IllegalStateException("Authentication is required");
        }

        // Try to extract from JWT token first
        Jwt jwt = extractJwt(authentication);
        if (jwt != null) {
            String subject = jwt.getSubject();
            if (subject != null && !subject.isBlank()) {
                return subject;
            }
        }

        // If JWT extraction failed, try to get email from principal (username)
        // This works with UsernamePasswordAuthenticationToken created by JwtAuthenticationFilter
        if (authentication.getPrincipal() instanceof String principal && !principal.isBlank()) {
            return principal;
        }

        // If principal is not a String, try getName() which returns the principal name
        String name = authentication.getName();
        if (name != null && !name.isBlank()) {
            return name;
        }

        throw new IllegalStateException("Email not found in JWT token or authentication principal");
    }

    /**
     * Extracts JWT from Authentication object.
     * Returns JWT token or null if extraction failed.
     */
    private static Jwt extractJwt(Authentication authentication) {
        if (authentication instanceof JwtAuthenticationToken jwtAuthenticationToken) {
            return jwtAuthenticationToken.getToken();
        }
        if (authentication != null && authentication.getPrincipal() instanceof Jwt jwt) {
            return jwt;
        }
        return null;
    }

    /**
     * Checks user access to resource.
     * ADMIN has access to all resources, USER - only to their own (checked by email).
     * Returns true if user has access to resource.
     */
    public static boolean hasAccess(Authentication authentication, String resourceOwnerEmail) {
        if (authentication == null) {
            return false;
        }

        if (isAdmin(authentication)) {
            return true;
        }

        try {
            String userEmail = getEmailFromToken(authentication);
            return userEmail.equals(resourceOwnerEmail);
        } catch (IllegalStateException e) {
            return false;
        }
    }
}
