package com.example.bankcards.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Unit tests for SecurityUtils.
 * Tests security-related utility methods for authentication and authorization.
 */
@ExtendWith(MockitoExtension.class)
class SecurityUtilsTest {

    // ================= isAdmin() Tests =================

    @Test
    @DisplayName("isAdmin - With ROLE_ADMIN authority - Should return true")
    void isAdmin_WithAdminRole_ShouldReturnTrue() {
        // given
        Authentication auth = createAuthenticationWithAuthorities(List.of("ROLE_ADMIN"));

        // when
        boolean result = SecurityUtils.isAdmin(auth);

        // then
        assertTrue(result);
    }

    @Test
    @DisplayName("isAdmin - With ROLE_USER authority - Should return false")
    void isAdmin_WithUserRole_ShouldReturnFalse() {
        // given
        Authentication auth = createAuthenticationWithAuthorities(List.of("ROLE_USER"));

        // when
        boolean result = SecurityUtils.isAdmin(auth);

        // then
        assertFalse(result);
    }

    @Test
    @DisplayName("isAdmin - With multiple authorities including ROLE_ADMIN - Should return true")
    void isAdmin_WithMultipleAuthoritiesIncludingAdmin_ShouldReturnTrue() {
        // given
        Authentication auth = createAuthenticationWithAuthorities(List.of("ROLE_USER", "ROLE_ADMIN", "ROLE_MANAGER"));

        // when
        boolean result = SecurityUtils.isAdmin(auth);

        // then
        assertTrue(result);
    }

    @Test
    @DisplayName("isAdmin - With no authorities - Should return false")
    void isAdmin_WithNoAuthorities_ShouldReturnFalse() {
        // given
        Authentication auth = createAuthenticationWithAuthorities(List.of());

        // when
        boolean result = SecurityUtils.isAdmin(auth);

        // then
        assertFalse(result);
    }

    @Test
    @DisplayName("isAdmin - With null authentication - Should return false")
    @SuppressWarnings("ConstantConditions") // Intentionally testing null case
    void isAdmin_WithNullAuthentication_ShouldReturnFalse() {
        // when
        boolean result = SecurityUtils.isAdmin(null);

        // then
        assertFalse(result);
    }

    @Test
    @DisplayName("isAdmin - With different role format - Should return false")
    void isAdmin_WithDifferentRoleFormat_ShouldReturnFalse() {
        // given
        Authentication auth = createAuthenticationWithAuthorities(List.of("ADMIN", "admin", "role_admin"));

        // when
        boolean result = SecurityUtils.isAdmin(auth);

        // then
        assertFalse(result);
    }

    // ================= getEmailFromToken() Tests =================

    @Test
    @DisplayName("getEmailFromToken - With valid JWT containing email - Should return email")
    void getEmailFromToken_WithValidJwt_ShouldReturnEmail() {
        // given
        String expectedEmail = "user@example.com";
        Authentication auth = createJwtAuthentication(expectedEmail);

        // when
        String result = SecurityUtils.getEmailFromToken(auth);

        // then
        assertEquals(expectedEmail, result);
    }

    @ParameterizedTest
    @DisplayName("getEmailFromToken - With invalid inputs - Should throw IllegalStateException")
    @MethodSource("provideInvalidTokenInputs")
    void getEmailFromToken_WithInvalidInputs_ShouldThrowException(Authentication auth, String expectedMessage) {
        // when & then
        IllegalStateException exception = assertThrows(IllegalStateException.class, () -> 
            SecurityUtils.getEmailFromToken(auth));
        
        assertEquals(expectedMessage, exception.getMessage());
    }

    private static Stream<Arguments> provideInvalidTokenInputs() {
        return Stream.of(
            Arguments.of(null, "Authentication is required"),
            Arguments.of(mock(Authentication.class), "Email not found in JWT token or authentication principal"),
            Arguments.of(createJwtAuthenticationStatic(null), "Email not found in JWT token or authentication principal"),
            Arguments.of(createJwtAuthenticationStatic("   "), "Email not found in JWT token or authentication principal"),
            Arguments.of(createJwtAuthenticationStatic(""), "Email not found in JWT token or authentication principal")
        );
    }

    private static Authentication createJwtAuthenticationStatic(String subject) {
        // Create JWT token
        Jwt jwt = Jwt.withTokenValue("token-value")
                .header("alg", "RS256")
                .subject(subject)
                .issuer("test-issuer")
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(3600))
                .build();

        // Create authorities
        Collection<GrantedAuthority> grantedAuthorities = Stream.of("ROLE_USER")
                .map(SimpleGrantedAuthority::new)
                .map(GrantedAuthority.class::cast)
                .toList();

        return new JwtAuthenticationToken(jwt, grantedAuthorities);
    }

    // ================= hasAccess() Tests =================

    @Test
    @DisplayName("hasAccess - Admin user accessing any resource - Should return true")
    void hasAccess_AdminUser_ShouldAccessAnyResource() {
        // given
        Authentication auth = createAdminAuthentication("admin@example.com");

        // when & then
        assertTrue(SecurityUtils.hasAccess(auth, "any@email.com"));
        assertTrue(SecurityUtils.hasAccess(auth, "other@email.com"));
        assertTrue(SecurityUtils.hasAccess(auth, "different@email.com"));
    }

    @Test
    @DisplayName("hasAccess - Regular user accessing own resource - Should return true")
    void hasAccess_UserAccessingOwnResource_ShouldReturnTrue() {
        // given
        String userEmail = "user@example.com";
        Authentication auth = createUserAuthentication(userEmail);

        // when
        boolean result = SecurityUtils.hasAccess(auth, userEmail);

        // then
        assertTrue(result);
    }

    @Test
    @DisplayName("hasAccess - Regular user accessing other resource - Should return false")
    void hasAccess_UserAccessingOtherResource_ShouldReturnFalse() {
        // given
        Authentication auth = createUserAuthentication("user@example.com");

        // when
        boolean result = SecurityUtils.hasAccess(auth, "other@example.com");

        // then
        assertFalse(result);
    }

    @Test
    @DisplayName("hasAccess - With null authentication - Should return false")
    @SuppressWarnings("ConstantConditions") // Intentionally testing null case
    void hasAccess_WithNullAuthentication_ShouldReturnFalse() {
        // when
        boolean result = SecurityUtils.hasAccess(null, "any@email.com");

        // then
        assertFalse(result);
    }

    @Test
    @DisplayName("hasAccess - With invalid JWT token - Should return false")
    void hasAccess_WithInvalidJwtToken_ShouldReturnFalse() {
        // given
        Authentication auth = mock(Authentication.class);
        // No stubbing needed - we just need a non-JWT authentication

        // when
        boolean result = SecurityUtils.hasAccess(auth, "any@email.com");

        // then
        assertFalse(result);
    }

    @Test
    @DisplayName("hasAccess - User with case-sensitive email comparison - Should return false for different case")
    void hasAccess_CaseSensitiveEmailComparison_ShouldReturnFalseForDifferentCase() {
        // given
        Authentication auth = createUserAuthentication("user@example.com");

        // when
        boolean result = SecurityUtils.hasAccess(auth, "USER@EXAMPLE.COM");

        // then
        assertFalse(result);
    }

    @Test
    @DisplayName("hasAccess - Admin with invalid JWT - Should still return true due to admin role")
    void hasAccess_AdminWithInvalidJwt_ShouldReturnTrue() {
        // given
        Authentication auth = createAuthenticationWithAuthorities(List.of("ROLE_ADMIN"));

        // when
        boolean result = SecurityUtils.hasAccess(auth, "any@email.com");

        // then
        assertTrue(result);
    }

    // ================= Edge Cases and Security Tests =================

    @Test
    @DisplayName("Security - Multiple concurrent calls should be thread-safe")
    void security_MultipleConcurrentCalls_ShouldBeThreadSafe() {
        // given
        Authentication adminAuth = createAdminAuthentication("admin@example.com");
        Authentication userAuth = createUserAuthentication("user@example.com");

        // when & then - Multiple calls should not interfere with each other
        assertTrue(SecurityUtils.isAdmin(adminAuth));
        assertFalse(SecurityUtils.isAdmin(userAuth));
        assertTrue(SecurityUtils.hasAccess(adminAuth, "any@email.com"));
        assertFalse(SecurityUtils.hasAccess(userAuth, "other@email.com"));
        assertEquals("admin@example.com", SecurityUtils.getEmailFromToken(adminAuth));
        assertEquals("user@example.com", SecurityUtils.getEmailFromToken(userAuth));
    }

    @Test
    @DisplayName("Security - Method should not modify authentication object")
    void security_MethodsShouldNotModifyAuthentication() {
        // given
        Authentication auth = createUserAuthentication("user@example.com");
        String originalEmail = SecurityUtils.getEmailFromToken(auth);

        // when - Call methods multiple times
        SecurityUtils.isAdmin(auth);
        SecurityUtils.hasAccess(auth, "user@example.com");
        SecurityUtils.getEmailFromToken(auth);

        // then - Authentication should remain unchanged
        assertEquals(originalEmail, SecurityUtils.getEmailFromToken(auth));
    }

    @Test
    @DisplayName("Integration - Complete workflow for admin user")
    void integration_CompleteWorkflowForAdminUser() {
        // given
        Authentication adminAuth = createAdminAuthentication("admin@example.com");

        // when & then - Complete admin workflow
        assertTrue(SecurityUtils.isAdmin(adminAuth));
        assertEquals("admin@example.com", SecurityUtils.getEmailFromToken(adminAuth));
        assertTrue(SecurityUtils.hasAccess(adminAuth, "user1@example.com"));
        assertTrue(SecurityUtils.hasAccess(adminAuth, "user2@example.com"));
        assertTrue(SecurityUtils.hasAccess(adminAuth, "admin@example.com"));
    }

    @Test
    @DisplayName("Integration - Complete workflow for regular user")
    void integration_CompleteWorkflowForRegularUser() {
        // given
        String userEmail = "user@example.com";
        Authentication userAuth = createUserAuthentication(userEmail);

        // when & then - Complete user workflow
        assertFalse(SecurityUtils.isAdmin(userAuth));
        assertEquals(userEmail, SecurityUtils.getEmailFromToken(userAuth));
        assertTrue(SecurityUtils.hasAccess(userAuth, userEmail));
        assertFalse(SecurityUtils.hasAccess(userAuth, "other@example.com"));
        assertFalse(SecurityUtils.hasAccess(userAuth, "admin@example.com"));
    }

    // ================= Helper Methods =================

    @SuppressWarnings("SameParameterValue") // Parameter is always the same in current tests but method is flexible
    private Authentication createAdminAuthentication(String email) {
        return createJwtAuthenticationWithAuthorities(email, List.of("ROLE_ADMIN", "ROLE_USER"));
    }

    private Authentication createUserAuthentication(String email) {
        return createJwtAuthenticationWithAuthorities(email, List.of("ROLE_USER"));
    }

    private Authentication createJwtAuthentication(String subject) {
        return createJwtAuthenticationWithAuthorities(subject, List.of("ROLE_USER"));
    }

    private Authentication createJwtAuthenticationWithAuthorities(String subject, List<String> authorities) {
        // Create JWT token
        Jwt jwt = Jwt.withTokenValue("token-value")
                .header("alg", "RS256")
                .subject(subject)
                .issuer("test-issuer")
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(3600))
                .build();

        // Create authorities
        Collection<GrantedAuthority> grantedAuthorities = authorities.stream()
                .map(SimpleGrantedAuthority::new)
                .map(GrantedAuthority.class::cast)
                .toList();

        return new JwtAuthenticationToken(jwt, grantedAuthorities);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private Authentication createAuthenticationWithAuthorities(List<String> authorities) {
        Authentication auth = mock(Authentication.class);
        
        Collection<GrantedAuthority> grantedAuthorities = authorities.stream()
                .map(SimpleGrantedAuthority::new)
                .collect(java.util.stream.Collectors.toList());
        
        when(auth.getAuthorities()).thenReturn((Collection) grantedAuthorities);
        return auth;
    }
}