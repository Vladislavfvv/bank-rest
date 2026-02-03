package com.example.bankcards.controller;

import com.example.bankcards.dto.card.BlockRequestDto;
import com.example.bankcards.dto.card.CardDto;
import com.example.bankcards.dto.card.CreateCardRequest;
import com.example.bankcards.dto.user.UserDto;
import com.example.bankcards.entity.RequestStatus;
import com.example.bankcards.entity.Status;
import com.example.bankcards.service.BlockRequestService;
import com.example.bankcards.service.CardService;
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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for CardController.
 * These tests focus on testing the controller logic without Spring context.
 * Security and authentication are mocked to test business logic.
 */
@ExtendWith(MockitoExtension.class)
class CardControllerTest {

    @Mock
    private CardService cardService;

    @Mock
    private UserService userService;

    @Mock
    private BlockRequestService blockRequestService;

    @Mock
    private Authentication authentication;

    @InjectMocks
    private CardController cardController;

    private CardDto cardDto;
    private UserDto userDto;
    private CreateCardRequest createCardRequest;

    @BeforeEach
    void setUp() {
        cardDto = new CardDto();
        cardDto.setId(1L);
        // Number is null in DTO for security (encrypted in DB)
        cardDto.setNumber(null);
        cardDto.setMaskedNumber("**** **** **** 3456");
        cardDto.setHolder("Ivan Ivanov");
        cardDto.setExpirationDate(LocalDate.of(2030, 12, 31));
        cardDto.setBalance(BigDecimal.valueOf(1000.00));
        cardDto.setStatus(Status.ACTIVE);
        cardDto.setUserId(1L);

        userDto = new UserDto();
        userDto.setId(1L);
        userDto.setEmail("ivan.ivanov@example.com");
        userDto.setFirstName("Ivan");
        userDto.setLastName("Ivanov");

        createCardRequest = new CreateCardRequest();
        createCardRequest.setHolder("Ivan Ivanov");
    }

    // ================= GET /api/v1/cards/{id} Tests =================

    @Test
    @DisplayName("GET /api/v1/cards/{id} - Success")
    void getCardById_Success() {
        // given
        Long cardId = 1L;
        when(cardService.getCardById(cardId)).thenReturn(cardDto);
        when(userService.findUserById(cardDto.getUserId())).thenReturn(userDto);

        try (MockedStatic<SecurityUtils> mockedSecurityUtils = mockStatic(SecurityUtils.class)) {
            mockedSecurityUtils.when(() -> SecurityUtils.hasAccess(authentication, userDto.getEmail()))
                    .thenReturn(true);

            // when
            ResponseEntity<CardDto> response = cardController.getCardById(cardId, authentication);

            // then
            assertEquals(HttpStatus.OK, response.getStatusCode());
            assertNotNull(response.getBody());
            assertEquals(cardDto.getId(), response.getBody().getId());
            // Number is null in DTO for security
            assertNull(response.getBody().getNumber());
            assertEquals("**** **** **** 3456", response.getBody().getMaskedNumber());
            assertEquals(cardDto.getHolder(), response.getBody().getHolder());
            
            verify(cardService).getCardById(cardId);
            verify(userService).findUserById(cardDto.getUserId());
            mockedSecurityUtils.verify(() -> SecurityUtils.hasAccess(authentication, userDto.getEmail()));
        }
    }

    // ================= GET /api/v1/cards Tests =================

    @Test
    @DisplayName("GET /api/v1/cards - Success with pagination")
    void getAllCards_Success() {
        // given
        List<CardDto> cards = List.of(cardDto);
        Page<CardDto> cardPage = new PageImpl<>(cards, PageRequest.of(0, 10), 1);
        when(cardService.getAllCards(0, 10, null, null, null, null)).thenReturn(cardPage);

        // when
        try (MockedStatic<SecurityUtils> mockedSecurityUtils = mockStatic(SecurityUtils.class)) {
            mockedSecurityUtils.when(() -> SecurityUtils.getEmailFromToken(authentication))
                    .thenReturn("admin@example.com");
            mockedSecurityUtils.when(() -> SecurityUtils.isAdmin(authentication))
                    .thenReturn(true);

            ResponseEntity<Page<CardDto>> response = cardController.getAllCards(0, 10, null, null, null, null, authentication);

            // then
            assertEquals(HttpStatus.OK, response.getStatusCode());
            assertNotNull(response.getBody());
            assertEquals(1, response.getBody().getTotalElements());
            assertEquals(1, response.getBody().getContent().size());
            assertEquals(cardDto.getId(), response.getBody().getContent().get(0).getId());
            
            verify(cardService).getAllCards(0, 10, null, null, null, null);
            mockedSecurityUtils.verify(() -> SecurityUtils.getEmailFromToken(authentication));
            mockedSecurityUtils.verify(() -> SecurityUtils.isAdmin(authentication));
        }
    }

    @Test
    @DisplayName("GET /api/v1/cards - Empty result")
    void getAllCards_EmptyResult() {
        // given
        Page<CardDto> emptyPage = new PageImpl<>(List.of(), PageRequest.of(0, 10), 0);
        when(cardService.getAllCards(0, 10, null, null, null, null)).thenReturn(emptyPage);

        // when
        try (MockedStatic<SecurityUtils> mockedSecurityUtils = mockStatic(SecurityUtils.class)) {
            mockedSecurityUtils.when(() -> SecurityUtils.getEmailFromToken(authentication))
                    .thenReturn("user@example.com");
            mockedSecurityUtils.when(() -> SecurityUtils.isAdmin(authentication))
                    .thenReturn(false);

            ResponseEntity<Page<CardDto>> response = cardController.getAllCards(0, 10, null, null, null, null, authentication);

            // then
            assertEquals(HttpStatus.OK, response.getStatusCode());
            assertNotNull(response.getBody());
            assertEquals(0, response.getBody().getTotalElements());
            assertTrue(response.getBody().getContent().isEmpty());
            
            verify(cardService).getAllCards(0, 10, null, null, null, null);
            mockedSecurityUtils.verify(() -> SecurityUtils.getEmailFromToken(authentication));
            mockedSecurityUtils.verify(() -> SecurityUtils.isAdmin(authentication));
        }
    }

    @Test
    @DisplayName("GET /api/v1/cards - Success with filters")
    void getAllCards_WithFilters() {
        // given
        List<CardDto> cards = List.of(cardDto);
        Page<CardDto> cardPage = new PageImpl<>(cards, PageRequest.of(0, 10), 1);
        Status status = Status.ACTIVE;
        LocalDate fromDate = LocalDate.of(2025, 1, 1);
        LocalDate toDate = LocalDate.of(2035, 12, 31);
        String lastFourDigits = "3456";
        
        when(cardService.getAllCards(0, 10, status, fromDate, toDate, lastFourDigits)).thenReturn(cardPage);

        // when
        try (MockedStatic<SecurityUtils> mockedSecurityUtils = mockStatic(SecurityUtils.class)) {
            mockedSecurityUtils.when(() -> SecurityUtils.getEmailFromToken(authentication))
                    .thenReturn("admin@example.com");
            mockedSecurityUtils.when(() -> SecurityUtils.isAdmin(authentication))
                    .thenReturn(true);

            ResponseEntity<Page<CardDto>> response = cardController.getAllCards(
                    0, 10, status, fromDate, toDate, lastFourDigits, authentication);

            // then
            assertEquals(HttpStatus.OK, response.getStatusCode());
            assertNotNull(response.getBody());
            assertEquals(1, response.getBody().getTotalElements());
            
            verify(cardService).getAllCards(0, 10, status, fromDate, toDate, lastFourDigits);
            mockedSecurityUtils.verify(() -> SecurityUtils.getEmailFromToken(authentication));
            mockedSecurityUtils.verify(() -> SecurityUtils.isAdmin(authentication));
        }
    }

    @Test
    @DisplayName("GET /api/v1/cards - Admin role - Returns all cards")
    void getAllCards_AdminRole_ReturnsAllCards() {
        // given
        List<CardDto> cards = List.of(cardDto);
        Page<CardDto> cardPage = new PageImpl<>(cards, PageRequest.of(0, 10), 1);
        
        try (MockedStatic<SecurityUtils> mockedSecurityUtils = mockStatic(SecurityUtils.class)) {
            mockedSecurityUtils.when(() -> SecurityUtils.isAdmin(authentication))
                    .thenReturn(true);
            
            when(cardService.getAllCards(0, 10, null, null, null, null)).thenReturn(cardPage);

            // when
            ResponseEntity<Page<CardDto>> response = cardController.getAllCards(
                    0, 10, null, null, null, null, authentication);

            // then
            assertEquals(HttpStatus.OK, response.getStatusCode());
            assertNotNull(response.getBody());
            assertEquals(1, response.getBody().getTotalElements());
            
            verify(cardService).getAllCards(0, 10, null, null, null, null);
        }
    }

    @Test
    @DisplayName("GET /api/v1/cards - User role - Returns only user's cards")
    void getAllCards_UserRole_ReturnsUserCardsOnly() {
        // given
        List<CardDto> cards = List.of(cardDto);
        Page<CardDto> cardPage = new PageImpl<>(cards, PageRequest.of(0, 10), 1);
        
        try (MockedStatic<SecurityUtils> mockedSecurityUtils = mockStatic(SecurityUtils.class)) {
            mockedSecurityUtils.when(() -> SecurityUtils.isAdmin(authentication))
                    .thenReturn(false);
            
            when(cardService.getAllCards(0, 10, null, null, null, null)).thenReturn(cardPage);

            // when
            ResponseEntity<Page<CardDto>> response = cardController.getAllCards(
                    0, 10, null, null, null, null, authentication);

            // then
            assertEquals(HttpStatus.OK, response.getStatusCode());
            assertNotNull(response.getBody());
            assertEquals(1, response.getBody().getTotalElements());
            
            verify(cardService).getAllCards(0, 10, null, null, null, null);
        }
    }

    // ================= POST /api/v1/cards Tests =================

//    @Test
//    @DisplayName("POST /api/v1/cards - Success")
//    void addCard_Success() {
//        // given
//        when(cardService.save(any(CardDto.class))).thenReturn(cardDto);
//
//        // when
//        try (MockedStatic<SecurityUtils> mockedSecurityUtils = mockStatic(SecurityUtils.class)) {
//            mockedSecurityUtils.when(() -> SecurityUtils.getEmailFromToken(authentication))
//                    .thenReturn("admin@example.com");
//
//            ResponseEntity<CardDto> response = cardController.addCard(cardDto, authentication);
//
//            // then
//            assertEquals(HttpStatus.CREATED, response.getStatusCode());
//            assertNotNull(response.getBody());
//            assertEquals(cardDto.getId(), response.getBody().getId());
//            // Number is null in DTO for security
//            assertNull(response.getBody().getNumber());
//            assertEquals("**** **** **** 3456", response.getBody().getMaskedNumber());
//            assertEquals(cardDto.getHolder(), response.getBody().getHolder());
//            
//            verify(cardService).save(cardDto);
//            mockedSecurityUtils.verify(() -> SecurityUtils.getEmailFromToken(authentication));
//        }
//    }

    // ================= PUT /api/v1/cards/{id} Tests =================

    @Test
    @DisplayName("PUT /api/v1/cards/{id} - Success")
    void updateCard_Success() {
        // given
        Long cardId = 1L;
        CardDto updatedCard = new CardDto();
        updatedCard.setId(cardId);
        updatedCard.setNumber(null); // Number is null in DTO for security
        updatedCard.setMaskedNumber("**** **** **** 3456");
        updatedCard.setHolder("Updated Holder");
        updatedCard.setExpirationDate(LocalDate.of(2031, 12, 31));
        updatedCard.setBalance(BigDecimal.valueOf(1500.00));
        updatedCard.setStatus(Status.ACTIVE);
        updatedCard.setUserId(1L);

        when(cardService.getCardById(cardId)).thenReturn(cardDto);
        when(userService.findUserById(cardDto.getUserId())).thenReturn(userDto);
        when(cardService.updateCard(eq(cardId), any(CardDto.class))).thenReturn(updatedCard);

        try (MockedStatic<SecurityUtils> mockedSecurityUtils = mockStatic(SecurityUtils.class)) {
            mockedSecurityUtils.when(() -> SecurityUtils.isAdmin(authentication))
                    .thenReturn(true);

            // when
            ResponseEntity<CardDto> response = cardController.updateCard(cardId, updatedCard, authentication);

            // then
            assertEquals(HttpStatus.OK, response.getStatusCode());
            assertNotNull(response.getBody());
            assertEquals(cardId, response.getBody().getId());
            assertEquals("Updated Holder", response.getBody().getHolder());
            assertEquals(BigDecimal.valueOf(1500.00), response.getBody().getBalance());
            
            verify(cardService).getCardById(cardId);
            verify(userService).findUserById(cardDto.getUserId());
            verify(cardService).updateCard(cardId, updatedCard);
            mockedSecurityUtils.verify(() -> SecurityUtils.isAdmin(authentication));
        }
    }

    // ================= DELETE /api/v1/cards/{id} Tests =================

    @Test
    @DisplayName("DELETE /api/v1/cards/{id} - Success")
    void deleteCard_Success() {
        // given
        Long cardId = 1L;
        when(cardService.getCardById(cardId)).thenReturn(cardDto);

        try (MockedStatic<SecurityUtils> mockedSecurityUtils = mockStatic(SecurityUtils.class)) {
            mockedSecurityUtils.when(() -> SecurityUtils.isAdmin(authentication))
                    .thenReturn(true);

            // when
            ResponseEntity<Void> response = cardController.deleteCard(cardId, authentication);

            // then
            assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
            assertNull(response.getBody());
            
            verify(cardService).getCardById(cardId);
            verify(cardService).deleteCard(cardId);
            mockedSecurityUtils.verify(() -> SecurityUtils.isAdmin(authentication));
        }
    }

    // ================= Admin Endpoints Tests =================

    @Test
    @DisplayName("POST /api/v1/cards/admin/create-for-user/{userId} - Success")
    void createCardForUser_Success() {
        // given
        Long userId = 1L;
        when(cardService.createCardForUser(eq(userId), any(CreateCardRequest.class))).thenReturn(cardDto);

        try (MockedStatic<SecurityUtils> mockedSecurityUtils = mockStatic(SecurityUtils.class)) {
            mockedSecurityUtils.when(() -> SecurityUtils.getEmailFromToken(authentication))
                    .thenReturn("admin@example.com");

            // when
            ResponseEntity<CardDto> response = cardController.createCardForUser(userId, createCardRequest, authentication);

            // then
            assertEquals(HttpStatus.CREATED, response.getStatusCode());
            assertNotNull(response.getBody());
            assertEquals(cardDto.getId(), response.getBody().getId());
            assertEquals(cardDto.getHolder(), response.getBody().getHolder());
            
            verify(cardService).createCardForUser(userId, createCardRequest);
            mockedSecurityUtils.verify(() -> SecurityUtils.getEmailFromToken(authentication));
        }
    }

    @Test
    @DisplayName("PUT /api/v1/cards/admin/{id}/block - Success")
    void blockCard_Success() {
        // given
        Long cardId = 1L;
        CardDto blockedCard = new CardDto();
        blockedCard.setId(cardId);
        blockedCard.setStatus(Status.BLOCKED);
        blockedCard.setNumber(null); // Number is null in DTO for security
        blockedCard.setMaskedNumber("**** **** **** 3456");
        blockedCard.setHolder("Ivan Ivanov");
        blockedCard.setUserId(1L);

        when(cardService.blockCard(cardId)).thenReturn(blockedCard);

        try (MockedStatic<SecurityUtils> mockedSecurityUtils = mockStatic(SecurityUtils.class)) {
            mockedSecurityUtils.when(() -> SecurityUtils.getEmailFromToken(authentication))
                    .thenReturn("admin@example.com");

            // when
            ResponseEntity<CardDto> response = cardController.blockCard(cardId, authentication);

            // then
            assertEquals(HttpStatus.OK, response.getStatusCode());
            assertNotNull(response.getBody());
            assertEquals(cardId, response.getBody().getId());
            assertEquals(Status.BLOCKED, response.getBody().getStatus());
            
            verify(cardService).blockCard(cardId);
            mockedSecurityUtils.verify(() -> SecurityUtils.getEmailFromToken(authentication));
        }
    }

    @Test
    @DisplayName("PUT /api/v1/cards/admin/{id}/activate - Success")
    void activateCard_Success() {
        // given
        Long cardId = 1L;
        CardDto activatedCard = new CardDto();
        activatedCard.setId(cardId);
        activatedCard.setStatus(Status.ACTIVE);
        activatedCard.setNumber(null); // Number is null in DTO for security
        activatedCard.setMaskedNumber("**** **** **** 3456");
        activatedCard.setHolder("Ivan Ivanov");
        activatedCard.setUserId(1L);

        when(cardService.activateCard(cardId)).thenReturn(activatedCard);

        try (MockedStatic<SecurityUtils> mockedSecurityUtils = mockStatic(SecurityUtils.class)) {
            mockedSecurityUtils.when(() -> SecurityUtils.getEmailFromToken(authentication))
                    .thenReturn("admin@example.com");

            // when
            ResponseEntity<CardDto> response = cardController.activateCard(cardId, authentication);

            // then
            assertEquals(HttpStatus.OK, response.getStatusCode());
            assertNotNull(response.getBody());
            assertEquals(cardId, response.getBody().getId());
            assertEquals(Status.ACTIVE, response.getBody().getStatus());
            
            verify(cardService).activateCard(cardId);
            mockedSecurityUtils.verify(() -> SecurityUtils.getEmailFromToken(authentication));
        }
    }

    @Test
    @DisplayName("GET /api/v1/cards/admin/user/{userId} - Admin Success")
    void getUserCards_AdminSuccess() {
        // given
        Long userId = 1L;
        List<CardDto> cards = List.of(cardDto);
        Page<CardDto> cardPage = new PageImpl<>(cards, PageRequest.of(0, 10), 1);
        when(cardService.getUserCardsForAdmin(userId, 0, 10)).thenReturn(cardPage);

        try (MockedStatic<SecurityUtils> mockedSecurityUtils = mockStatic(SecurityUtils.class)) {
            mockedSecurityUtils.when(() -> SecurityUtils.getEmailFromToken(authentication))
                    .thenReturn("admin@example.com");
            mockedSecurityUtils.when(() -> SecurityUtils.isAdmin(authentication))
                    .thenReturn(true);

            // when
            ResponseEntity<Page<CardDto>> response = cardController.getUserCards(userId, 0, 10, authentication);

            // then
            assertEquals(HttpStatus.OK, response.getStatusCode());
            assertNotNull(response.getBody());
            assertEquals(1, response.getBody().getTotalElements());
            assertEquals(1, response.getBody().getContent().size());
            assertEquals(cardDto.getId(), response.getBody().getContent().get(0).getId());
            
            verify(cardService).getUserCardsForAdmin(userId, 0, 10);
            mockedSecurityUtils.verify(() -> SecurityUtils.getEmailFromToken(authentication));
            mockedSecurityUtils.verify(() -> SecurityUtils.isAdmin(authentication));
        }
    }

    @Test
    @DisplayName("GET /api/v1/cards/admin/user/{userId} - User Success (own cards)")
    void getUserCards_UserSuccess_OwnCards() {
        // given
        Long userId = 1L;
        List<CardDto> cards = List.of(cardDto);
        Page<CardDto> cardPage = new PageImpl<>(cards, PageRequest.of(0, 10), 1);
        when(cardService.getUserCardsForAdmin(userId, 0, 10)).thenReturn(cardPage);
        when(userService.getUserByEmail("ivan.ivanov@example.com")).thenReturn(userDto);

        try (MockedStatic<SecurityUtils> mockedSecurityUtils = mockStatic(SecurityUtils.class)) {
            mockedSecurityUtils.when(() -> SecurityUtils.getEmailFromToken(authentication))
                    .thenReturn("ivan.ivanov@example.com");
            mockedSecurityUtils.when(() -> SecurityUtils.isAdmin(authentication))
                    .thenReturn(false);

            // when
            ResponseEntity<Page<CardDto>> response = cardController.getUserCards(userId, 0, 10, authentication);

            // then
            assertEquals(HttpStatus.OK, response.getStatusCode());
            assertNotNull(response.getBody());
            assertEquals(1, response.getBody().getTotalElements());
            assertEquals(1, response.getBody().getContent().size());
            assertEquals(cardDto.getId(), response.getBody().getContent().get(0).getId());
            
            verify(cardService).getUserCardsForAdmin(userId, 0, 10);
            verify(userService).getUserByEmail("ivan.ivanov@example.com");
            mockedSecurityUtils.verify(() -> SecurityUtils.getEmailFromToken(authentication));
            mockedSecurityUtils.verify(() -> SecurityUtils.isAdmin(authentication));
        }
    }

    @Test
    @DisplayName("GET /api/v1/cards/admin/user/{userId} - User Access Denied (other user's cards)")
    void getUserCards_UserAccessDenied_OtherUserCards() {
        // given
        Long otherUserId = 2L;
        UserDto otherUser = new UserDto();
        otherUser.setId(2L);
        otherUser.setEmail("other@example.com");
        when(userService.getUserByEmail("ivan.ivanov@example.com")).thenReturn(userDto);

        try (MockedStatic<SecurityUtils> mockedSecurityUtils = mockStatic(SecurityUtils.class)) {
            mockedSecurityUtils.when(() -> SecurityUtils.getEmailFromToken(authentication))
                    .thenReturn("ivan.ivanov@example.com");
            mockedSecurityUtils.when(() -> SecurityUtils.isAdmin(authentication))
                    .thenReturn(false);

            // when & then
            org.springframework.security.access.AccessDeniedException exception = 
                org.junit.jupiter.api.Assertions.assertThrows(
                    org.springframework.security.access.AccessDeniedException.class,
                    () -> cardController.getUserCards(otherUserId, 0, 10, authentication)
                );

            assertTrue(exception.getMessage().contains("Access denied"));
            verify(userService).getUserByEmail("ivan.ivanov@example.com");
            verify(cardService, org.mockito.Mockito.never()).getUserCardsForAdmin(any(), anyInt(), anyInt());
            mockedSecurityUtils.verify(() -> SecurityUtils.getEmailFromToken(authentication));
            mockedSecurityUtils.verify(() -> SecurityUtils.isAdmin(authentication));
        }
    }

    // ================= User Endpoints Tests =================

    @Test
    @DisplayName("POST /api/v1/cards/{id}/request-block - Success")
    void requestCardBlock_Success() {
        // given
        Long cardId = 1L;
        com.example.bankcards.dto.card.CreateBlockRequest blockRequest = 
            new com.example.bankcards.dto.card.CreateBlockRequest();
        blockRequest.setReason("Lost card");

        BlockRequestDto blockRequestDto = BlockRequestDto.builder()
                .id(1L)
                .cardId(cardId)
                .cardMaskedNumber("**** **** **** 3456")
                .userId(1L)
                .userEmail("ivan.ivanov@example.com")
                .reason("Lost card")
                .status(RequestStatus.PENDING)
                .build();

        when(cardService.getCardById(cardId)).thenReturn(cardDto);
        when(userService.findUserById(cardDto.getUserId())).thenReturn(userDto);
        when(blockRequestService.createBlockRequest(cardId, blockRequest.getReason(), authentication))
                .thenReturn(blockRequestDto);

        try (MockedStatic<SecurityUtils> mockedSecurityUtils = mockStatic(SecurityUtils.class)) {
            mockedSecurityUtils.when(() -> SecurityUtils.getEmailFromToken(authentication))
                    .thenReturn("ivan.ivanov@example.com");
            mockedSecurityUtils.when(() -> SecurityUtils.hasAccess(authentication, userDto.getEmail()))
                    .thenReturn(true);

            // when
            ResponseEntity<BlockRequestDto> response = cardController.requestCardBlock(cardId, blockRequest, authentication);

            // then
            assertEquals(HttpStatus.CREATED, response.getStatusCode());
            assertNotNull(response.getBody());
            assertEquals(blockRequestDto.getId(), response.getBody().getId());
            assertEquals(cardId, response.getBody().getCardId());
            assertEquals(RequestStatus.PENDING, response.getBody().getStatus());
            
            verify(cardService).getCardById(cardId);
            verify(userService).findUserById(cardDto.getUserId());
            verify(blockRequestService).createBlockRequest(cardId, blockRequest.getReason(), authentication);
            mockedSecurityUtils.verify(() -> SecurityUtils.getEmailFromToken(authentication));
            mockedSecurityUtils.verify(() -> SecurityUtils.hasAccess(authentication, userDto.getEmail()));
        }
    }

    @Test
    @DisplayName("GET /api/v1/cards/{id}/balance - Success")
    void getCardBalance_Success() {
        // given
        Long cardId = 1L;
        BigDecimal balance = BigDecimal.valueOf(1000.00);
        cardDto.setBalance(balance);
        
        when(cardService.getCardById(cardId)).thenReturn(cardDto);
        when(userService.findUserById(cardDto.getUserId())).thenReturn(userDto);

        try (MockedStatic<SecurityUtils> mockedSecurityUtils = mockStatic(SecurityUtils.class)) {
            mockedSecurityUtils.when(() -> SecurityUtils.getEmailFromToken(authentication))
                    .thenReturn("user@example.com");
            mockedSecurityUtils.when(() -> SecurityUtils.hasAccess(authentication, userDto.getEmail()))
                    .thenReturn(true);

            // when
            ResponseEntity<BigDecimal> response = cardController.getCardBalance(cardId, authentication);

            // then
            assertEquals(HttpStatus.OK, response.getStatusCode());
            assertEquals(balance, response.getBody());
            
            verify(cardService).getCardById(cardId);
            verify(userService).findUserById(cardDto.getUserId());
            mockedSecurityUtils.verify(() -> SecurityUtils.getEmailFromToken(authentication));
            mockedSecurityUtils.verify(() -> SecurityUtils.hasAccess(authentication, userDto.getEmail()));
        }
    }

    // ========== BLOCK REQUEST TESTS ==========

    @Test
    @DisplayName("GET /api/v1/cards/admin/block-requests - Success")
    void getAllBlockRequests_Success() {
        // given
        int page = 0;
        int size = 10;
        RequestStatus status = RequestStatus.PENDING;
        
        BlockRequestDto blockRequestDto = BlockRequestDto.builder()
                .id(1L)
                .cardId(1L)
                .cardMaskedNumber("**** **** **** 3456")
                .userId(1L)
                .userEmail("user@example.com")
                .reason("Lost card")
                .status(RequestStatus.PENDING)
                .build();

        Page<BlockRequestDto> blockRequestPage = new PageImpl<>(List.of(blockRequestDto), PageRequest.of(page, size), 1);

        when(blockRequestService.getAllBlockRequests(page, size, status)).thenReturn(blockRequestPage);

        try (MockedStatic<SecurityUtils> mockedSecurityUtils = mockStatic(SecurityUtils.class)) {
            mockedSecurityUtils.when(() -> SecurityUtils.getEmailFromToken(authentication))
                    .thenReturn("admin@example.com");

            // when
            ResponseEntity<Page<BlockRequestDto>> response = cardController.getAllBlockRequests(page, size, status, authentication);

            // then
            assertEquals(HttpStatus.OK, response.getStatusCode());
            assertNotNull(response.getBody());
            assertEquals(1, response.getBody().getContent().size());
            assertEquals(blockRequestDto.getId(), response.getBody().getContent().get(0).getId());
            
            verify(blockRequestService).getAllBlockRequests(page, size, status);
            mockedSecurityUtils.verify(() -> SecurityUtils.getEmailFromToken(authentication));
        }
    }

    @Test
    @DisplayName("GET /api/v1/cards/admin/block-requests/pending-count - Success")
    void getPendingBlockRequestsCount_Success() {
        // given
        long pendingCount = 5L;
        when(blockRequestService.getPendingRequestsCount()).thenReturn(pendingCount);

        try (MockedStatic<SecurityUtils> mockedSecurityUtils = mockStatic(SecurityUtils.class)) {
            mockedSecurityUtils.when(() -> SecurityUtils.getEmailFromToken(authentication))
                    .thenReturn("admin@example.com");

            // when
            ResponseEntity<Long> response = cardController.getPendingBlockRequestsCount(authentication);

            // then
            assertEquals(HttpStatus.OK, response.getStatusCode());
            assertNotNull(response.getBody());
            assertEquals(pendingCount, response.getBody());
            
            verify(blockRequestService).getPendingRequestsCount();
            mockedSecurityUtils.verify(() -> SecurityUtils.getEmailFromToken(authentication));
        }
    }

    @Test
    @DisplayName("PUT /api/v1/cards/admin/block-requests/{requestId}/approve - Success")
    void approveBlockRequest_Success() {
        // given
        Long requestId = 1L;
        String adminComment = "Card blocked as requested";
        
        BlockRequestDto approvedDto = BlockRequestDto.builder()
                .id(requestId)
                .cardId(1L)
                .cardMaskedNumber("**** **** **** 3456")
                .userId(1L)
                .userEmail("user@example.com")
                .reason("Lost card")
                .status(RequestStatus.APPROVED)
                .adminComment(adminComment)
                .build();

        when(blockRequestService.approveBlockRequest(requestId, adminComment, authentication))
                .thenReturn(approvedDto);

        try (MockedStatic<SecurityUtils> mockedSecurityUtils = mockStatic(SecurityUtils.class)) {
            mockedSecurityUtils.when(() -> SecurityUtils.getEmailFromToken(authentication))
                    .thenReturn("admin@example.com");

            // when
            ResponseEntity<BlockRequestDto> response = cardController.approveBlockRequest(requestId, adminComment, authentication);

            // then
            assertEquals(HttpStatus.OK, response.getStatusCode());
            assertNotNull(response.getBody());
            assertEquals(requestId, response.getBody().getId());
            assertEquals(RequestStatus.APPROVED, response.getBody().getStatus());
            assertEquals(adminComment, response.getBody().getAdminComment());
            
            verify(blockRequestService).approveBlockRequest(requestId, adminComment, authentication);
            mockedSecurityUtils.verify(() -> SecurityUtils.getEmailFromToken(authentication));
        }
    }

    @Test
    @DisplayName("PUT /api/v1/cards/admin/block-requests/{requestId}/reject - Success")
    void rejectBlockRequest_Success() {
        // given
        Long requestId = 1L;
        String adminComment = "Request rejected - insufficient reason";
        
        BlockRequestDto rejectedDto = BlockRequestDto.builder()
                .id(requestId)
                .cardId(1L)
                .cardMaskedNumber("**** **** **** 3456")
                .userId(1L)
                .userEmail("user@example.com")
                .reason("Lost card")
                .status(RequestStatus.REJECTED)
                .adminComment(adminComment)
                .build();

        when(blockRequestService.rejectBlockRequest(requestId, adminComment, authentication))
                .thenReturn(rejectedDto);

        try (MockedStatic<SecurityUtils> mockedSecurityUtils = mockStatic(SecurityUtils.class)) {
            mockedSecurityUtils.when(() -> SecurityUtils.getEmailFromToken(authentication))
                    .thenReturn("admin@example.com");

            // when
            ResponseEntity<BlockRequestDto> response = cardController.rejectBlockRequest(requestId, adminComment, authentication);

            // then
            assertEquals(HttpStatus.OK, response.getStatusCode());
            assertNotNull(response.getBody());
            assertEquals(requestId, response.getBody().getId());
            assertEquals(RequestStatus.REJECTED, response.getBody().getStatus());
            assertEquals(adminComment, response.getBody().getAdminComment());
            
            verify(blockRequestService).rejectBlockRequest(requestId, adminComment, authentication);
            mockedSecurityUtils.verify(() -> SecurityUtils.getEmailFromToken(authentication));
        }
    }
}