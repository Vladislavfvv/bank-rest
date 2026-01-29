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
import org.springframework.security.core.Authentication;
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
            // Используем AuthenticationManager для аутентификации
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            loginRequest.getEmail(),
                            loginRequest.getPassword()
                    )
            );

            // Получаем пользователя из БД для генерации токенов
            User user = userRepository.findByEmailJPQL(loginRequest.getEmail())
                    .orElseThrow(() -> new BadCredentialsException("User not found"));

            // Генерация JWT токенов
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
     * Регистрирует нового пользователя
     */
    @Transactional
    public TokenResponse register(RegisterRequest registerRequest) {
        // Проверяем, не существует ли уже пользователь с таким email
        if (userRepository.findByEmailJPQL(registerRequest.getEmail()).isPresent()) {
            log.warn("Registration attempt for existing email: {}", registerRequest.getEmail());
            throw new UserAlreadyExistsException("User with email " + registerRequest.getEmail() + " already exists");
        }

        // Создаем нового пользователя с ролью USER по умолчанию
        User user = new User();
        user.setEmail(registerRequest.getEmail());
        user.setPassword(passwordEncoder.encode(registerRequest.getPassword()));
        user.setRole(Role.ROLE_USER); // По умолчанию все новые пользователи - USER
        user.setFirstName(registerRequest.getFirstName());
        user.setLastName(registerRequest.getLastName());
        user.setBirthDate(registerRequest.getBirthDate());
        user.setPhoneNumber(registerRequest.getPhoneNumber());
        user.setCreatedAt(LocalDateTime.now());
        user.setIsActive(true);

        User savedUser = userRepository.save(user);
        log.info("User registered successfully: {}", savedUser.getEmail());

        // Генерация JWT токенов
        String accessToken = jwtTokenProvider.generateAccessToken(user.getEmail(), user.getRole());
        String refreshToken = jwtTokenProvider.generateRefreshToken(user.getEmail(), user.getRole());
        
        return new TokenResponse(accessToken, refreshToken, jwtTokenProvider.getJwtExpiration() / 1000);
    }

    /**
     * Обновляет access токен с помощью refresh токена
     */
    public TokenResponse refreshToken(String refreshToken) {
        if (!jwtTokenProvider.validateToken(refreshToken)) {
            throw new BadCredentialsException("Invalid refresh token");
        }

        String username = jwtTokenProvider.getUsernameFromToken(refreshToken);
        String roleStr = jwtTokenProvider.getRoleFromToken(refreshToken);

        // Валидация роли из токена
        Role roleFromToken;
        try {
            roleFromToken = Role.valueOf(roleStr);
        } catch (IllegalArgumentException e) {
            throw new BadCredentialsException("Invalid role in refresh token: " + roleStr);
        }

        User user = userRepository.findByEmailJPQL(username)
                .orElseThrow(() -> new BadCredentialsException("User not found"));

        // Проверяем, что роль в токене совпадает с ролью в БД (дополнительная проверка безопасности)
        if (user.getRole() != roleFromToken) {
            throw new BadCredentialsException("Role mismatch: token role does not match user role");
        }

        // Проверяем, что аккаунт активен
        if (!user.isAccountActive()) {
            throw new BadCredentialsException("Account is disabled");
        }

        // Используем роль из БД для генерации новых токенов
        String newAccessToken = jwtTokenProvider.generateAccessToken(user.getEmail(), user.getRole());
        String newRefreshToken = jwtTokenProvider.generateRefreshToken(user.getEmail(), user.getRole());

        log.info("Tokens refreshed for user: {}", user.getEmail());
        return new TokenResponse(newAccessToken, newRefreshToken, jwtTokenProvider.getJwtExpiration() / 1000);
    }

    /**
     * Валидирует токен
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
     * Удаляет пользователя по email
     */
    @Transactional
    public void deleteUserByEmail(String email) {
        User user = userRepository.findByEmailJPQL(email)
                .orElseThrow(() -> new UserNotFoundException("User with email " + email + " not found"));

        userRepository.delete(user);
        log.info("User deleted: {}", email);
    }

    /**
     * Создает администратора (только для внутреннего использования)
     */
    @Transactional
    public User createAdmin(String email, String password, String firstName, String lastName) {
        // Проверяем, не существует ли уже пользователь с таким email
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
