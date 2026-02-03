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
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthenticationService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final AuthenticationManager authenticationManager;

    @Transactional(readOnly = true)
    public TokenResponse login(LoginRequest loginRequest) {
        try {
            // Use AuthenticationManager for authentication (validates credentials)
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            loginRequest.getEmail(),
                            loginRequest.getPassword()
                    )
            );

            // Get user from database for token generation
            User user = userRepository.findByEmailJPQL(loginRequest.getEmail())
                    .orElseThrow(() -> new BadCredentialsException("User not found"));

            // Generate JWT tokens
            String accessToken = jwtTokenProvider.generateAccessToken(user.getEmail(), user.getRole());
            String refreshToken = jwtTokenProvider.generateRefreshToken(user.getEmail(), user.getRole());

            log.info("User logged in successfully: {}", user.getEmail());
            return new TokenResponse(accessToken, refreshToken, jwtTokenProvider.getJwtExpiration() / 1000);

        } catch (AuthenticationException e) {
            log.warn("Authentication failed for user: {}", loginRequest.getEmail());
            throw new BadCredentialsException("Invalid email or password");
        }
    }

    /**
     * Registers a new user
     */
    @Transactional
    public TokenResponse register(RegisterRequest registerRequest) {
        // Check if user with this email already exists
        if (userRepository.findByEmailJPQL(registerRequest.getEmail()).isPresent()) {
            log.warn("Registration attempt for existing email: {}", registerRequest.getEmail());
            throw new UserAlreadyExistsException("User with email " + registerRequest.getEmail() + " already exists");
        }

        // Create new user with default USER role
        User user = new User();
        user.setEmail(registerRequest.getEmail());
        user.setPassword(passwordEncoder.encode(registerRequest.getPassword()));
        user.setRole(Role.ROLE_USER); // By default
        user.setFirstName(registerRequest.getFirstName());
        user.setLastName(registerRequest.getLastName());
        user.setBirthDate(registerRequest.getBirthDate());
        user.setPhoneNumber(registerRequest.getPhoneNumber());
        user.setCreatedAt(LocalDateTime.now());
        user.setIsActive(true);

        User savedUser = userRepository.save(user);
        log.info("User registered successfully: {}", savedUser.getEmail());

        // Generate JWT tokens
        String accessToken = jwtTokenProvider.generateAccessToken(user.getEmail(), user.getRole());
        String refreshToken = jwtTokenProvider.generateRefreshToken(user.getEmail(), user.getRole());
        
        return new TokenResponse(accessToken, refreshToken, jwtTokenProvider.getJwtExpiration() / 1000);
    }

    /**
     * Updates access token using refresh token
     */
    public TokenResponse refreshToken(String refreshToken) {
        if (!jwtTokenProvider.validateToken(refreshToken)) {
            throw new BadCredentialsException("Invalid refresh token");
        }

        String username = jwtTokenProvider.getUsernameFromToken(refreshToken);
        String roleStr = jwtTokenProvider.getRoleFromToken(refreshToken);

        // Validate role from token
        Role roleFromToken;
        try {
            roleFromToken = Role.valueOf(roleStr);
        } catch (IllegalArgumentException e) {
            throw new BadCredentialsException("Invalid role in refresh token: " + roleStr);
        }

        User user = userRepository.findByEmailJPQL(username)
                .orElseThrow(() -> new BadCredentialsException("User not found"));

        // Check that role in token matches role in database (additional security check)
        if (user.getRole() != roleFromToken) {
            throw new BadCredentialsException("Role mismatch: token role does not match user role");
        }

        // Check that account is active
        if (user.isAccountInactive()) {
            throw new BadCredentialsException("Account is disabled");
        }

        // Use role from database for generating new tokens
        String newAccessToken = jwtTokenProvider.generateAccessToken(user.getEmail(), user.getRole());
        String newRefreshToken = jwtTokenProvider.generateRefreshToken(user.getEmail(), user.getRole());

        log.info("Tokens refreshed for user: {}", user.getEmail());
        return new TokenResponse(newAccessToken, newRefreshToken, jwtTokenProvider.getJwtExpiration() / 1000);
    }

    /**
     * Validates token
     */
    public TokenValidationResponse validateToken(String token) {
        try {
            if (jwtTokenProvider.validateToken(token)) {
                String username = jwtTokenProvider.getUsernameFromToken(token);
                String role = jwtTokenProvider.getRoleFromToken(token);
                return new TokenValidationResponse(true, username, role);
            }
            return new TokenValidationResponse(false, null, null);
        } catch (Exception e) {
            log.warn("Token validation failed: {}", e.getMessage());
            return new TokenValidationResponse(false, null, null);
        }
    }

    /**
     * Deletes user by email
     */
    @Transactional
    public void deleteUserByEmail(String email) {
        User user = userRepository.findByEmailJPQL(email)
                .orElseThrow(() -> new UserNotFoundException("User with email " + email + " not found"));

        userRepository.delete(user);
        log.info("User deleted: {}", email);
    }

    /**
     * Creates administrator (for internal use only)
     */
    @Transactional
    public User createAdmin(String email, String password, String firstName, String lastName) {
        // Check if user with this email already exists
        if (userRepository.findByEmailJPQL(email).isPresent()) {
            throw new UserAlreadyExistsException("User with email " + email + " already exists");
        }

        User admin = new User();
        admin.setEmail(email);
        admin.setPassword(passwordEncoder.encode(password));
        admin.setRole(Role.ROLE_ADMIN);
        admin.setFirstName(firstName);
        admin.setLastName(lastName);
        admin.setCreatedAt(LocalDateTime.now());
        admin.setIsActive(true);

        User savedAdmin = userRepository.save(admin);
        log.info("Admin created successfully: {}", savedAdmin.getEmail());
        
        return savedAdmin;
    }
}
