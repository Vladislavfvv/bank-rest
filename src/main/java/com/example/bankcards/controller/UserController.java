package com.example.bankcards.controller;

import com.example.bankcards.dto.user.PagedUserResponse;
import com.example.bankcards.dto.user.UpdateUserDto;
import com.example.bankcards.dto.user.UserDto;
import com.example.bankcards.service.UserService;
import com.example.bankcards.util.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
@SuppressWarnings({"all", "SimilarLogMessages"})
@Tag(name = "Users", description = "Управление пользователями")
@SecurityRequirement(name = "bearerAuth")
public class UserController {
    private final UserService userService;

    public static final String EXTRACTED_EMAIL_FROM_TOKEN = "Extracted email from token: {}";
    /**
     * Get own data from JWT token.
     * Email is extracted from token (claim "sub"), user gets their own data.
     *
     * @param authentication authentication object containing JWT token
     * @return current user data
     */
    @Operation(
        summary = "Получить данные текущего пользователя",
        description = "Возвращает информацию о пользователе, извлеченную из JWT токена"
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Данные пользователя получены успешно",
            content = @Content(schema = @Schema(implementation = UserDto.class))
        ),
        @ApiResponse(
            responseCode = "401",
            description = "Не авторизован"
        ),
        @ApiResponse(
            responseCode = "404",
            description = "Пользователь не найден"
        )
    })
    @GetMapping("/self")
    public ResponseEntity<UserDto> getSelfUser(Authentication authentication) {
        log.info("Getting user data from token");

        // Extract email from JWT token
        String email = SecurityUtils.getEmailFromToken(authentication);
        log.debug(EXTRACTED_EMAIL_FROM_TOKEN, email);

        // Get user by email
        UserDto userDto = userService.getUserByEmail(email);
        return ResponseEntity.ok(userDto);
    }

    /**
     * Create user from JWT token.
     * Email is extracted from token (claim "sub"), other data from request body.
     * User must be registered in auth-service and have valid JWT token.
     *
     * @param request        user data (firstName, lastName, birthDate)
     * @param authentication authentication object containing JWT token
     * @return created user
     */
//    @PostMapping("/createUser")
//    public ResponseEntity<UserDto> createUserFromToken(
//            @Valid @RequestBody CreateUserFromTokenRequest request,
//            Authentication authentication) {
//        log.info("Creating user from token for authenticated user");
//
//        // Extract email from JWT token
//        String email = SecurityUtils.getEmailFromToken(authentication);
//        log.debug(EXTRACTED_EMAIL_FROM_TOKEN, email);
//
//        // Create user with email from token
//        UserDto userDto = userService.createUserFromToken(email, request);
//        return ResponseEntity.ok(userDto);
//    }

    @Operation(
        summary = "Создать нового пользователя",
        description = "Создание нового пользователя в системе. Доступно только администраторам."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Пользователь успешно создан",
            content = @Content(schema = @Schema(implementation = UserDto.class))
        ),
        @ApiResponse(
            responseCode = "400",
            description = "Некорректные данные пользователя"
        ),
        @ApiResponse(
            responseCode = "409",
            description = "Пользователь с таким email уже существует"
        )
    })
    @PostMapping
    public ResponseEntity<UserDto> createUser(@Valid @RequestBody UserDto dto) {
        return ResponseEntity.ok(userService.createUser(dto));
    }

    /**
     * Get user by ID.
     * ADMIN: can get any user.
     * USER: can get only their own information.
     */
    @Operation(
        summary = "Получить пользователя по ID",
        description = "Возвращает информацию о пользователе по его ID. Пользователи могут получать только свою информацию."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Пользователь найден",
            content = @Content(schema = @Schema(implementation = UserDto.class))
        ),
        @ApiResponse(
            responseCode = "403",
            description = "Нет доступа к данному пользователю"
        ),
        @ApiResponse(
            responseCode = "404",
            description = "Пользователь не найден"
        )
    })
    @GetMapping("/id")
    public ResponseEntity<UserDto> getUserById(
            @Parameter(description = "ID пользователя") @RequestParam Long id,
            Authentication authentication) {
        UserDto user = userService.findUserById(id);

        // Access check: USER can get only their own information
        if (!SecurityUtils.hasAccess(authentication, user.getEmail())) {
            throw new AccessDeniedException("Access denied: You can only access your own information");
        }

        return ResponseEntity.ok(user);
    }

    /**
     * Get list of all users.
     * Available only for ADMIN.
     */
    @Operation(
        summary = "Получить список всех пользователей",
        description = "Возвращает постраничный список всех пользователей системы. Доступно только администраторам."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Список пользователей получен успешно",
            content = @Content(schema = @Schema(implementation = PagedUserResponse.class))
        ),
        @ApiResponse(
            responseCode = "403",
            description = "Доступ запрещен - требуются права администратора"
        )
    })
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping
    public ResponseEntity<PagedUserResponse> getUsers(
            @Parameter(description = "Номер страницы") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Размер страницы") @RequestParam(defaultValue = "5") int size) {
        return ResponseEntity.ok(userService.findAllUsers(page, size));
    }

    /**
     * Get user by email.
     * ADMIN: can get any user.
     * USER: can get only their own information.
     */
    @Operation(
        summary = "Получить пользователя по email",
        description = "Возвращает информацию о пользователе по его email. Пользователи могут получать только свою информацию."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Пользователь найден",
            content = @Content(schema = @Schema(implementation = UserDto.class))
        ),
        @ApiResponse(
            responseCode = "403",
            description = "Нет доступа к данному пользователю"
        ),
        @ApiResponse(
            responseCode = "404",
            description = "Пользователь не найден"
        )
    })
    @GetMapping("/email")
    public ResponseEntity<UserDto> getUserByEmail(
            @Parameter(description = "Email пользователя") @RequestParam String email,
            Authentication authentication) {
        log.info("Getting user by email: {} (requested by: {})",
                email, authentication != null ? SecurityUtils.getEmailFromToken(authentication) : "unknown");

        // Access check BEFORE getting user from database
        // USER can only request their own email
        if (!SecurityUtils.isAdmin(authentication)) {
            String userEmail = SecurityUtils.getEmailFromToken(authentication);
            log.debug("User is not ADMIN. Checking access: token email={}, requested email={}", userEmail, email);
            if (!userEmail.equals(email)) {
                log.warn("Access denied: User {} tried to access email {}", userEmail, email);
                throw new AccessDeniedException("Access denied: You can only access your own information");
            }
        } else {
            log.debug("User is ADMIN, skipping access check");
        }

        // Get user from database only after access check
        log.debug("Fetching user from database for email: {}", email);
        UserDto user = userService.getUserByEmail(email);
        log.info("User found: id={}, email={}", user.getId(), user.getEmail());

        return ResponseEntity.ok(user);
    }

    /**
     * Update current user (own profile).
     * ID is taken from JWT token (by email).
     * Performs partial update - only provided fields are updated.
     * Email is taken from token, holder for cards is automatically formed from firstName + lastName.
     */
    @Operation(
        summary = "Обновить профиль текущего пользователя",
        description = "Обновляет профиль текущего пользователя. Email извлекается из JWT токена. Выполняется частичное обновление - обновляются только переданные поля."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Профиль успешно обновлен",
            content = @Content(schema = @Schema(implementation = UserDto.class))
        ),
        @ApiResponse(
            responseCode = "400",
            description = "Некорректные данные пользователя"
        ),
        @ApiResponse(
            responseCode = "401",
            description = "Не авторизован"
        ),
        @ApiResponse(
            responseCode = "404",
            description = "Пользователь не найден"
        )
    })
    @PutMapping("/me")
    public ResponseEntity<UserDto> updateCurrentUser(
            @RequestBody UpdateUserDto dto,
            Authentication authentication) {
        log.info("Received update request for current user. DTO: firstName={}, lastName={}, birthDate={}",
                dto.getFirstName(), dto.getLastName(), dto.getBirthDate());

        // Extract email from token
        String userEmail;
        try {
            userEmail = SecurityUtils.getEmailFromToken(authentication);
            log.debug(EXTRACTED_EMAIL_FROM_TOKEN, userEmail);
        } catch (IllegalStateException e) {
            log.error("Failed to extract email from token: {}", e.getMessage());
            throw new AccessDeniedException("Access denied: Authentication required.");
        }

        // Find user by email and update them
        UserDto updated = userService.updateCurrentUser(userEmail, dto);
        log.info("User successfully updated: id={}, email={}", updated.getId(), updated.getEmail());
        return ResponseEntity.ok(updated);
    }

    /**
     * Update user by ID (admin only).
     * ADMIN can update any user.
     * Performs partial update - only provided fields are updated.
     * Holder for cards is automatically formed from name + surname.
     */
    @Operation(
        summary = "Обновить пользователя по ID",
        description = "Обновляет данные пользователя по его ID. Выполняется частичное обновление - обновляются только переданные поля. Доступно только администраторам."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Пользователь успешно обновлен",
            content = @Content(schema = @Schema(implementation = UserDto.class))
        ),
        @ApiResponse(
            responseCode = "400",
            description = "Некорректные данные пользователя"
        ),
        @ApiResponse(
            responseCode = "403",
            description = "Доступ запрещен - требуются права администратора"
        ),
        @ApiResponse(
            responseCode = "404",
            description = "Пользователь не найден"
        )
    })
    @PutMapping("/{id}")
    public ResponseEntity<UserDto> updateUser(
            @Parameter(description = "ID пользователя") @PathVariable Long id,
            @RequestBody UpdateUserDto dto,
            Authentication authentication) {
        // Access check: only ADMIN can update users by ID
        if (!SecurityUtils.isAdmin(authentication)) {
            throw new AccessDeniedException("Access denied: Only administrators can update users by ID. " +
                    "Use PUT /api/v1/users/me to update your own profile.");
        }

        // Extract admin email for logging
        String adminEmail;
        try {
            adminEmail = SecurityUtils.getEmailFromToken(authentication);
        } catch (IllegalStateException e) {
            throw new AccessDeniedException("Access denied: Authentication required.");
        }

        // Admin can update any user (no access check required)
        return ResponseEntity.ok(userService.updateUserByAdmin(id, dto, adminEmail));
    }

    /**
     * Delete user.
     * Only ADMIN can delete users.
     * USER cannot delete even their own data.
     */
    @Operation(
        summary = "Удалить пользователя",
        description = "Удаляет пользователя из системы. Также удаляются все связанные с ним карты и переводы. Доступно только администраторам."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Пользователь успешно удален"
        ),
        @ApiResponse(
            responseCode = "403",
            description = "Доступ запрещен - требуются права администратора"
        ),
        @ApiResponse(
            responseCode = "404",
            description = "Пользователь не найден"
        ),
        @ApiResponse(
            responseCode = "409",
            description = "Невозможно удалить пользователя - есть связанные данные"
        )
    })
    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteUser(
            @Parameter(description = "ID пользователя") @PathVariable Long id,
            Authentication authentication) {
        log.info("Delete user request received for user ID: {} by user: {}", id,
                authentication != null ? SecurityUtils.getEmailFromToken(authentication) : "unknown");

        // Access check: only ADMIN can delete users
        if (!SecurityUtils.isAdmin(authentication)) {
            log.warn("Access denied: User {} attempted to delete user ID: {}",
                    SecurityUtils.getEmailFromToken(authentication), id);
            throw new AccessDeniedException("Access denied: Only administrators can delete users");
        }

        log.info("Admin user {} is deleting user ID: {}", SecurityUtils.getEmailFromToken(authentication), id);

        // Check that user exists and get email for synchronization
        UserDto userToDelete = userService.findUserById(id);
        log.info("User to delete found: {} (email: {})", id, userToDelete.getEmail());

        userService.deleteUser(id);
        log.info("User ID: {} successfully deleted from user-service", id);

        return ResponseEntity.noContent().build();
    }

    // ========== ADMIN METHODS ==========

    /**
     * Get all users (admin only)
     */
    @Operation(
        summary = "Получить всех пользователей (для администратора)",
        description = "Возвращает постраничный список всех пользователей системы с подробной информацией. Доступно только администраторам."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Список пользователей получен успешно",
            content = @Content(schema = @Schema(implementation = PagedUserResponse.class))
        ),
        @ApiResponse(
            responseCode = "403",
            description = "Доступ запрещен - требуются права администратора"
        )
    })
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/admin/all")
    public ResponseEntity<PagedUserResponse> getAllUsersForAdmin(
            @Parameter(description = "Номер страницы") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Размер страницы") @RequestParam(defaultValue = "10") int size,
            Authentication authentication) {
        
        String adminEmail = SecurityUtils.getEmailFromToken(authentication);
        log.info("Admin {} requesting all users, page: {}, size: {}", adminEmail, page, size);

        PagedUserResponse users = userService.getAllUsersForAdmin(page, size);
        
        log.info("Admin {} retrieved {} users", adminEmail, users.getTotalElements());
        return ResponseEntity.ok(users);
    }

    /**
     * Search users by email (admin only)
     */
    @Operation(
        summary = "Поиск пользователей по email",
        description = "Выполняет поиск пользователей по шаблону email. Поддерживает частичное совпадение. Доступно только администраторам."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Поиск выполнен успешно"
        ),
        @ApiResponse(
            responseCode = "403",
            description = "Доступ запрещен - требуются права администратора"
        )
    })
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/admin/search")
    public ResponseEntity<java.util.List<UserDto>> searchUsersByEmail(
            @Parameter(description = "Шаблон email для поиска") @RequestParam String emailPattern,
            Authentication authentication) {
        
        String adminEmail = SecurityUtils.getEmailFromToken(authentication);
        log.info("Admin {} searching users by email pattern: {}", adminEmail, emailPattern);

        java.util.List<UserDto> users = userService.searchUsersByEmail(emailPattern);
        
        log.info("Admin {} found {} users matching pattern: {}", adminEmail, users.size(), emailPattern);
        return ResponseEntity.ok(users);
    }
}
