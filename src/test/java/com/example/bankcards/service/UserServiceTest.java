package com.example.bankcards.service;

import com.example.bankcards.dto.user.UserDto;
import com.example.bankcards.dto.user.CreateUserFromTokenRequest;
import com.example.bankcards.dto.user.PagedUserResponse;
import com.example.bankcards.dto.user.UpdateUserDto;
import com.example.bankcards.entity.Role;
import com.example.bankcards.entity.User;
import com.example.bankcards.exception.UserNotFoundException;
import com.example.bankcards.repository.UserRepository;
import com.example.bankcards.util.UserMapper;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @InjectMocks
    private UserService userService;

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserMapper userMapper;

    @Mock
    private EntityManager entityManager;

    private User user;
    private UserDto userDto;
    private CreateUserFromTokenRequest createUserFromTokenRequest;
    private UpdateUserDto updateUserDto;

    @BeforeEach
    void setUp() {
        // Set up mock EntityManager via ReflectionTestUtils,
        // since @InjectMocks doesn't inject fields with @PersistenceContext
        ReflectionTestUtils.setField(userService, "entityManager", entityManager);

        user = new User();
        user.setId(1L);
        user.setFirstName("Natasha");
        user.setLastName("Rostova");
        user.setEmail("natasha@gmail.ru");
        user.setBirthDate(LocalDate.of(1990, 1, 1));
        user.setPhoneNumber("123456789");
        user.setPassword("encodedPassword");
        user.setRole(Role.ROLE_USER);
        user.setCreatedAt(LocalDateTime.of(2000, 1, 1, 0, 0));
        user.setIsActive(true);

        userDto = new UserDto();
        userDto.setId(1L);
        userDto.setFirstName("Natasha");
        userDto.setLastName("Rostova");
        userDto.setEmail("natasha@gmail.ru");
        userDto.setBirthDate(LocalDate.of(1990, 1, 1));
        userDto.setPhoneNumber("123456789");
        userDto.setRole(Role.ROLE_USER);
        userDto.setCreatedAt(LocalDateTime.of(2000, 1, 1, 0, 0));
        userDto.setIsActive(true);

        createUserFromTokenRequest = new CreateUserFromTokenRequest();
        createUserFromTokenRequest.setFirstName("Natasha");
        createUserFromTokenRequest.setLastName("Rostova");
        createUserFromTokenRequest.setBirthDate(LocalDate.of(1990, 1, 1));

        updateUserDto = new UpdateUserDto();
        updateUserDto.setFirstName("UpdatedName");
        updateUserDto.setLastName("UpdatedLastName");
        updateUserDto.setBirthDate(LocalDate.of(1995, 5, 5));
    }


    @DisplayName("findUserById - User exists - Returns UserDto")
    @Test
    void findUserById_UserExists_ReturnsDto() {
        // given
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(userMapper.toDto(user)).thenReturn(userDto);

        // when
        UserDto result = userService.findUserById(1L);

        // then
        assertNotNull(result);
        assertEquals(userDto, result);
        assertEquals(1L, result.getId());
        verify(userRepository).findById(1L);
        verify(userMapper).toDto(user);
    }

    @DisplayName("findUserById - User not found - Throws UserNotFoundException")
    @Test
    void findUserById_UserNotFound_ThrowsException() {
        // given
        Long userId = 1L;
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        // when & then
        UserNotFoundException exception = assertThrows(
                UserNotFoundException.class,
                () -> userService.findUserById(userId)
        );

        assertEquals("User with id " + userId + " not found!", exception.getMessage());
        verify(userRepository).findById(userId);
        verifyNoInteractions(userMapper);
    }


    @DisplayName("getUserByEmail - User exists - Returns UserDto")
    @Test
    void getUserByEmail_UserExists_ReturnsDto() {
        // given
        String email = "natasha@gmail.ru";
        when(userRepository.findByEmailNativeQuery(email)).thenReturn(Optional.of(user));
        when(userMapper.toDto(user)).thenReturn(userDto);

        // when
        UserDto result = userService.getUserByEmail(email);

        // then
        assertNotNull(result);
        assertEquals(email, result.getEmail());
        assertEquals(1L, result.getId());
        verify(userRepository).findByEmailNativeQuery(email);
        verify(userMapper).toDto(user);
    }

    @DisplayName("getUserByEmail - User not found - Throws UserNotFoundException")
    @Test
    void getUserByEmail_UserNotFound_ThrowsException() {
        // given
        String email = "notfound@gmail.ru";
        when(userRepository.findByEmailNativeQuery(email)).thenReturn(Optional.empty());
        when(userRepository.findByEmailJPQL(email)).thenReturn(Optional.empty());
        when(userRepository.findByEmailNamed(email)).thenReturn(Optional.empty());

        // when & then
        UserNotFoundException exception = assertThrows(
                UserNotFoundException.class,
                () -> userService.getUserByEmail(email)
        );

        assertTrue(exception.getMessage().contains(email));
        verify(userRepository).findByEmailNamed(email);
        verifyNoInteractions(userMapper);
    }


    @DisplayName("createUserFromToken - New user - Creates and returns UserDto")
    @Test
    void createUserFromToken_NewUser_CreatesAndReturnsDto() {
        // given
        String email = "natasha@gmail.ru";
        when(userRepository.findByEmailNativeQuery(email)).thenReturn(Optional.empty());
        when(userMapper.toEntity(any(UserDto.class))).thenReturn(user);
        when(userRepository.save(any(User.class))).thenReturn(user);
        when(userMapper.toDto(user)).thenReturn(userDto);

        // when
        UserDto result = userService.createUserFromToken(email, createUserFromTokenRequest);

        // then
        assertNotNull(result);
        assertEquals(userDto, result);
        verify(userRepository).findByEmailNativeQuery(email);
        verify(userRepository).save(any(User.class));
        verify(userMapper).toDto(user);
    }

    @DisplayName("createUserFromToken - User already exists - Updates existing user")
    @Test
    void createUserFromToken_UserAlreadyExists_UpdatesExistingUser() {
        // given
        String email = "natasha@gmail.ru";
        when(userRepository.findByEmailNativeQuery(email)).thenReturn(Optional.of(user));
        when(userRepository.save(user)).thenReturn(user);
        when(userMapper.toDto(user)).thenReturn(userDto);

        // when
        UserDto result = userService.createUserFromToken(email, createUserFromTokenRequest);

        // then
        assertNotNull(result);
        assertEquals(userDto, result);
        verify(userRepository).findByEmailNativeQuery(email);
        verify(userRepository).save(user);
        verify(userMapper).toDto(user);
    }


    @DisplayName("createUser - Valid user - Creates and returns UserDto")
    @Test
    void createUser_ValidUser_CreatesAndReturnsDto() {
        // given
        when(userRepository.findByEmailNativeQuery(userDto.getEmail())).thenReturn(Optional.empty());
        when(userMapper.toEntity(userDto)).thenReturn(user);
        when(userMapper.updateCards(user, userDto.getCards())).thenReturn(new ArrayList<>());
        when(userRepository.save(user)).thenReturn(user);
        when(userMapper.toDto(user)).thenReturn(userDto);

        // when
        UserDto result = userService.createUser(userDto);

        // then
        assertNotNull(result);
        assertEquals(userDto, result);
        verify(userRepository).findByEmailNativeQuery(userDto.getEmail());
        verify(userMapper).toEntity(userDto);
        verify(userRepository).save(user);
        verify(userMapper).toDto(user);
    }


    @DisplayName("findAllUsers - Valid pagination - Returns PagedUserResponse")
    @Test
    void findAllUsers_ValidPagination_ReturnsPagedResponse() {
        // given
        int page = 0;
        int size = 10;
        List<User> users = Collections.singletonList(user);
        Page<User> userPage = new PageImpl<>(users, PageRequest.of(page, size), 1);
        List<UserDto> dtoList = Collections.singletonList(userDto);

        when(userRepository.findAll(PageRequest.of(page, size, Sort.by("id").ascending())))
                .thenReturn(userPage);
        when(userMapper.toDto(any(User.class))).thenReturn(userDto);

        // when
        PagedUserResponse result = userService.findAllUsers(page, size);

        // then
        assertNotNull(result);
        assertEquals(dtoList, result.getContent());
        assertEquals(1, result.getTotalElements());
        assertEquals(1, result.getTotalPages());
        assertEquals(page, result.getPage());
        assertEquals(size, result.getSize());
        verify(userRepository).findAll(PageRequest.of(page, size, Sort.by("id").ascending()));
    }


    @DisplayName("updateCurrentUser - Valid update - Updates and returns UserDto")
    @Test
    void updateCurrentUser_ValidUpdate_UpdatesAndReturnsDto() {
        // given
        String email = "natasha@gmail.ru";
        user.setId(1L); // Ensure user has an ID
        when(userRepository.findByEmailNativeQuery(email)).thenReturn(Optional.of(user));
        when(userRepository.findById(1L)).thenReturn(Optional.of(user)); // Mock the findById call
        when(userRepository.save(user)).thenReturn(user);
        when(userMapper.toDto(user)).thenReturn(userDto);

        // when
        UserDto result = userService.updateCurrentUser(email, updateUserDto);

        // then
        assertNotNull(result);
        assertEquals(userDto, result);
        verify(userRepository).findByEmailNativeQuery(email);
        verify(userRepository).save(user);
        verify(userMapper).toDto(user);
    }

    @DisplayName("updateCurrentUser - User not found - Throws UserNotFoundException")
    @Test
    void updateCurrentUser_UserNotFound_ThrowsException() {
        // given
        String email = "notfound@gmail.ru";
        when(userRepository.findByEmailNativeQuery(email)).thenReturn(Optional.empty());

        // when & then
        UserNotFoundException exception = assertThrows(
                UserNotFoundException.class,
                () -> userService.updateCurrentUser(email, updateUserDto)
        );

        assertTrue(exception.getMessage().contains(email));
        verify(userRepository).findByEmailNativeQuery(email);
        verify(userRepository, never()).save(any());
        verifyNoInteractions(userMapper);
    }


    @DisplayName("deleteUser - User exists - Deletes user successfully")
    @Test
    void deleteUser_UserExists_DeletesSuccessfully() {
        // given
        Long userId = 1L;
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        // when
        userService.deleteUser(userId);

        // then
        verify(userRepository).findById(userId);
        verify(userRepository).deleteById(userId);
    }

    @DisplayName("deleteUser - User not found - Throws UserNotFoundException")
    @Test
    void deleteUser_UserNotFound_ThrowsException() {
        // given
        Long userId = 1L;
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        // when & then
        UserNotFoundException exception = assertThrows(
                UserNotFoundException.class,
                () -> userService.deleteUser(userId)
        );

        assertTrue(exception.getMessage().contains(userId.toString()));
        verify(userRepository).findById(userId);
        verify(userRepository, never()).delete(any());
    }


    @DisplayName("searchUsersByEmail - Valid pattern - Returns list of users")
    @Test
    void searchUsersByEmail_ValidPattern_ReturnsUserList() {
        // given
        String emailPattern = "natasha";
        List<User> users = Collections.singletonList(user);
        when(userRepository.findByEmailContainingIgnoreCase(emailPattern)).thenReturn(users);
        when(userMapper.toDto(user)).thenReturn(userDto);

        // when
        List<UserDto> result = userService.searchUsersByEmail(emailPattern);

        // then
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(userDto, result.get(0));
        verify(userRepository).findByEmailContainingIgnoreCase(emailPattern);
        verify(userMapper).toDto(user);
    }
}