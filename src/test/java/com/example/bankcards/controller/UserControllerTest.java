package com.example.bankcards.controller;

import com.example.bankcards.dto.user.CreateUserFromTokenRequest;
import com.example.bankcards.dto.user.PagedUserResponse;
import com.example.bankcards.dto.user.UpdateUserDto;
import com.example.bankcards.dto.user.UserDto;
import com.example.bankcards.entity.Role;
import com.example.bankcards.service.UserService;
import com.example.bankcards.util.SecurityUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for UserController.
 * Tests user management endpoints without Spring context.
 */
@ExtendWith(MockitoExtension.class)
class UserControllerTest {

    @Mock
    private UserService userService;

    @Mock
    private Authentication authentication;

    @InjectMocks
    private UserController userController;

    private UserDto userDto;
    private CreateUserFromTokenRequest createUserFromTokenRequest;
    private UpdateUserDto updateUserDto;
    private PagedUserResponse pagedUserResponse;

    @BeforeEach
    void setUp() {
        // Setup UserDto
        userDto = new UserDto();
        userDto.setId(1L);
        userDto.setFirstName("Ivan");
        userDto.setLastName("Ivanov");
        userDto.setBirthDate(LocalDate.of(1990, 1, 1));
        userDto.setEmail("ivan.ivanov@example.com");
        userDto.setPhoneNumber("+1234567890");
        userDto.setRole(Role.ROLE_USER);
        userDto.setCreatedAt(LocalDateTime.now());
        userDto.setIsActive(true);
        userDto.setFullName("Ivan Ivanov");
        userDto.setCardCount(2);

        // Setup CreateUserFromTokenRequest
        createUserFromTokenRequest = new CreateUserFromTokenRequest();
        createUserFromTokenRequest.setFirstName("Natasha");
        createUserFromTokenRequest.setLastName("Rostova");
        createUserFromTokenRequest.setBirthDate(LocalDate.of(1985, 5, 15));

        // Setup UpdateUserDto
        updateUserDto = new UpdateUserDto();
        updateUserDto.setFirstName("Updated Ivan");
        updateUserDto.setLastName("Updated Ivanov");
        updateUserDto.setBirthDate(LocalDate.of(1991, 2, 2));

        // Setup PagedUserResponse
        List<UserDto> users = List.of(userDto);
        pagedUserResponse = new PagedUserResponse();
        pagedUserResponse.setContent(users);
        pagedUserResponse.setPage(0);
        pagedUserResponse.setSize(10);
        pagedUserResponse.setTotalElements(1L);
        pagedUserResponse.setTotalPages(1);
    }

    // ================= GET /api/v1/users/self Tests =================

    @Test
    @DisplayName("GET /api/v1/users/self - Success")
    void getSelfUser_Success() {
        // given
        String userEmail = "ivan.ivanov@example.com";
        when(userService.getUserByEmail(userEmail)).thenReturn(userDto);

        try (MockedStatic<SecurityUtils> mockedSecurityUtils = mockStatic(SecurityUtils.class)) {
            mockedSecurityUtils.when(() -> SecurityUtils.getEmailFromToken(authentication))
                    .thenReturn(userEmail);

            // when
            ResponseEntity<UserDto> response = userController.getSelfUser(authentication);

            // then
            assertEquals(HttpStatus.OK, response.getStatusCode());
            assertNotNull(response.getBody());
            assertEquals(userDto.getId(), response.getBody().getId());
            assertEquals(userDto.getEmail(), response.getBody().getEmail());
            assertEquals(userDto.getFirstName(), response.getBody().getFirstName());

            verify(userService).getUserByEmail(userEmail);
            mockedSecurityUtils.verify(() -> SecurityUtils.getEmailFromToken(authentication));
        }
    }

    // ================= POST /api/v1/users/createUser Tests =================
    // NOTE: createUserFromToken method is commented out in UserController, so this test is disabled

    // @Test
    // @DisplayName("POST /api/v1/users/createUser - Success")
    // void createUserFromToken_Success() {
    //     // given
    //     String userEmail = "natasha.rostova@example.com";
    //     UserDto createdUser = new UserDto();
    //     createdUser.setId(2L);
    //     createdUser.setFirstName("Natasha");
    //     createdUser.setLastName("Rostova");
    //     createdUser.setEmail(userEmail);
    //
    //     when(userService.createUserFromToken(userEmail, createUserFromTokenRequest)).thenReturn(createdUser);
    //
    //     try (MockedStatic<SecurityUtils> mockedSecurityUtils = mockStatic(SecurityUtils.class)) {
    //         mockedSecurityUtils.when(() -> SecurityUtils.getEmailFromToken(authentication))
    //                 .thenReturn(userEmail);
    //
    //         // when
    //         ResponseEntity<UserDto> response = userController.createUserFromToken(createUserFromTokenRequest, authentication);
    //
    //         // then
    //         assertEquals(HttpStatus.OK, response.getStatusCode());
    //         assertNotNull(response.getBody());
    //         assertEquals(createdUser.getId(), response.getBody().getId());
    //         assertEquals(createdUser.getEmail(), response.getBody().getEmail());
    //         assertEquals("Natasha", response.getBody().getFirstName());
    //         assertEquals("Rostova", response.getBody().getLastName());
    //
    //         verify(userService).createUserFromToken(userEmail, createUserFromTokenRequest);
    //         mockedSecurityUtils.verify(() -> SecurityUtils.getEmailFromToken(authentication));
    //     }
    // }

    // ================= POST /api/v1/users Tests =================

    @Test
    @DisplayName("POST /api/v1/users - Success")
    void createUser_Success() {
        // given
        when(userService.createUser(userDto)).thenReturn(userDto);

        // when
        ResponseEntity<UserDto> response = userController.createUser(userDto);

        // then
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(userDto.getId(), response.getBody().getId());
        assertEquals(userDto.getEmail(), response.getBody().getEmail());

        verify(userService).createUser(userDto);
    }

    // ================= GET /api/v1/users/id Tests =================

    @Test
    @DisplayName("GET /api/v1/users/id - Success with access")
    void getUserById_SuccessWithAccess() {
        // given
        Long userId = 1L;
        when(userService.findUserById(userId)).thenReturn(userDto);

        try (MockedStatic<SecurityUtils> mockedSecurityUtils = mockStatic(SecurityUtils.class)) {
            mockedSecurityUtils.when(() -> SecurityUtils.hasAccess(authentication, userDto.getEmail()))
                    .thenReturn(true);

            // when
            ResponseEntity<UserDto> response = userController.getUserById(userId, authentication);

            // then
            assertEquals(HttpStatus.OK, response.getStatusCode());
            assertNotNull(response.getBody());
            assertEquals(userDto.getId(), response.getBody().getId());

            verify(userService).findUserById(userId);
            mockedSecurityUtils.verify(() -> SecurityUtils.hasAccess(authentication, userDto.getEmail()));
        }
    }

    @Test
    @DisplayName("GET /api/v1/users/id - Access denied")
    void getUserById_AccessDenied() {
        // given
        Long userId = 1L;
        when(userService.findUserById(userId)).thenReturn(userDto);

        try (MockedStatic<SecurityUtils> mockedSecurityUtils = mockStatic(SecurityUtils.class)) {
            mockedSecurityUtils.when(() -> SecurityUtils.hasAccess(authentication, userDto.getEmail()))
                    .thenReturn(false);

            // when & then
            assertThrows(AccessDeniedException.class, () -> 
                userController.getUserById(userId, authentication));

            verify(userService).findUserById(userId);
            mockedSecurityUtils.verify(() -> SecurityUtils.hasAccess(authentication, userDto.getEmail()));
        }
    }

    // ================= GET /api/v1/users Tests =================

    @Test
    @DisplayName("GET /api/v1/users - Success")
    void getUsers_Success() {
        // given
        when(userService.findAllUsers(0, 5)).thenReturn(pagedUserResponse);

        // when
        ResponseEntity<PagedUserResponse> response = userController.getUsers(0, 5);

        // then
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(1L, response.getBody().getTotalElements());
        assertEquals(1, response.getBody().getContent().size());

        verify(userService).findAllUsers(0, 5);
    }

    // ================= GET /api/v1/users/email Tests =================

    @Test
    @DisplayName("GET /api/v1/users/email - Success for admin")
    void getUserByEmail_SuccessForAdmin() {
        // given
        String requestedEmail = "ivan.ivanov@example.com";
        when(userService.getUserByEmail(requestedEmail)).thenReturn(userDto);

        try (MockedStatic<SecurityUtils> mockedSecurityUtils = mockStatic(SecurityUtils.class)) {
            mockedSecurityUtils.when(() -> SecurityUtils.isAdmin(authentication))
                    .thenReturn(true);

            // when
            ResponseEntity<UserDto> response = userController.getUserByEmail(requestedEmail, authentication);

            // then
            assertEquals(HttpStatus.OK, response.getStatusCode());
            assertNotNull(response.getBody());
            assertEquals(userDto.getEmail(), response.getBody().getEmail());

            verify(userService).getUserByEmail(requestedEmail);
            mockedSecurityUtils.verify(() -> SecurityUtils.isAdmin(authentication));
        }
    }

    @Test
    @DisplayName("GET /api/v1/users/email - Success for user accessing own email")
    void getUserByEmail_SuccessForUserOwnEmail() {
        // given
        String requestedEmail = "ivan.ivanov@example.com";
        when(userService.getUserByEmail(requestedEmail)).thenReturn(userDto);

        try (MockedStatic<SecurityUtils> mockedSecurityUtils = mockStatic(SecurityUtils.class)) {
            mockedSecurityUtils.when(() -> SecurityUtils.isAdmin(authentication))
                    .thenReturn(false);
            mockedSecurityUtils.when(() -> SecurityUtils.getEmailFromToken(authentication))
                    .thenReturn(requestedEmail);

            // when
            ResponseEntity<UserDto> response = userController.getUserByEmail(requestedEmail, authentication);

            // then
            assertEquals(HttpStatus.OK, response.getStatusCode());
            assertNotNull(response.getBody());
            assertEquals(userDto.getEmail(), response.getBody().getEmail());

            verify(userService).getUserByEmail(requestedEmail);
            mockedSecurityUtils.verify(() -> SecurityUtils.isAdmin(authentication));
            mockedSecurityUtils.verify(() -> SecurityUtils.getEmailFromToken(authentication), times(2));
        }
    }

    @Test
    @DisplayName("GET /api/v1/users/email - Access denied for user accessing other email")
    void getUserByEmail_AccessDeniedForOtherEmail() {
        // given
        String requestedEmail = "other@example.com";
        String userEmail = "ivan.ivanov@example.com";

        try (MockedStatic<SecurityUtils> mockedSecurityUtils = mockStatic(SecurityUtils.class)) {
            mockedSecurityUtils.when(() -> SecurityUtils.isAdmin(authentication))
                    .thenReturn(false);
            mockedSecurityUtils.when(() -> SecurityUtils.getEmailFromToken(authentication))
                    .thenReturn(userEmail);

            // when & then
            assertThrows(AccessDeniedException.class, () -> 
                userController.getUserByEmail(requestedEmail, authentication));

            verify(userService, never()).getUserByEmail(anyString());
            mockedSecurityUtils.verify(() -> SecurityUtils.isAdmin(authentication));
            mockedSecurityUtils.verify(() -> SecurityUtils.getEmailFromToken(authentication), times(2));
        }
    }

    // ================= PUT /api/v1/users/me Tests =================

    @Test
    @DisplayName("PUT /api/v1/users/me - Success")
    void updateCurrentUser_Success() {
        // given
        String userEmail = "ivan.ivanov@example.com";
        UserDto updatedUser = new UserDto();
        updatedUser.setId(1L);
        updatedUser.setFirstName("Updated Ivan");
        updatedUser.setLastName("Updated Ivanov");
        updatedUser.setEmail(userEmail);

        when(userService.updateCurrentUser(userEmail, updateUserDto)).thenReturn(updatedUser);

        try (MockedStatic<SecurityUtils> mockedSecurityUtils = mockStatic(SecurityUtils.class)) {
            mockedSecurityUtils.when(() -> SecurityUtils.getEmailFromToken(authentication))
                    .thenReturn(userEmail);

            // when
            ResponseEntity<UserDto> response = userController.updateCurrentUser(updateUserDto, authentication);

            // then
            assertEquals(HttpStatus.OK, response.getStatusCode());
            assertNotNull(response.getBody());
            assertEquals(updatedUser.getId(), response.getBody().getId());
            assertEquals("Updated Ivan", response.getBody().getFirstName());
            assertEquals("Updated Ivanov", response.getBody().getLastName());

            verify(userService).updateCurrentUser(userEmail, updateUserDto);
            mockedSecurityUtils.verify(() -> SecurityUtils.getEmailFromToken(authentication));
        }
    }

    @Test
    @DisplayName("PUT /api/v1/users/me - Access denied when token extraction fails")
    void updateCurrentUser_AccessDeniedWhenTokenExtractionFails() {
        // given
        try (MockedStatic<SecurityUtils> mockedSecurityUtils = mockStatic(SecurityUtils.class)) {
            mockedSecurityUtils.when(() -> SecurityUtils.getEmailFromToken(authentication))
                    .thenThrow(new IllegalStateException("Token extraction failed"));

            // when & then
            assertThrows(AccessDeniedException.class, () -> 
                userController.updateCurrentUser(updateUserDto, authentication));

            verify(userService, never()).updateCurrentUser(anyString(), any(UpdateUserDto.class));
            mockedSecurityUtils.verify(() -> SecurityUtils.getEmailFromToken(authentication));
        }
    }

    // ================= PUT /api/v1/users/{id} Tests =================

    @Test
    @DisplayName("PUT /api/v1/users/{id} - Success for admin")
    void updateUser_SuccessForAdmin() {
        // given
        Long userId = 1L;
        String adminEmail = "admin@example.com";
        UserDto updatedUser = new UserDto();
        updatedUser.setId(userId);
        updatedUser.setFirstName("Admin Updated");

        when(userService.updateUserByAdmin(userId, updateUserDto, adminEmail)).thenReturn(updatedUser);

        try (MockedStatic<SecurityUtils> mockedSecurityUtils = mockStatic(SecurityUtils.class)) {
            mockedSecurityUtils.when(() -> SecurityUtils.isAdmin(authentication))
                    .thenReturn(true);
            mockedSecurityUtils.when(() -> SecurityUtils.getEmailFromToken(authentication))
                    .thenReturn(adminEmail);

            // when
            ResponseEntity<UserDto> response = userController.updateUser(userId, updateUserDto, authentication);

            // then
            assertEquals(HttpStatus.OK, response.getStatusCode());
            assertNotNull(response.getBody());
            assertEquals(userId, response.getBody().getId());
            assertEquals("Admin Updated", response.getBody().getFirstName());

            verify(userService).updateUserByAdmin(userId, updateUserDto, adminEmail);
            mockedSecurityUtils.verify(() -> SecurityUtils.isAdmin(authentication));
            mockedSecurityUtils.verify(() -> SecurityUtils.getEmailFromToken(authentication));
        }
    }

    @Test
    @DisplayName("PUT /api/v1/users/{id} - Access denied for non-admin")
    void updateUser_AccessDeniedForNonAdmin() {
        // given
        Long userId = 1L;

        try (MockedStatic<SecurityUtils> mockedSecurityUtils = mockStatic(SecurityUtils.class)) {
            mockedSecurityUtils.when(() -> SecurityUtils.isAdmin(authentication))
                    .thenReturn(false);

            // when & then
            assertThrows(AccessDeniedException.class, () -> 
                userController.updateUser(userId, updateUserDto, authentication));

            verify(userService, never()).updateUserByAdmin(anyLong(), any(UpdateUserDto.class), anyString());
            mockedSecurityUtils.verify(() -> SecurityUtils.isAdmin(authentication));
        }
    }

    // ================= DELETE /api/v1/users/{id} Tests =================

    @Test
    @DisplayName("DELETE /api/v1/users/{id} - Success for admin")
    void deleteUser_SuccessForAdmin() {
        // given
        Long userId = 1L;
        String adminEmail = "admin@example.com";
        when(userService.findUserById(userId)).thenReturn(userDto);

        try (MockedStatic<SecurityUtils> mockedSecurityUtils = mockStatic(SecurityUtils.class)) {
            mockedSecurityUtils.when(() -> SecurityUtils.isAdmin(authentication))
                    .thenReturn(true);
            mockedSecurityUtils.when(() -> SecurityUtils.getEmailFromToken(authentication))
                    .thenReturn(adminEmail);

            // when
            ResponseEntity<Void> response = userController.deleteUser(userId, authentication);

            // then
            assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
            assertNull(response.getBody());

            verify(userService).findUserById(userId);
            verify(userService).deleteUser(userId);
            mockedSecurityUtils.verify(() -> SecurityUtils.isAdmin(authentication));
            mockedSecurityUtils.verify(() -> SecurityUtils.getEmailFromToken(authentication), times(2));
        }
    }

    @Test
    @DisplayName("DELETE /api/v1/users/{id} - Access denied for non-admin")
    void deleteUser_AccessDeniedForNonAdmin() {
        // given
        Long userId = 1L;
        String userEmail = "user@example.com";

        try (MockedStatic<SecurityUtils> mockedSecurityUtils = mockStatic(SecurityUtils.class)) {
            mockedSecurityUtils.when(() -> SecurityUtils.isAdmin(authentication))
                    .thenReturn(false);
            mockedSecurityUtils.when(() -> SecurityUtils.getEmailFromToken(authentication))
                    .thenReturn(userEmail);

            // when & then
            assertThrows(AccessDeniedException.class, () -> 
                userController.deleteUser(userId, authentication));

            verify(userService, never()).findUserById(anyLong());
            verify(userService, never()).deleteUser(anyLong());
            mockedSecurityUtils.verify(() -> SecurityUtils.isAdmin(authentication));
            mockedSecurityUtils.verify(() -> SecurityUtils.getEmailFromToken(authentication), times(2));
        }
    }

    // ================= GET /api/v1/users/admin/all Tests =================

    @Test
    @DisplayName("GET /api/v1/users/admin/all - Success")
    void getAllUsersForAdmin_Success() {
        // given
        String adminEmail = "admin@example.com";
        when(userService.getAllUsersForAdmin(0, 10)).thenReturn(pagedUserResponse);

        try (MockedStatic<SecurityUtils> mockedSecurityUtils = mockStatic(SecurityUtils.class)) {
            mockedSecurityUtils.when(() -> SecurityUtils.getEmailFromToken(authentication))
                    .thenReturn(adminEmail);

            // when
            ResponseEntity<PagedUserResponse> response = userController.getAllUsersForAdmin(0, 10, authentication);

            // then
            assertEquals(HttpStatus.OK, response.getStatusCode());
            assertNotNull(response.getBody());
            assertEquals(1L, response.getBody().getTotalElements());
            assertEquals(1, response.getBody().getContent().size());

            verify(userService).getAllUsersForAdmin(0, 10);
            mockedSecurityUtils.verify(() -> SecurityUtils.getEmailFromToken(authentication));
        }
    }

    // ================= GET /api/v1/users/admin/search Tests =================

    @Test
    @DisplayName("GET /api/v1/users/admin/search - Success")
    void searchUsersByEmail_Success() {
        // given
        String adminEmail = "admin@example.com";
        String emailPattern = "ivan";
        List<UserDto> searchResults = List.of(userDto);
        when(userService.searchUsersByEmail(emailPattern)).thenReturn(searchResults);

        try (MockedStatic<SecurityUtils> mockedSecurityUtils = mockStatic(SecurityUtils.class)) {
            mockedSecurityUtils.when(() -> SecurityUtils.getEmailFromToken(authentication))
                    .thenReturn(adminEmail);

            // when
            ResponseEntity<List<UserDto>> response = userController.searchUsersByEmail(emailPattern, authentication);

            // then
            assertEquals(HttpStatus.OK, response.getStatusCode());
            assertNotNull(response.getBody());
            assertEquals(1, response.getBody().size());
            assertEquals(userDto.getEmail(), response.getBody().get(0).getEmail());

            verify(userService).searchUsersByEmail(emailPattern);
            mockedSecurityUtils.verify(() -> SecurityUtils.getEmailFromToken(authentication));
        }
    }

    // ================= Edge Cases and Validation Tests =================

    @Test
    @DisplayName("All endpoints return proper HTTP status codes")
    void allEndpoints_ReturnProperStatusCodes() {
        // given
        String userEmail = "ivan.ivanov@example.com";
        when(userService.getUserByEmail(userEmail)).thenReturn(userDto);
        when(userService.createUser(userDto)).thenReturn(userDto);
        when(userService.findAllUsers(0, 5)).thenReturn(pagedUserResponse);

        try (MockedStatic<SecurityUtils> mockedSecurityUtils = mockStatic(SecurityUtils.class)) {
            mockedSecurityUtils.when(() -> SecurityUtils.getEmailFromToken(authentication))
                    .thenReturn(userEmail);

            // when & then
            ResponseEntity<UserDto> selfResponse = userController.getSelfUser(authentication);
            assertEquals(HttpStatus.OK, selfResponse.getStatusCode());

            ResponseEntity<UserDto> createResponse = userController.createUser(userDto);
            assertEquals(HttpStatus.OK, createResponse.getStatusCode());

            ResponseEntity<PagedUserResponse> usersResponse = userController.getUsers(0, 5);
            assertEquals(HttpStatus.OK, usersResponse.getStatusCode());
        }
    }

    @Test
    @DisplayName("Controller properly delegates to UserService")
    void controller_ProperlyDelegatesToService() {
        // given
        String userEmail = "ivan.ivanov@example.com";
        when(userService.getUserByEmail(userEmail)).thenReturn(userDto);
        when(userService.createUser(userDto)).thenReturn(userDto);
        // NOTE: createUserFromToken is commented out in UserController
        // when(userService.createUserFromToken(userEmail, createUserFromTokenRequest)).thenReturn(userDto);

        try (MockedStatic<SecurityUtils> mockedSecurityUtils = mockStatic(SecurityUtils.class)) {
            mockedSecurityUtils.when(() -> SecurityUtils.getEmailFromToken(authentication))
                    .thenReturn(userEmail);

            // when
            userController.getSelfUser(authentication);
            userController.createUser(userDto);
            // NOTE: createUserFromToken is commented out in UserController
            // userController.createUserFromToken(createUserFromTokenRequest, authentication);

            // then
            verify(userService).getUserByEmail(userEmail);
            verify(userService).createUser(userDto);
            // NOTE: createUserFromToken is commented out in UserController
            // verify(userService).createUserFromToken(userEmail, createUserFromTokenRequest);
        }
    }
}