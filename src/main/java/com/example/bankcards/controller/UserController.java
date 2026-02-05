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
@Tag(name = "Users", description = "User management")
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
        summary = "Get current user data",
        description = "Returns information about the user extracted from JWT token"
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "User data retrieved successfully",
            content = @Content(schema = @Schema(implementation = UserDto.class))
        ),
        @ApiResponse(
            responseCode = "401",
            description = "Unauthorized"
        ),
        @ApiResponse(
            responseCode = "404",
            description = "User not found"
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
        summary = "Create new user",
        description = "Create new user in the system. Available only to administrators."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "User successfully created",
            content = @Content(schema = @Schema(implementation = UserDto.class))
        ),
        @ApiResponse(
            responseCode = "400",
            description = "Invalid user data"
        ),
        @ApiResponse(
            responseCode = "409",
            description = "User with this email already exists"
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
        summary = "Get user by ID",
        description = "Returns user information by ID. Users can only get their own information."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "User found",
            content = @Content(schema = @Schema(implementation = UserDto.class))
        ),
        @ApiResponse(
            responseCode = "403",
            description = "No access to this user"
        ),
        @ApiResponse(
            responseCode = "404",
            description = "User not found"
        )
    })
    @GetMapping("/id")
    public ResponseEntity<UserDto> getUserById(
            @Parameter(description = "User ID") @RequestParam Long id,
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
        summary = "Get list of all users",
        description = "Returns paginated list of all system users. Available only to administrators."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "User list retrieved successfully",
            content = @Content(schema = @Schema(implementation = PagedUserResponse.class))
        ),
        @ApiResponse(
            responseCode = "403",
            description = "Access denied - administrator rights required"
        )
    })
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping
    public ResponseEntity<PagedUserResponse> getUsers(
            @Parameter(description = "Page number") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size") @RequestParam(defaultValue = "5") int size) {
        return ResponseEntity.ok(userService.findAllUsers(page, size));
    }

    /**
     * Get user by email.
     * ADMIN: can get any user.
     * USER: can get only their own information.
     */
    @Operation(
        summary = "Get user by email",
        description = "Returns user information by email. Users can only get their own information."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "User found",
            content = @Content(schema = @Schema(implementation = UserDto.class))
        ),
        @ApiResponse(
            responseCode = "403",
            description = "No access to this user"
        ),
        @ApiResponse(
            responseCode = "404",
            description = "User not found"
        )
    })
    @GetMapping("/email")
    public ResponseEntity<UserDto> getUserByEmail(
            @Parameter(description = "User email") @RequestParam String email,
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
        summary = "Update current user profile",
        description = "Updates current user profile. Email is extracted from JWT token. Performs partial update - only provided fields are updated."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Profile successfully updated",
            content = @Content(schema = @Schema(implementation = UserDto.class))
        ),
        @ApiResponse(
            responseCode = "400",
            description = "Invalid user data"
        ),
        @ApiResponse(
            responseCode = "401",
            description = "Unauthorized"
        ),
        @ApiResponse(
            responseCode = "404",
            description = "User not found"
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
        summary = "Update user by ID",
        description = "Updates user data by ID. Performs partial update - only provided fields are updated. Available only to administrators."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "User successfully updated",
            content = @Content(schema = @Schema(implementation = UserDto.class))
        ),
        @ApiResponse(
            responseCode = "400",
            description = "Invalid user data"
        ),
        @ApiResponse(
            responseCode = "403",
            description = "Access denied - administrator rights required"
        ),
        @ApiResponse(
            responseCode = "404",
            description = "User not found"
        )
    })
    @PutMapping("/{id}")
    public ResponseEntity<UserDto> updateUser(
            @Parameter(description = "User ID") @PathVariable Long id,
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
        summary = "Delete user",
        description = "Deletes user from the system. Also deletes all related cards and transfers. Available only to administrators."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "User successfully deleted"
        ),
        @ApiResponse(
            responseCode = "403",
            description = "Access denied - administrator rights required"
        ),
        @ApiResponse(
            responseCode = "404",
            description = "User not found"
        ),
        @ApiResponse(
            responseCode = "409",
            description = "Cannot delete user - related data exists"
        )
    })
    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteUser(
            @Parameter(description = "User ID") @PathVariable Long id,
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
        summary = "Get all users (for administrator)",
        description = "Returns paginated list of all system users with detailed information. Available only to administrators."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "User list retrieved successfully",
            content = @Content(schema = @Schema(implementation = PagedUserResponse.class))
        ),
        @ApiResponse(
            responseCode = "403",
            description = "Access denied - administrator rights required"
        )
    })
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/admin/all")
    public ResponseEntity<PagedUserResponse> getAllUsersForAdmin(
            @Parameter(description = "Page number") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size") @RequestParam(defaultValue = "10") int size,
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
        summary = "Search users by email",
        description = "Performs user search by email pattern. Supports partial matching. Available only to administrators."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Search completed successfully"
        ),
        @ApiResponse(
            responseCode = "403",
            description = "Access denied - administrator rights required"
        )
    })
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/admin/search")
    public ResponseEntity<java.util.List<UserDto>> searchUsersByEmail(
            @Parameter(description = "Email pattern for search") @RequestParam String emailPattern,
            Authentication authentication) {
        
        String adminEmail = SecurityUtils.getEmailFromToken(authentication);
        log.info("Admin {} searching users by email pattern: {}", adminEmail, emailPattern);

        java.util.List<UserDto> users = userService.searchUsersByEmail(emailPattern);
        
        log.info("Admin {} found {} users matching pattern: {}", adminEmail, users.size(), emailPattern);
        return ResponseEntity.ok(users);
    }
}
