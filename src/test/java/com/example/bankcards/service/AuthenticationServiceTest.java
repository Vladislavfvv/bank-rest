package com.example.bankcards.service;

import com.example.bankcards.dto.authentication.LoginRequest;
import com.example.bankcards.dto.authentication.RegisterRequest;
import com.example.bankcards.dto.authentication.TokenResponse;
import com.example.bankcards.dto.authentication.TokenValidationResponse;
import com.example.bankcards.entity.Role;
import com.example.bankcards.entity.User;
import com.example.bankcards.exception.UserAlreadyExistsException;
import com.example.bankcards.exception.UserNotFoundException;
import com.example.bankcards.repository.UserRepository;
import com.example.bankcards.security.JwtTokenProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthenticationServiceTest {

    @InjectMocks
    private AuthenticationService authenticationService;

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtTokenProvider jwtTokenProvider;

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private Authentication authentication;

    private User user;
    private LoginRequest loginRequest;
    private RegisterRequest registerRequest;

    @BeforeEach
    void setUp() {
        user = new User();
        user.setId(1L);
        user.setEmail("test@example.com");
        user.setPassword("encodedPassword");
        user.setFirstName("Ivan");
        user.setLastName("Ivanov");
        user.setBirthDate(LocalDate.of(1990, 1, 1));
        user.setPhoneNumber("+1234567890");
        user.setRole(Role.ROLE_USER);
        user.setCreatedAt(LocalDateTime.now());
        user.setIsActive(true);

        loginRequest = new LoginRequest();
        loginRequest.setEmail("test@example.com");
        loginRequest.setPassword("password123");

        registerRequest = new RegisterRequest();
        registerRequest.setEmail("test@example.com");
        registerRequest.setPassword("Password123");
        registerRequest.setFirstName("Ivan");
        registerRequest.setLastName("Ivanov");
        registerRequest.setBirthDate(LocalDate.of(1990, 1, 1));
        registerRequest.setPhoneNumber("+1234567890");
    }

    // ================= login Tests =================

    @DisplayName("login - Valid credentials - Returns TokenResponse")
    @Test
    void login_ValidCredentials_ReturnsTokenResponse() {
        // given
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(authentication);
        when(userRepository.findByEmailJPQL(loginRequest.getEmail()))
                .thenReturn(Optional.of(user));
        when(jwtTokenProvider.generateAccessToken(user.getEmail(), user.getRole()))
                .thenReturn("accessToken");
        when(jwtTokenProvider.generateRefreshToken(user.getEmail(), user.getRole()))
                .thenReturn("refreshToken");
        when(jwtTokenProvider.getJwtExpiration()).thenReturn(3600000L);

        // when
        TokenResponse result = authenticationService.login(loginRequest);

        // then
        assertNotNull(result);
        assertEquals("accessToken", result.getAccessToken());
        assertEquals("refreshToken", result.getRefreshToken());
        assertEquals("Bearer", result.getType());
        assertEquals(3600L, result.getExpiresIn());

        verify(authenticationManager).authenticate(any(UsernamePasswordAuthenticationToken.class));
        verify(userRepository).findByEmailJPQL(loginRequest.getEmail());
        verify(jwtTokenProvider).generateAccessToken(user.getEmail(), user.getRole());
        verify(jwtTokenProvider).generateRefreshToken(user.getEmail(), user.getRole());
    }

    @DisplayName("login - Authentication fails - Throws BadCredentialsException")
    @Test
    void login_AuthenticationFails_ThrowsBadCredentialsException() {
        // given
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenThrow(new BadCredentialsException("Invalid credentials"));

        // when & then
        BadCredentialsException exception = assertThrows(
                BadCredentialsException.class,
                () -> authenticationService.login(loginRequest)
        );

        assertEquals("Invalid email or password", exception.getMessage());
        verify(authenticationManager).authenticate(any(UsernamePasswordAuthenticationToken.class));
        verify(userRepository, never()).findByEmailJPQL(anyString());
        verify(jwtTokenProvider, never()).generateAccessToken(anyString(), any(Role.class));
    }

    @DisplayName("login - User not found after authentication - Throws BadCredentialsException")
    @Test
    void login_UserNotFoundAfterAuthentication_ThrowsBadCredentialsException() {
        // given
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(authentication);
        when(userRepository.findByEmailJPQL(loginRequest.getEmail()))
                .thenReturn(Optional.empty());

        // when & then
        BadCredentialsException exception = assertThrows(
                BadCredentialsException.class,
                () -> authenticationService.login(loginRequest)
        );

        // Exception is caught in catch block and replaced with generic message
        assertEquals("Invalid email or password", exception.getMessage());
        verify(authenticationManager).authenticate(any(UsernamePasswordAuthenticationToken.class));
        verify(userRepository).findByEmailJPQL(loginRequest.getEmail());
        verify(jwtTokenProvider, never()).generateAccessToken(anyString(), any(Role.class));
    }

    // ================= register Tests =================

    @DisplayName("register - Valid request - Returns TokenResponse")
    @Test
    void register_ValidRequest_ReturnsTokenResponse() {
        // given
        when(userRepository.findByEmailJPQL(registerRequest.getEmail()))
                .thenReturn(Optional.empty());
        when(passwordEncoder.encode(registerRequest.getPassword()))
                .thenReturn("encodedPassword");
        when(userRepository.save(any(User.class))).thenReturn(user);
        when(jwtTokenProvider.generateAccessToken(anyString(), any(Role.class)))
                .thenReturn("accessToken");
        when(jwtTokenProvider.generateRefreshToken(anyString(), any(Role.class)))
                .thenReturn("refreshToken");
        when(jwtTokenProvider.getJwtExpiration()).thenReturn(3600000L);

        // when
        TokenResponse result = authenticationService.register(registerRequest);

        // then
        assertNotNull(result);
        assertEquals("accessToken", result.getAccessToken());
        assertEquals("refreshToken", result.getRefreshToken());
        assertEquals("Bearer", result.getType());
        assertEquals(3600L, result.getExpiresIn());

        verify(userRepository).findByEmailJPQL(registerRequest.getEmail());
        verify(passwordEncoder).encode(registerRequest.getPassword());
        verify(userRepository).save(any(User.class));
        verify(jwtTokenProvider).generateAccessToken(anyString(), eq(Role.ROLE_USER));
        verify(jwtTokenProvider).generateRefreshToken(anyString(), eq(Role.ROLE_USER));
    }

    @DisplayName("register - User already exists - Throws UserAlreadyExistsException")
    @Test
    void register_UserAlreadyExists_ThrowsUserAlreadyExistsException() {
        // given
        when(userRepository.findByEmailJPQL(registerRequest.getEmail()))
                .thenReturn(Optional.of(user));

        // when & then
        UserAlreadyExistsException exception = assertThrows(
                UserAlreadyExistsException.class,
                () -> authenticationService.register(registerRequest)
        );

        assertTrue(exception.getMessage().contains(registerRequest.getEmail()));
        verify(userRepository).findByEmailJPQL(registerRequest.getEmail());
        verify(passwordEncoder, never()).encode(anyString());
        verify(userRepository, never()).save(any(User.class));
        verify(jwtTokenProvider, never()).generateAccessToken(anyString(), any(Role.class));
    }

    // ================= refreshToken Tests =================

    @DisplayName("refreshToken - Valid refresh token - Returns new TokenResponse")
    @Test
    void refreshToken_ValidRefreshToken_ReturnsNewTokenResponse() {
        // given
        String refreshToken = "validRefreshToken";
        when(jwtTokenProvider.validateToken(refreshToken)).thenReturn(true);
        when(jwtTokenProvider.getUsernameFromToken(refreshToken)).thenReturn(user.getEmail());
        when(jwtTokenProvider.getRoleFromToken(refreshToken)).thenReturn(Role.ROLE_USER.name());
        when(userRepository.findByEmailJPQL(user.getEmail())).thenReturn(Optional.of(user));
        when(jwtTokenProvider.generateAccessToken(user.getEmail(), user.getRole()))
                .thenReturn("newAccessToken");
        when(jwtTokenProvider.generateRefreshToken(user.getEmail(), user.getRole()))
                .thenReturn("newRefreshToken");
        when(jwtTokenProvider.getJwtExpiration()).thenReturn(3600000L);

        // when
        TokenResponse result = authenticationService.refreshToken(refreshToken);

        // then
        assertNotNull(result);
        assertEquals("newAccessToken", result.getAccessToken());
        assertEquals("newRefreshToken", result.getRefreshToken());
        assertEquals("Bearer", result.getType());
        assertEquals(3600L, result.getExpiresIn());

        verify(jwtTokenProvider).validateToken(refreshToken);
        verify(jwtTokenProvider).getUsernameFromToken(refreshToken);
        verify(jwtTokenProvider).getRoleFromToken(refreshToken);
        verify(userRepository).findByEmailJPQL(user.getEmail());
        verify(jwtTokenProvider).generateAccessToken(user.getEmail(), user.getRole());
        verify(jwtTokenProvider).generateRefreshToken(user.getEmail(), user.getRole());
    }

    @DisplayName("refreshToken - Invalid refresh token - Throws BadCredentialsException")
    @Test
    void refreshToken_InvalidRefreshToken_ThrowsBadCredentialsException() {
        // given
        String invalidRefreshToken = "invalidRefreshToken";
        when(jwtTokenProvider.validateToken(invalidRefreshToken)).thenReturn(false);

        // when & then
        BadCredentialsException exception = assertThrows(
                BadCredentialsException.class,
                () -> authenticationService.refreshToken(invalidRefreshToken)
        );

        assertEquals("Invalid refresh token", exception.getMessage());
        verify(jwtTokenProvider).validateToken(invalidRefreshToken);
        verify(jwtTokenProvider, never()).getUsernameFromToken(anyString());
        verify(userRepository, never()).findByEmailJPQL(anyString());
    }

    @DisplayName("refreshToken - User not found - Throws BadCredentialsException")
    @Test
    void refreshToken_UserNotFound_ThrowsBadCredentialsException() {
        // given
        String refreshToken = "validRefreshToken";
        when(jwtTokenProvider.validateToken(refreshToken)).thenReturn(true);
        when(jwtTokenProvider.getUsernameFromToken(refreshToken)).thenReturn(user.getEmail());
        when(jwtTokenProvider.getRoleFromToken(refreshToken)).thenReturn(Role.ROLE_USER.name());
        when(userRepository.findByEmailJPQL(user.getEmail())).thenReturn(Optional.empty());

        // when & then
        BadCredentialsException exception = assertThrows(
                BadCredentialsException.class,
                () -> authenticationService.refreshToken(refreshToken)
        );

        assertEquals("User not found", exception.getMessage());
        verify(jwtTokenProvider).validateToken(refreshToken);
        verify(jwtTokenProvider).getUsernameFromToken(refreshToken);
        verify(userRepository).findByEmailJPQL(user.getEmail());
        verify(jwtTokenProvider, never()).generateAccessToken(anyString(), any(Role.class));
    }

    @DisplayName("refreshToken - Role mismatch - Throws BadCredentialsException")
    @Test
    void refreshToken_RoleMismatch_ThrowsBadCredentialsException() {
        // given
        String refreshToken = "validRefreshToken";
        user.setRole(Role.ROLE_ADMIN); // User has ADMIN role in DB
        when(jwtTokenProvider.validateToken(refreshToken)).thenReturn(true);
        when(jwtTokenProvider.getUsernameFromToken(refreshToken)).thenReturn(user.getEmail());
        when(jwtTokenProvider.getRoleFromToken(refreshToken)).thenReturn(Role.ROLE_USER.name()); // Token has USER role
        when(userRepository.findByEmailJPQL(user.getEmail())).thenReturn(Optional.of(user));

        // when & then
        BadCredentialsException exception = assertThrows(
                BadCredentialsException.class,
                () -> authenticationService.refreshToken(refreshToken)
        );

        assertEquals("Role mismatch: token role does not match user role", exception.getMessage());
        verify(jwtTokenProvider).validateToken(refreshToken);
        verify(jwtTokenProvider).getUsernameFromToken(refreshToken);
        verify(jwtTokenProvider).getRoleFromToken(refreshToken);
        verify(userRepository).findByEmailJPQL(user.getEmail());
        verify(jwtTokenProvider, never()).generateAccessToken(anyString(), any(Role.class));
    }

    @DisplayName("refreshToken - Account disabled - Throws BadCredentialsException")
    @Test
    void refreshToken_AccountDisabled_ThrowsBadCredentialsException() {
        // given
        String refreshToken = "validRefreshToken";
        user.setIsActive(false); // Account is disabled
        when(jwtTokenProvider.validateToken(refreshToken)).thenReturn(true);
        when(jwtTokenProvider.getUsernameFromToken(refreshToken)).thenReturn(user.getEmail());
        when(jwtTokenProvider.getRoleFromToken(refreshToken)).thenReturn(Role.ROLE_USER.name());
        when(userRepository.findByEmailJPQL(user.getEmail())).thenReturn(Optional.of(user));

        // when & then
        BadCredentialsException exception = assertThrows(
                BadCredentialsException.class,
                () -> authenticationService.refreshToken(refreshToken)
        );

        assertEquals("Account is disabled", exception.getMessage());
        verify(jwtTokenProvider).validateToken(refreshToken);
        verify(jwtTokenProvider).getUsernameFromToken(refreshToken);
        verify(jwtTokenProvider).getRoleFromToken(refreshToken);
        verify(userRepository).findByEmailJPQL(user.getEmail());
        verify(jwtTokenProvider, never()).generateAccessToken(anyString(), any(Role.class));
    }

    // ================= validateToken Tests =================

    @DisplayName("validateToken - Valid token - Returns valid response")
    @Test
    void validateToken_ValidToken_ReturnsValidResponse() {
        // given
        String token = "validToken";
        when(jwtTokenProvider.validateToken(token)).thenReturn(true);
        when(jwtTokenProvider.getUsernameFromToken(token)).thenReturn(user.getEmail());
        when(jwtTokenProvider.getRoleFromToken(token)).thenReturn(Role.ROLE_USER.name());

        // when
        TokenValidationResponse result = authenticationService.validateToken(token);

        // then
        assertNotNull(result);
        assertTrue(result.isValid());
        assertEquals(user.getEmail(), result.getUsername());
        assertEquals(Role.ROLE_USER.name(), result.getRole());

        verify(jwtTokenProvider).validateToken(token);
        verify(jwtTokenProvider).getUsernameFromToken(token);
        verify(jwtTokenProvider).getRoleFromToken(token);
    }

    @DisplayName("validateToken - Invalid token - Returns invalid response")
    @Test
    void validateToken_InvalidToken_ReturnsInvalidResponse() {
        // given
        String token = "invalidToken";
        when(jwtTokenProvider.validateToken(token)).thenReturn(false);

        // when
        TokenValidationResponse result = authenticationService.validateToken(token);

        // then
        assertNotNull(result);
        assertFalse(result.isValid());
        assertNull(result.getUsername());
        assertNull(result.getRole());

        verify(jwtTokenProvider).validateToken(token);
        verify(jwtTokenProvider, never()).getUsernameFromToken(anyString());
        verify(jwtTokenProvider, never()).getRoleFromToken(anyString());
    }

    @DisplayName("validateToken - Exception during validation - Returns invalid response")
    @Test
    void validateToken_ExceptionDuringValidation_ReturnsInvalidResponse() {
        // given
        String token = "tokenCausingException";
        when(jwtTokenProvider.validateToken(token)).thenThrow(new RuntimeException("Token parsing error"));

        // when
        TokenValidationResponse result = authenticationService.validateToken(token);

        // then
        assertNotNull(result);
        assertFalse(result.isValid());
        assertNull(result.getUsername());
        assertNull(result.getRole());

        verify(jwtTokenProvider).validateToken(token);
        verify(jwtTokenProvider, never()).getUsernameFromToken(anyString());
        verify(jwtTokenProvider, never()).getRoleFromToken(anyString());
    }

    // ================= deleteUserByEmail Tests =================

    @DisplayName("deleteUserByEmail - User exists - Deletes successfully")
    @Test
    void deleteUserByEmail_UserExists_DeletesSuccessfully() {
        // given
        String email = "test@example.com";
        when(userRepository.findByEmailJPQL(email)).thenReturn(Optional.of(user));

        // when
        authenticationService.deleteUserByEmail(email);

        // then
        verify(userRepository).findByEmailJPQL(email);
        verify(userRepository).delete(user);
    }

    @DisplayName("deleteUserByEmail - User not found - Throws UserNotFoundException")
    @Test
    void deleteUserByEmail_UserNotFound_ThrowsUserNotFoundException() {
        // given
        String email = "nonexistent@example.com";
        when(userRepository.findByEmailJPQL(email)).thenReturn(Optional.empty());

        // when & then
        UserNotFoundException exception = assertThrows(
                UserNotFoundException.class,
                () -> authenticationService.deleteUserByEmail(email)
        );

        assertTrue(exception.getMessage().contains(email));
        verify(userRepository).findByEmailJPQL(email);
        verify(userRepository, never()).delete(any(User.class));
    }

    // ================= createAdmin Tests =================

    @DisplayName("createAdmin - Valid data - Creates admin successfully")
    @Test
    void createAdmin_ValidData_CreatesAdminSuccessfully() {
        // given
        String email = "admin@example.com";
        String password = "adminPassword";
        String firstName = "Admin";
        String lastName = "Admin";
        
        User admin = new User();
        admin.setId(2L);
        admin.setEmail(email);
        admin.setPassword("encodedAdminPassword");
        admin.setRole(Role.ROLE_ADMIN);
        admin.setFirstName(firstName);
        admin.setLastName(lastName);
        admin.setCreatedAt(LocalDateTime.now());
        admin.setIsActive(true);

        when(userRepository.findByEmailJPQL(email)).thenReturn(Optional.empty());
        when(passwordEncoder.encode(password)).thenReturn("encodedAdminPassword");
        when(userRepository.save(any(User.class))).thenReturn(admin);

        // when
        User result = authenticationService.createAdmin(email, password, firstName, lastName);

        // then
        assertNotNull(result);
        assertEquals(email, result.getEmail());
        assertEquals(Role.ROLE_ADMIN, result.getRole());
        assertEquals(firstName, result.getFirstName());
        assertEquals(lastName, result.getLastName());
        assertTrue(result.getIsActive());

        verify(userRepository).findByEmailJPQL(email);
        verify(passwordEncoder).encode(password);
        verify(userRepository).save(any(User.class));
    }

    @DisplayName("createAdmin - User already exists - Throws UserAlreadyExistsException")
    @Test
    void createAdmin_UserAlreadyExists_ThrowsUserAlreadyExistsException() {
        // given
        String email = "admin@example.com";
        String password = "adminPassword";
        String firstName = "Admin";
        String lastName = "Admin";

        when(userRepository.findByEmailJPQL(email)).thenReturn(Optional.of(user));

        // when & then
        UserAlreadyExistsException exception = assertThrows(
                UserAlreadyExistsException.class,
                () -> authenticationService.createAdmin(email, password, firstName, lastName)
        );

        assertTrue(exception.getMessage().contains(email));
        verify(userRepository).findByEmailJPQL(email);
        verify(passwordEncoder, never()).encode(anyString());
        verify(userRepository, never()).save(any(User.class));
    }
}