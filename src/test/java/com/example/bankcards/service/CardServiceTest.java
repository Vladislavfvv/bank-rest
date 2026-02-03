package com.example.bankcards.service;

import com.example.bankcards.dto.card.CardDto;
import com.example.bankcards.dto.card.CreateCardRequest;
import com.example.bankcards.entity.Card;
import com.example.bankcards.entity.Role;
import com.example.bankcards.entity.Status;
import com.example.bankcards.entity.User;
import com.example.bankcards.exception.CardNotFoundException;
import com.example.bankcards.exception.UserNotFoundException;
import com.example.bankcards.repository.CardRepository;
import com.example.bankcards.repository.UserRepository;
import com.example.bankcards.util.CardMapper;
import com.example.bankcards.util.CardMaskingUtils;
import com.example.bankcards.util.EncryptionService;
import com.example.bankcards.util.SecurityUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class CardServiceTest {

    @InjectMocks
    private CardService cardService;

    @Mock
    private CardRepository cardRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private CardMapper cardMapper;

    @Mock
    private EncryptionService encryptionService;

    @Mock
    private CardMaskingUtils cardMaskingUtils;

    @Mock
    private SecurityContext securityContext;

    @Mock
    private Authentication authentication;

    private MockedStatic<SecurityUtils> securityUtilsMock;

    private Card card;
    private CardDto cardDto;
    private User user;
    private CreateCardRequest createCardRequest;

    @BeforeEach
    void setUp() {
        // Mock SecurityUtils static method
        securityUtilsMock = mockStatic(SecurityUtils.class);
        // Default mock - return email from any authentication (lenient to avoid UnnecessaryStubbing)
        securityUtilsMock.when(() -> SecurityUtils.getEmailFromToken(any(Authentication.class)))
                .thenReturn("natasha@gmail.ru");

        // Setup SecurityContext mock
        when(securityContext.getAuthentication()).thenReturn(authentication);
        SecurityContextHolder.setContext(securityContext);

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

        card = new Card();
        card.setId(1L);
        // Card number and CVV are stored encrypted in DB
        card.setNumber("encrypted_card_number_1234567890123456");
        card.setHolder("Natasha Rostova");
        card.setExpirationDate(LocalDate.of(2030, 1, 1));
        card.setCvv("encrypted_cvv_123");
        card.setBalance(BigDecimal.valueOf(1000.00));
        card.setStatus(Status.ACTIVE);
        card.setUser(user);
        
        // Mock encryption service - decrypt returns plain text
        // Use lenient() because decrypt may be called multiple times
        lenient().when(encryptionService.decrypt("encrypted_card_number_1234567890123456"))
                .thenReturn("1234567890123456");
        lenient().when(encryptionService.decrypt("encrypted_cvv_123")).thenReturn("123");
        lenient().when(encryptionService.encrypt("1234567890123456"))
                .thenReturn("encrypted_card_number_1234567890123456");
        lenient().when(encryptionService.encrypt("123")).thenReturn("encrypted_cvv_123");
        lenient().when(encryptionService.decryptLastChars("encrypted_card_number_1234567890123456", 4))
                .thenReturn("3456");

        // Mock CardMaskingUtils
        lenient().when(cardMaskingUtils.getMaskedNumber(any(Card.class)))
                .thenReturn("**** **** **** 3456");

        cardDto = new CardDto();
        cardDto.setId(1L);
        cardDto.setNumber("1234567890123456");
        cardDto.setHolder("Natasha Rostova");
        cardDto.setExpirationDate(LocalDate.of(2030, 1, 1));
        cardDto.setBalance(BigDecimal.valueOf(1000.00));
        cardDto.setStatus(Status.ACTIVE);
        cardDto.setUserId(user.getId());

        createCardRequest = new CreateCardRequest();
        createCardRequest.setHolder("Natasha Rostova");
        createCardRequest.setNumber("1234567890123456");
        createCardRequest.setCvv("123");
        createCardRequest.setBalance(BigDecimal.valueOf(1000.00));
        createCardRequest.setExpirationDate(LocalDate.of(2030, 1, 1));
    }

    @AfterEach
    void tearDown() {
        if (securityUtilsMock != null) {
            securityUtilsMock.close();
        }
    }

    private JwtAuthenticationToken createMockAuthenticationToken(String email, String role) {
        Jwt jwt = Jwt.withTokenValue("mock-token")
                .header("alg", "HS256")
                .claim("sub", email)
                .claim("role", "ROLE_" + role)
                .claim("userId", user.getId())
                .claim("email", email)
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(3600))
                .build();
        return new JwtAuthenticationToken(jwt, Collections.singletonList(new SimpleGrantedAuthority("ROLE_" + role)));
    }

    @SuppressWarnings("SameParameterValue") // Parameter is always the same in current tests but method is flexible
    private void mockUserAuthentication(String email, String role) {
        JwtAuthenticationToken auth = createMockAuthenticationToken(email, role);
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    // ================= getCardById Tests =================

    @DisplayName("getCardById - Card exists - Returns CardDto")
    @Test
    void getCardById_CardExists_ReturnsCardDto() {
        // given
        mockUserAuthentication("natasha@gmail.ru", "USER");
        Long cardId = 1L;

        when(cardRepository.findByIdWithUser(cardId)).thenReturn(Optional.of(card));
        when(cardMapper.toDto(card)).thenReturn(cardDto);

        // when
        CardDto result = cardService.getCardById(cardId);

        // then
        assertNotNull(result);
        assertEquals(cardId, result.getId());
        // Number is null in DTO for security (only maskedNumber is set)
        assertNull(result.getNumber());
        assertEquals("**** **** **** 3456", result.getMaskedNumber());
        assertEquals(cardDto.getHolder(), result.getHolder());
        assertEquals(cardDto.getExpirationDate(), result.getExpirationDate());
        assertEquals(cardDto.getUserId(), result.getUserId());

        verify(cardRepository).findByIdWithUser(cardId);
        verify(cardMapper).toDto(card);
        verify(encryptionService).decrypt(card.getNumber());
    }

    @DisplayName("getCardById - Card not found - Throws CardNotFoundException")
    @Test
    void getCardById_CardNotFound_ThrowsException() {
        // given
        mockUserAuthentication("natasha@gmail.ru", "USER");
        Long nonExistentCardId = 999L;

        when(cardRepository.findByIdWithUser(nonExistentCardId)).thenReturn(Optional.empty());

        // when & then
        CardNotFoundException exception = assertThrows(
            CardNotFoundException.class, 
            () -> cardService.getCardById(nonExistentCardId)
        );

        assertTrue(exception.getMessage().contains(nonExistentCardId.toString()));
        verify(cardRepository).findByIdWithUser(nonExistentCardId);
        verify(cardMapper, never()).toDto(any());
    }

    // ================= save Tests =================

    @DisplayName("save - Valid card - Creates and returns CardDto")
    @Test
    void save_ValidCard_CreatesAndReturnsDto() {
        // given
        // Create a card entity with unencrypted number (as returned by mapper)
        Card cardEntity = new Card();
        cardEntity.setId(1L);
        cardEntity.setNumber("1234567890123456"); // Unencrypted number from DTO
        cardEntity.setHolder("Natasha Rostova");
        cardEntity.setExpirationDate(LocalDate.of(2030, 1, 1));
        cardEntity.setCvv("123"); // Unencrypted CVV from DTO
        cardEntity.setBalance(BigDecimal.valueOf(1000.00));
        cardEntity.setStatus(Status.ACTIVE);
        cardEntity.setUser(user);
        
        // Create a card with encrypted number (as it would be after encryption and saving)
        Card savedCard = new Card();
        savedCard.setId(1L);
        savedCard.setNumber("encrypted_card_number_1234567890123456"); // Encrypted number
        savedCard.setHolder("Natasha Rostova");
        savedCard.setExpirationDate(LocalDate.of(2030, 1, 1));
        savedCard.setCvv("encrypted_cvv_123");
        savedCard.setBalance(BigDecimal.valueOf(1000.00));
        savedCard.setStatus(Status.ACTIVE);
        savedCard.setUser(user);
        
        when(userRepository.findById(cardDto.getUserId())).thenReturn(Optional.of(user));
        when(cardMapper.toEntity(cardDto)).thenReturn(cardEntity);
        when(cardRepository.save(any(Card.class))).thenReturn(savedCard);
        when(cardMapper.toDto(savedCard)).thenReturn(cardDto);
        // Mock decryption for the saved card
        when(encryptionService.decrypt("encrypted_card_number_1234567890123456"))
                .thenReturn("1234567890123456");

        // when
        CardDto result = cardService.save(cardDto);

        // then
        assertNotNull(result);
        // Number is encrypted before saving and null in result DTO
        assertNull(result.getNumber());
        assertEquals("**** **** **** 3456", result.getMaskedNumber());
        verify(userRepository).findById(cardDto.getUserId());
        verify(cardMapper).toEntity(cardDto);
        verify(encryptionService).encrypt("1234567890123456");
        verify(encryptionService).encrypt("123"); // For CVV
        verify(cardRepository).save(any(Card.class));
        verify(cardMapper).toDto(savedCard);
        // decrypt is called in mapToDtoWithDecryption
        verify(encryptionService).decrypt("encrypted_card_number_1234567890123456");
    }

    @DisplayName("save - User not found - Throws UserNotFoundException")
    @Test
    void save_UserNotFound_ThrowsException() {
        // given
        when(userRepository.findById(cardDto.getUserId())).thenReturn(Optional.empty());

        // when & then
        UserNotFoundException exception = assertThrows(
            UserNotFoundException.class,
            () -> cardService.save(cardDto)
        );

        assertTrue(exception.getMessage().contains(cardDto.getUserId().toString()));
        verify(userRepository).findById(cardDto.getUserId());
        verify(cardRepository, never()).save(any());
        verifyNoInteractions(cardMapper);
    }

    // ================= getAllCards Tests =================

    @DisplayName("getAllCards - User role - Returns user's cards only")
    @Test
    void getAllCards_UserRole_ReturnsUserCardsOnly() {
        // given
        mockUserAuthentication("natasha@gmail.ru", "USER");
        int page = 0;
        int size = 10;
        List<Card> cards = List.of(card);
        Page<Card> cardPage = new PageImpl<>(cards, PageRequest.of(page, size), 1);

        when(cardRepository.findAllByUser_EmailIgnoreCaseWithFilters(
                "natasha@gmail.ru", null, null, null, PageRequest.of(page, size)))
            .thenReturn(cardPage);
        when(cardMapper.toDto(any(Card.class))).thenReturn(cardDto);

        // when
        Page<CardDto> result = cardService.getAllCards(page, size, null, null, null, null);

        // then
        assertNotNull(result);
        assertEquals(1, result.getContent().size());
        CardDto resultDto = result.getContent().get(0);
        assertNull(resultDto.getNumber()); // Number is null in DTO
        assertEquals("**** **** **** 3456", resultDto.getMaskedNumber());
        verify(cardRepository).findAllByUser_EmailIgnoreCaseWithFilters(
                "natasha@gmail.ru", null, null, null, PageRequest.of(page, size));
        verify(encryptionService).decrypt(card.getNumber()); // For masked number
    }

    @DisplayName("getAllCards - User role with status filter - Returns filtered cards")
    @Test
    void getAllCards_UserRoleWithStatusFilter_ReturnsFilteredCards() {
        // given
        mockUserAuthentication("natasha@gmail.ru", "USER");
        int page = 0;
        int size = 10;
        List<Card> cards = List.of(card);
        Page<Card> cardPage = new PageImpl<>(cards, PageRequest.of(page, size), 1);

        when(cardRepository.findAllByUser_EmailIgnoreCaseWithFilters(
                "natasha@gmail.ru", Status.ACTIVE, null, null, PageRequest.of(page, size)))
            .thenReturn(cardPage);
        when(cardMapper.toDto(any(Card.class))).thenReturn(cardDto);

        // when
        Page<CardDto> result = cardService.getAllCards(page, size, Status.ACTIVE, null, null, null);

        // then
        assertNotNull(result);
        assertEquals(1, result.getContent().size());
        verify(cardRepository).findAllByUser_EmailIgnoreCaseWithFilters(
                "natasha@gmail.ru", Status.ACTIVE, null, null, PageRequest.of(page, size));
    }

    @DisplayName("getAllCards - User role with date filters - Returns filtered cards")
    @Test
    void getAllCards_UserRoleWithDateFilters_ReturnsFilteredCards() {
        // given
        mockUserAuthentication("natasha@gmail.ru", "USER");
        int page = 0;
        int size = 10;
        List<Card> cards = List.of(card);
        LocalDate fromDate = LocalDate.of(2025, 1, 1);
        LocalDate toDate = LocalDate.of(2035, 12, 31);
        Page<Card> cardPage = new PageImpl<>(cards, PageRequest.of(page, size), 1);

        when(cardRepository.findAllByUser_EmailIgnoreCaseWithFilters(
                "natasha@gmail.ru", null, fromDate, toDate, PageRequest.of(page, size)))
            .thenReturn(cardPage);
        when(cardMapper.toDto(any(Card.class))).thenReturn(cardDto);

        // when
        Page<CardDto> result = cardService.getAllCards(page, size, null, fromDate, toDate, null);

        // then
        assertNotNull(result);
        assertEquals(1, result.getContent().size());
        verify(cardRepository).findAllByUser_EmailIgnoreCaseWithFilters(
                "natasha@gmail.ru", null, fromDate, toDate, PageRequest.of(page, size));
    }

    @DisplayName("getAllCards - User role with lastFourDigits filter - Returns filtered cards")
    @Test
    void getAllCards_UserRoleWithLastFourDigitsFilter_ReturnsFilteredCards() {
        // given
        mockUserAuthentication("natasha@gmail.ru", "USER");
        int page = 0;
        int size = 10;
        List<Card> cards = List.of(card);
        Page<Card> cardPage = new PageImpl<>(cards, PageRequest.of(page, size), 1);

        when(cardRepository.findAllByUser_EmailIgnoreCaseWithFilters(
                "natasha@gmail.ru", null, null, null, PageRequest.of(page, size)))
            .thenReturn(cardPage);
        when(cardMapper.toDto(any(Card.class))).thenReturn(cardDto);

        // when
        Page<CardDto> result = cardService.getAllCards(page, size, null, null, null, "3456");

        // then
        assertNotNull(result);
        assertEquals(1, result.getContent().size());
        CardDto resultDto = result.getContent().get(0);
        assertEquals("**** **** **** 3456", resultDto.getMaskedNumber());
        verify(cardRepository).findAllByUser_EmailIgnoreCaseWithFilters(
                "natasha@gmail.ru", null, null, null, PageRequest.of(page, size));
    }

    @DisplayName("getAllCards - User role with all filters - Returns filtered cards")
    @Test
    void getAllCards_UserRoleWithAllFilters_ReturnsFilteredCards() {
        // given
        mockUserAuthentication("natasha@gmail.ru", "USER");
        int page = 0;
        int size = 10;
        List<Card> cards = List.of(card);
        LocalDate fromDate = LocalDate.of(2025, 1, 1);
        LocalDate toDate = LocalDate.of(2035, 12, 31);
        Page<Card> cardPage = new PageImpl<>(cards, PageRequest.of(page, size), 1);

        when(cardRepository.findAllByUser_EmailIgnoreCaseWithFilters(
                "natasha@gmail.ru", Status.ACTIVE, fromDate, toDate, PageRequest.of(page, size)))
            .thenReturn(cardPage);
        when(cardMapper.toDto(any(Card.class))).thenReturn(cardDto);

        // when
        Page<CardDto> result = cardService.getAllCards(page, size, Status.ACTIVE, fromDate, toDate, "3456");

        // then
        assertNotNull(result);
        assertEquals(1, result.getContent().size());
        verify(cardRepository).findAllByUser_EmailIgnoreCaseWithFilters(
                "natasha@gmail.ru", Status.ACTIVE, fromDate, toDate, PageRequest.of(page, size));
    }

    // ================= updateCard Tests =================

    @DisplayName("updateCard - Valid update - Updates and returns CardDto")
    @Test
    void updateCard_ValidUpdate_UpdatesAndReturnsDto() {
        // given
        Long cardId = 1L;
        CardDto updateDto = new CardDto();
        updateDto.setHolder("Updated Holder");
        updateDto.setExpirationDate(LocalDate.of(2031, 12, 31));
        // Number not updated in this test

        when(cardRepository.findById(cardId)).thenReturn(Optional.of(card));
        when(cardRepository.save(card)).thenReturn(card);
        when(cardMapper.toDto(card)).thenReturn(cardDto);

        // when
        CardDto result = cardService.updateCard(cardId, updateDto);

        // then
        assertNotNull(result);
        assertNull(result.getNumber()); // Number is null in DTO for security
        assertEquals("**** **** **** 3456", result.getMaskedNumber());
        verify(cardRepository).findById(cardId);
        verify(encryptionService).decrypt(card.getNumber()); // For comparison
        verify(cardRepository).save(card);
        verify(cardMapper).toDto(card);
        verify(encryptionService).decrypt(card.getNumber()); // For mapping to DTO
    }

    @DisplayName("updateCard - Card not found - Throws CardNotFoundException")
    @Test
    void updateCard_CardNotFound_ThrowsException() {
        // given
        Long cardId = 999L;
        CardDto updateDto = new CardDto();
        when(cardRepository.findById(cardId)).thenReturn(Optional.empty());

        // when & then
        CardNotFoundException exception = assertThrows(
            CardNotFoundException.class,
            () -> cardService.updateCard(cardId, updateDto)
        );

        assertTrue(exception.getMessage().contains(cardId.toString()));
        verify(cardRepository).findById(cardId);
        verify(cardRepository, never()).save(any());
        verifyNoInteractions(cardMapper);
    }

    // ================= deleteCard Tests =================

    @DisplayName("deleteCard - Card exists - Deletes successfully")
    @Test
    void deleteCard_CardExists_DeletesSuccessfully() {
        // given
        mockUserAuthentication("natasha@gmail.ru", "ADMIN");
        Long cardId = 1L;
        when(cardRepository.findById(cardId)).thenReturn(Optional.of(card));

        // when
        cardService.deleteCard(cardId);

        // then
        verify(cardRepository).findById(cardId);
        verify(cardRepository).delete(card);
    }

    @DisplayName("deleteCard - Card not found - Throws CardNotFoundException")
    @Test
    void deleteCard_CardNotFound_ThrowsException() {
        // given
        Long cardId = 999L;
        when(cardRepository.findById(cardId)).thenReturn(Optional.empty());

        // when & then
        CardNotFoundException exception = assertThrows(
            CardNotFoundException.class,
            () -> cardService.deleteCard(cardId)
        );

        assertTrue(exception.getMessage().contains(cardId.toString()));
        verify(cardRepository).findById(cardId);
        verify(cardRepository, never()).delete(any());
    }

    // ================= createCardForUser Tests =================

    @DisplayName("createCardForUser - Valid request - Creates and returns CardDto")
    @Test
    void createCardForUser_ValidRequest_CreatesAndReturnsDto() {
        // given
        Long userId = 1L;
        
        // Card number is generated randomly, so we need to mock with any()
        // We'll capture the actual generated number through the save mock
        Card savedCard = new Card();
        savedCard.setId(1L);
        savedCard.setNumber("encrypted_generated_number"); // Will be set by answer
        savedCard.setHolder("Natasha Rostova");
        savedCard.setExpirationDate(LocalDate.of(2030, 1, 1));
        savedCard.setCvv("encrypted_cvv_123");
        savedCard.setBalance(BigDecimal.valueOf(1000.00));
        savedCard.setStatus(Status.ACTIVE);
        savedCard.setUser(user);
        
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        // Mock findByNumber to return empty (card doesn't exist yet)
        when(cardRepository.findByNumber(any(String.class))).thenReturn(Optional.empty());
        when(cardMapper.toEntity(any(CardDto.class))).thenReturn(card);
        // Mock cardMaskingUtils
        when(cardMaskingUtils.getMaskedNumber(any(String.class))).thenReturn("**** **** **** 3456");
        when(cardMaskingUtils.getMaskedNumber(any(Card.class))).thenReturn("**** **** **** 3456");
        // Capture the saved card to get the actual encrypted number
        when(cardRepository.save(any(Card.class))).thenAnswer(invocation -> {
            Card saved = invocation.getArgument(0);
            savedCard.setId(1L);
            savedCard.setNumber(saved.getNumber()); // Capture encrypted number (will be "encrypted_1234567890123456")
            savedCard.setCvv(saved.getCvv()); // Capture encrypted CVV
            savedCard.setHolder("Natasha Rostova");
            savedCard.setExpirationDate(LocalDate.of(2030, 1, 1));
            savedCard.setBalance(BigDecimal.valueOf(1000.00));
            savedCard.setStatus(Status.ACTIVE);
            savedCard.setUser(user);
            return savedCard;
        });
        // Mock toDto to return DTO with null number (for security)
        CardDto resultDto = new CardDto();
        resultDto.setId(1L);
        resultDto.setNumber(null); // Number is null in DTO for security
        resultDto.setMaskedNumber("**** **** **** 3456");
        resultDto.setHolder("Natasha Rostova");
        resultDto.setExpirationDate(LocalDate.of(2030, 1, 1));
        resultDto.setBalance(BigDecimal.valueOf(1000.00));
        resultDto.setStatus(Status.ACTIVE);
        resultDto.setUserId(userId);
        when(cardMapper.toDto(any(Card.class))).thenReturn(resultDto);
        // Mock encryption/decryption for any generated card number
        when(encryptionService.encrypt(any(String.class))).thenAnswer(invocation -> {
            String plain = invocation.getArgument(0);
            return "encrypted_" + plain;
        });
        when(encryptionService.decrypt(any(String.class))).thenAnswer(invocation -> {
            String encrypted = invocation.getArgument(0);
            if (encrypted.startsWith("encrypted_")) {
                return encrypted.substring("encrypted_".length());
            }
            return encrypted;
        });

        // when
        CardDto result = cardService.createCardForUser(userId, createCardRequest);

        // then
        assertNotNull(result);
        assertNull(result.getNumber()); // Number is null in DTO for security
        assertNotNull(result.getMaskedNumber()); // Masked number should be set
        assertTrue(result.getMaskedNumber().matches("\\*\\*\\*\\* \\*\\*\\*\\* \\*\\*\\*\\* \\d{4}")); // Format: **** **** **** 1234
        verify(userRepository).findById(userId);
        verify(encryptionService, atLeast(1)).encrypt(any(String.class)); // Encrypt card number and CVV
        verify(cardRepository).save(any(Card.class));
        verify(cardMapper).toDto(any(Card.class));
        verify(encryptionService, atLeast(1)).decrypt(any(String.class)); // For masked number
    }

    @DisplayName("createCardForUser - User not found - Throws UserNotFoundException")
    @Test
    void createCardForUser_UserNotFound_ThrowsException() {
        // given
        Long userId = 999L;
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        // when & then
        UserNotFoundException exception = assertThrows(
            UserNotFoundException.class,
            () -> cardService.createCardForUser(userId, createCardRequest)
        );

        assertTrue(exception.getMessage().contains(userId.toString()));
        verify(userRepository).findById(userId);
        verify(cardRepository, never()).save(any());
        verifyNoInteractions(cardMapper);
    }

    // ================= blockCard Tests =================

    @DisplayName("blockCard - Card exists - Blocks successfully")
    @Test
    void blockCard_CardExists_BlocksSuccessfully() {
        // given
        Long cardId = 1L;
        card.setStatus(Status.ACTIVE);
        when(cardRepository.findById(cardId)).thenReturn(Optional.of(card));
        when(cardRepository.save(card)).thenReturn(card);
        when(cardMapper.toDto(card)).thenReturn(cardDto);

        // when
        CardDto result = cardService.blockCard(cardId);

        // then
        assertNotNull(result);
        assertEquals(Status.BLOCKED, card.getStatus());
        assertNull(result.getNumber()); // Number is null in DTO
        assertEquals("**** **** **** 3456", result.getMaskedNumber());
        verify(cardRepository).findById(cardId);
        verify(cardRepository).save(card);
        verify(cardMapper).toDto(card);
        // decrypt is called in getMaskedNumberForCard (for logging) and in mapToDtoWithDecryption
        verify(encryptionService, atLeast(1)).decrypt(card.getNumber());
    }

    // ================= activateCard Tests =================

    @DisplayName("activateCard - Card exists - Activates successfully")
    @Test
    void activateCard_CardExists_ActivatesSuccessfully() {
        // given
        Long cardId = 1L;
        card.setStatus(Status.BLOCKED);
        when(cardRepository.findById(cardId)).thenReturn(Optional.of(card));
        when(cardRepository.save(card)).thenReturn(card);
        when(cardMapper.toDto(card)).thenReturn(cardDto);

        // when
        CardDto result = cardService.activateCard(cardId);

        // then
        assertNotNull(result);
        assertEquals(Status.ACTIVE, card.getStatus());
        assertNull(result.getNumber()); // Number is null in DTO
        assertEquals("**** **** **** 3456", result.getMaskedNumber());
        verify(cardRepository).findById(cardId);
        verify(cardRepository).save(card);
        verify(cardMapper).toDto(card);
        // decrypt is called in getMaskedNumberForCard (for logging) and in mapToDtoWithDecryption
        verify(encryptionService, atLeast(1)).decrypt(card.getNumber());
    }

    // ================= getUserCardsForAdmin Tests =================

    @DisplayName("getUserCardsForAdmin - Valid user - Returns user's cards")
    @Test
    void getUserCardsForAdmin_ValidUser_ReturnsUserCards() {
        // given
        Long userId = 1L;
        int page = 0;
        int size = 10;
        List<Card> cards = List.of(card);
        Page<Card> cardPage = new PageImpl<>(cards, PageRequest.of(page, size), 1);

        when(userRepository.existsById(userId)).thenReturn(true);
        when(cardRepository.findAllByUserId(userId, PageRequest.of(page, size))).thenReturn(cardPage);
        when(cardMapper.toDto(any(Card.class))).thenReturn(cardDto);

        // when
        Page<CardDto> result = cardService.getUserCardsForAdmin(userId, page, size);

        // then
        assertNotNull(result);
        assertEquals(1, result.getContent().size());
        CardDto resultDto = result.getContent().get(0);
        assertNull(resultDto.getNumber()); // Number is null in DTO
        assertEquals("**** **** **** 3456", resultDto.getMaskedNumber());
        verify(userRepository).existsById(userId);
        verify(cardRepository).findAllByUserId(userId, PageRequest.of(page, size));
        verify(encryptionService).decrypt(card.getNumber()); // For masked number
    }

    @DisplayName("getUserCardsForAdmin - User not found - Throws UserNotFoundException")
    @Test
    void getUserCardsForAdmin_UserNotFound_ThrowsException() {
        // given
        Long userId = 999L;
        int page = 0;
        int size = 10;
        when(userRepository.existsById(userId)).thenReturn(false);

        // when & then
        UserNotFoundException exception = assertThrows(
            UserNotFoundException.class,
            () -> cardService.getUserCardsForAdmin(userId, page, size)
        );

        assertTrue(exception.getMessage().contains(userId.toString()));
        verify(userRepository).existsById(userId);
        verify(cardRepository, never()).findAllByUserId(any(), any());
        verifyNoInteractions(cardMapper);
    }

    // ========== EXPIRATION TESTS ==========

    @Test
    @DisplayName("updateExpiredCardsStatus - Updates expired cards successfully")
    void updateExpiredCardsStatus_WithExpiredCards_UpdatesStatus() {
        // given
        int expectedUpdatedCount = 5;
        
        when(cardRepository.updateExpiredCardsStatus(any(LocalDate.class), eq(Status.EXPIRED)))
                .thenReturn(expectedUpdatedCount);

        // when
        int actualUpdatedCount = cardService.updateExpiredCardsStatus();

        // then
        assertEquals(expectedUpdatedCount, actualUpdatedCount);
        verify(cardRepository).updateExpiredCardsStatus(any(LocalDate.class), eq(Status.EXPIRED));
    }

    @Test
    @DisplayName("updateExpiredCardsStatus - No expired cards found")
    void updateExpiredCardsStatus_NoExpiredCards_ReturnsZero() {
        // given
        int expectedUpdatedCount = 0;
        
        when(cardRepository.updateExpiredCardsStatus(any(LocalDate.class), eq(Status.EXPIRED)))
                .thenReturn(expectedUpdatedCount);

        // when
        int actualUpdatedCount = cardService.updateExpiredCardsStatus();

        // then
        assertEquals(expectedUpdatedCount, actualUpdatedCount);
        verify(cardRepository).updateExpiredCardsStatus(any(LocalDate.class), eq(Status.EXPIRED));
    }
}