package com.example.bankcards.controller;

import com.example.bankcards.dto.transfer.CardTransferStatsDto;
import com.example.bankcards.dto.transfer.TransferDto;
import com.example.bankcards.dto.transfer.TransferRequest;
import com.example.bankcards.dto.transfer.UserTransferStatsDto;
import com.example.bankcards.entity.TransferStatus;
import com.example.bankcards.service.TransferService;
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
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

/**
 * Unit tests for TransferController.
 * Tests transfer management endpoints without Spring context.
 */
@ExtendWith(MockitoExtension.class)
class TransferControllerTest {

    @Mock
    private TransferService transferService;

    @Mock
    private Authentication authentication;

    @InjectMocks
    private TransferController transferController;

    private TransferRequest transferRequest;
    private TransferDto transferDto;
    private Page<TransferDto> transferPage;
    private CardTransferStatsDto cardTransferStatsDto;
    private UserTransferStatsDto userTransferStatsDto;

    @BeforeEach
    void setUp() {
        // Setup TransferRequest
        transferRequest = new TransferRequest();
        transferRequest.setFromCardId(1L);
        transferRequest.setToCardId(2L);
        transferRequest.setAmount(BigDecimal.valueOf(100.00));
        transferRequest.setDescription("Test transfer");
        transferRequest.setCvv("123");

        // Setup TransferDto
        transferDto = new TransferDto();
        transferDto.setId(1L);
        transferDto.setFromCardId(1L);
        transferDto.setFromCardMaskedNumber("**** **** **** 1234");
        transferDto.setToCardId(2L);
        transferDto.setToCardMaskedNumber("**** **** **** 5678");
        transferDto.setAmount(BigDecimal.valueOf(100.00));
        transferDto.setDescription("Test transfer");
        transferDto.setTransferDate(LocalDateTime.now());
        transferDto.setStatus(TransferStatus.COMPLETED);

        // Setup Page<TransferDto>
        List<TransferDto> transfers = List.of(transferDto);
        transferPage = new PageImpl<>(transfers, PageRequest.of(0, 10), 1);

        // Setup CardTransferStatsDto
        cardTransferStatsDto = new CardTransferStatsDto();
        cardTransferStatsDto.setCardId(1L);
        cardTransferStatsDto.setCardMaskedNumber("**** **** **** 1234");
        cardTransferStatsDto.setTotalIncome(BigDecimal.valueOf(500.00));
        cardTransferStatsDto.setTotalExpense(BigDecimal.valueOf(300.00));
        cardTransferStatsDto.setBalance(BigDecimal.valueOf(1000.00));
        cardTransferStatsDto.setIncomeTransfersCount(5L);
        cardTransferStatsDto.setExpenseTransfersCount(3L);

        // Setup UserTransferStatsDto
        userTransferStatsDto = new UserTransferStatsDto();
        userTransferStatsDto.setUserId(1L);
        userTransferStatsDto.setUserEmail("user@example.com");
        userTransferStatsDto.setUserFullName("Ivan Ivanov");
        userTransferStatsDto.setTotalIncome(BigDecimal.valueOf(1000.00));
        userTransferStatsDto.setTotalExpense(BigDecimal.valueOf(600.00));
        userTransferStatsDto.setTotalBalance(BigDecimal.valueOf(2000.00));
        userTransferStatsDto.setCardStats(List.of(cardTransferStatsDto));
    }

    // ================= POST /api/v1/transfers Tests =================

    @Test
    @DisplayName("POST /api/v1/transfers - Success")
    void transferBetweenCards_Success() {
        // given
        String userEmail = "user@example.com";
        when(transferService.transferBetweenCards(transferRequest)).thenReturn(transferDto);

        try (MockedStatic<SecurityUtils> mockedSecurityUtils = mockStatic(SecurityUtils.class)) {
            mockedSecurityUtils.when(() -> SecurityUtils.getEmailFromToken(authentication))
                    .thenReturn(userEmail);

            // when
            ResponseEntity<TransferDto> response = transferController.transferBetweenCards(transferRequest, authentication);

            // then
            assertEquals(HttpStatus.OK, response.getStatusCode());
            assertNotNull(response.getBody());
            assertEquals(transferDto.getId(), response.getBody().getId());
            assertEquals(transferDto.getFromCardId(), response.getBody().getFromCardId());
            assertEquals(transferDto.getToCardId(), response.getBody().getToCardId());
            assertEquals(transferDto.getAmount(), response.getBody().getAmount());
            assertEquals(TransferStatus.COMPLETED, response.getBody().getStatus());

            verify(transferService).transferBetweenCards(transferRequest);
            mockedSecurityUtils.verify(() -> SecurityUtils.getEmailFromToken(authentication));
        }
    }

    @Test
    @DisplayName("POST /api/v1/transfers - Service called with correct parameters")
    void transferBetweenCards_ServiceCalledWithCorrectParameters() {
        // given
        String userEmail = "user@example.com";
        TransferRequest specificRequest = new TransferRequest();
        specificRequest.setFromCardId(3L);
        specificRequest.setToCardId(4L);
        specificRequest.setAmount(BigDecimal.valueOf(250.00));
        specificRequest.setDescription("Specific transfer");
        specificRequest.setCvv("456");

        when(transferService.transferBetweenCards(specificRequest)).thenReturn(transferDto);

        try (MockedStatic<SecurityUtils> mockedSecurityUtils = mockStatic(SecurityUtils.class)) {
            mockedSecurityUtils.when(() -> SecurityUtils.getEmailFromToken(authentication))
                    .thenReturn(userEmail);

            // when
            transferController.transferBetweenCards(specificRequest, authentication);

            // then
            verify(transferService).transferBetweenCards(specificRequest);
            verify(transferService, times(1)).transferBetweenCards(any(TransferRequest.class));
            mockedSecurityUtils.verify(() -> SecurityUtils.getEmailFromToken(authentication));
        }
    }

    // ================= GET /api/v1/transfers/my Tests =================

    @Test
    @DisplayName("GET /api/v1/transfers/my - Success")
    void getMyTransfers_Success() {
        // given
        String userEmail = "user@example.com";
        when(transferService.getUserTransfers(0, 10)).thenReturn(transferPage);

        try (MockedStatic<SecurityUtils> mockedSecurityUtils = mockStatic(SecurityUtils.class)) {
            mockedSecurityUtils.when(() -> SecurityUtils.getEmailFromToken(authentication))
                    .thenReturn(userEmail);

            // when
            ResponseEntity<Page<TransferDto>> response = transferController.getMyTransfers(0, 10, authentication);

            // then
            assertEquals(HttpStatus.OK, response.getStatusCode());
            assertNotNull(response.getBody());
            assertEquals(1L, response.getBody().getTotalElements());
            assertEquals(1, response.getBody().getContent().size());
            assertEquals(transferDto.getId(), response.getBody().getContent().get(0).getId());

            verify(transferService).getUserTransfers(0, 10);
            mockedSecurityUtils.verify(() -> SecurityUtils.getEmailFromToken(authentication));
        }
    }

    @Test
    @DisplayName("GET /api/v1/transfers/my - Default pagination parameters")
    void getMyTransfers_DefaultPaginationParameters() {
        // given
        String userEmail = "user@example.com";
        when(transferService.getUserTransfers(0, 10)).thenReturn(transferPage);

        try (MockedStatic<SecurityUtils> mockedSecurityUtils = mockStatic(SecurityUtils.class)) {
            mockedSecurityUtils.when(() -> SecurityUtils.getEmailFromToken(authentication))
                    .thenReturn(userEmail);

            // when
            ResponseEntity<Page<TransferDto>> response = transferController.getMyTransfers(0, 10, authentication);

            // then
            assertEquals(HttpStatus.OK, response.getStatusCode());
            verify(transferService).getUserTransfers(0, 10);
            mockedSecurityUtils.verify(() -> SecurityUtils.getEmailFromToken(authentication));
        }
    }

    @Test
    @DisplayName("GET /api/v1/transfers/my - Custom pagination parameters")
    void getMyTransfers_CustomPaginationParameters() {
        // given
        String userEmail = "user@example.com";
        Page<TransferDto> customPage = new PageImpl<>(List.of(transferDto), PageRequest.of(2, 5), 11);
        when(transferService.getUserTransfers(2, 5)).thenReturn(customPage);

        try (MockedStatic<SecurityUtils> mockedSecurityUtils = mockStatic(SecurityUtils.class)) {
            mockedSecurityUtils.when(() -> SecurityUtils.getEmailFromToken(authentication))
                    .thenReturn(userEmail);

            // when
            ResponseEntity<Page<TransferDto>> response = transferController.getMyTransfers(2, 5, authentication);

            // then
            assertEquals(HttpStatus.OK, response.getStatusCode());
            assertNotNull(response.getBody());
            assertEquals(11L, response.getBody().getTotalElements());
            assertEquals(2, response.getBody().getNumber());
            assertEquals(5, response.getBody().getSize());

            verify(transferService).getUserTransfers(2, 5);
            mockedSecurityUtils.verify(() -> SecurityUtils.getEmailFromToken(authentication));
        }
    }

    // ================= GET /api/v1/transfers/card/{cardId} Tests =================

    @Test
    @DisplayName("GET /api/v1/transfers/card/{cardId} - Success")
    void getCardTransfers_Success() {
        // given
        Long cardId = 1L;
        String userEmail = "user@example.com";
        when(transferService.getCardTransfers(cardId, 0, 10)).thenReturn(transferPage);

        try (MockedStatic<SecurityUtils> mockedSecurityUtils = mockStatic(SecurityUtils.class)) {
            mockedSecurityUtils.when(() -> SecurityUtils.getEmailFromToken(authentication))
                    .thenReturn(userEmail);

            // when
            ResponseEntity<Page<TransferDto>> response = transferController.getCardTransfers(cardId, 0, 10, authentication);

            // then
            assertEquals(HttpStatus.OK, response.getStatusCode());
            assertNotNull(response.getBody());
            assertEquals(1L, response.getBody().getTotalElements());
            assertEquals(1, response.getBody().getContent().size());

            verify(transferService).getCardTransfers(cardId, 0, 10);
            mockedSecurityUtils.verify(() -> SecurityUtils.getEmailFromToken(authentication));
        }
    }

    @Test
    @DisplayName("GET /api/v1/transfers/card/{cardId} - Different card ID")
    void getCardTransfers_DifferentCardId() {
        // given
        Long cardId = 5L;
        String userEmail = "user@example.com";
        when(transferService.getCardTransfers(cardId, 0, 10)).thenReturn(transferPage);

        try (MockedStatic<SecurityUtils> mockedSecurityUtils = mockStatic(SecurityUtils.class)) {
            mockedSecurityUtils.when(() -> SecurityUtils.getEmailFromToken(authentication))
                    .thenReturn(userEmail);

            // when
            transferController.getCardTransfers(cardId, 0, 10, authentication);

            // then
            verify(transferService).getCardTransfers(cardId, 0, 10);
            verify(transferService, times(1)).getCardTransfers(anyLong(), anyInt(), anyInt());
            mockedSecurityUtils.verify(() -> SecurityUtils.getEmailFromToken(authentication));
        }
    }

    // ================= GET /api/v1/transfers/admin/card/{cardId} Tests =================

    @Test
    @DisplayName("GET /api/v1/transfers/admin/card/{cardId} - Success")
    void getCardTransfersForAdmin_Success() {
        // given
        Long cardId = 1L;
        String adminEmail = "admin@example.com";
        when(transferService.getCardTransfersForAdmin(cardId, 0, 10)).thenReturn(transferPage);

        try (MockedStatic<SecurityUtils> mockedSecurityUtils = mockStatic(SecurityUtils.class)) {
            mockedSecurityUtils.when(() -> SecurityUtils.getEmailFromToken(authentication))
                    .thenReturn(adminEmail);

            // when
            ResponseEntity<Page<TransferDto>> response = transferController.getCardTransfersForAdmin(cardId, 0, 10, authentication);

            // then
            assertEquals(HttpStatus.OK, response.getStatusCode());
            assertNotNull(response.getBody());
            assertEquals(1L, response.getBody().getTotalElements());
            assertEquals(1, response.getBody().getContent().size());

            verify(transferService).getCardTransfersForAdmin(cardId, 0, 10);
            mockedSecurityUtils.verify(() -> SecurityUtils.getEmailFromToken(authentication));
        }
    }

    @Test
    @DisplayName("GET /api/v1/transfers/admin/card/{cardId} - Custom pagination")
    void getCardTransfersForAdmin_CustomPagination() {
        // given
        Long cardId = 2L;
        String adminEmail = "admin@example.com";
        Page<TransferDto> customPage = new PageImpl<>(List.of(transferDto), PageRequest.of(1, 20), 21);
        when(transferService.getCardTransfersForAdmin(cardId, 1, 20)).thenReturn(customPage);

        try (MockedStatic<SecurityUtils> mockedSecurityUtils = mockStatic(SecurityUtils.class)) {
            mockedSecurityUtils.when(() -> SecurityUtils.getEmailFromToken(authentication))
                    .thenReturn(adminEmail);

            // when
            ResponseEntity<Page<TransferDto>> response = transferController.getCardTransfersForAdmin(cardId, 1, 20, authentication);

            // then
            assertEquals(HttpStatus.OK, response.getStatusCode());
            assertNotNull(response.getBody());
            assertEquals(21L, response.getBody().getTotalElements());
            assertEquals(1, response.getBody().getNumber());
            assertEquals(20, response.getBody().getSize());

            verify(transferService).getCardTransfersForAdmin(cardId, 1, 20);
            mockedSecurityUtils.verify(() -> SecurityUtils.getEmailFromToken(authentication));
        }
    }

    // ================= GET /api/v1/transfers/admin/card/{cardId}/stats Tests =================

    @Test
    @DisplayName("GET /api/v1/transfers/admin/card/{cardId}/stats - Success")
    void getCardTransferStats_Success() {
        // given
        Long cardId = 1L;
        String adminEmail = "admin@example.com";
        when(transferService.getCardTransferStats(cardId)).thenReturn(cardTransferStatsDto);

        try (MockedStatic<SecurityUtils> mockedSecurityUtils = mockStatic(SecurityUtils.class)) {
            mockedSecurityUtils.when(() -> SecurityUtils.getEmailFromToken(authentication))
                    .thenReturn(adminEmail);

            // when
            ResponseEntity<CardTransferStatsDto> response = transferController.getCardTransferStats(cardId, authentication);

            // then
            assertEquals(HttpStatus.OK, response.getStatusCode());
            assertNotNull(response.getBody());
            assertEquals(cardTransferStatsDto.getCardId(), response.getBody().getCardId());
            assertEquals(cardTransferStatsDto.getCardMaskedNumber(), response.getBody().getCardMaskedNumber());
            assertEquals(cardTransferStatsDto.getTotalIncome(), response.getBody().getTotalIncome());
            assertEquals(cardTransferStatsDto.getTotalExpense(), response.getBody().getTotalExpense());
            assertEquals(cardTransferStatsDto.getBalance(), response.getBody().getBalance());
            assertEquals(cardTransferStatsDto.getIncomeTransfersCount(), response.getBody().getIncomeTransfersCount());
            assertEquals(cardTransferStatsDto.getExpenseTransfersCount(), response.getBody().getExpenseTransfersCount());

            verify(transferService).getCardTransferStats(cardId);
            mockedSecurityUtils.verify(() -> SecurityUtils.getEmailFromToken(authentication));
        }
    }

    @Test
    @DisplayName("GET /api/v1/transfers/admin/card/{cardId}/stats - Different card")
    void getCardTransferStats_DifferentCard() {
        // given
        Long cardId = 3L;
        String adminEmail = "admin@example.com";
        CardTransferStatsDto differentStats = new CardTransferStatsDto();
        differentStats.setCardId(cardId);
        differentStats.setTotalIncome(BigDecimal.valueOf(750.00));
        differentStats.setTotalExpense(BigDecimal.valueOf(250.00));

        when(transferService.getCardTransferStats(cardId)).thenReturn(differentStats);

        try (MockedStatic<SecurityUtils> mockedSecurityUtils = mockStatic(SecurityUtils.class)) {
            mockedSecurityUtils.when(() -> SecurityUtils.getEmailFromToken(authentication))
                    .thenReturn(adminEmail);

            // when
            ResponseEntity<CardTransferStatsDto> response = transferController.getCardTransferStats(cardId, authentication);

            // then
            assertEquals(HttpStatus.OK, response.getStatusCode());
            assertNotNull(response.getBody());
            assertEquals(cardId, response.getBody().getCardId());
            assertEquals(BigDecimal.valueOf(750.00), response.getBody().getTotalIncome());
            assertEquals(BigDecimal.valueOf(250.00), response.getBody().getTotalExpense());

            verify(transferService).getCardTransferStats(cardId);
            mockedSecurityUtils.verify(() -> SecurityUtils.getEmailFromToken(authentication));
        }
    }

    // ================= GET /api/v1/transfers/admin/user/{userId}/stats Tests =================

    @Test
    @DisplayName("GET /api/v1/transfers/admin/user/{userId}/stats - Success")
    void getUserTransferStats_Success() {
        // given
        Long userId = 1L;
        String adminEmail = "admin@example.com";
        when(transferService.getUserTransferStats(userId)).thenReturn(userTransferStatsDto);

        try (MockedStatic<SecurityUtils> mockedSecurityUtils = mockStatic(SecurityUtils.class)) {
            mockedSecurityUtils.when(() -> SecurityUtils.getEmailFromToken(authentication))
                    .thenReturn(adminEmail);

            // when
            ResponseEntity<UserTransferStatsDto> response = transferController.getUserTransferStats(userId, authentication);

            // then
            assertEquals(HttpStatus.OK, response.getStatusCode());
            assertNotNull(response.getBody());
            assertEquals(userTransferStatsDto.getUserId(), response.getBody().getUserId());
            assertEquals(userTransferStatsDto.getUserEmail(), response.getBody().getUserEmail());
            assertEquals(userTransferStatsDto.getUserFullName(), response.getBody().getUserFullName());
            assertEquals(userTransferStatsDto.getTotalIncome(), response.getBody().getTotalIncome());
            assertEquals(userTransferStatsDto.getTotalExpense(), response.getBody().getTotalExpense());
            assertEquals(userTransferStatsDto.getTotalBalance(), response.getBody().getTotalBalance());
            assertEquals(1, response.getBody().getCardStats().size());

            verify(transferService).getUserTransferStats(userId);
            mockedSecurityUtils.verify(() -> SecurityUtils.getEmailFromToken(authentication));
        }
    }

    @Test
    @DisplayName("GET /api/v1/transfers/admin/user/{userId}/stats - Different user")
    void getUserTransferStats_DifferentUser() {
        // given
        Long userId = 2L;
        String adminEmail = "admin@example.com";
        UserTransferStatsDto differentStats = new UserTransferStatsDto();
        differentStats.setUserId(userId);
        differentStats.setUserEmail("different@example.com");
        differentStats.setTotalIncome(BigDecimal.valueOf(1500.00));
        differentStats.setTotalExpense(BigDecimal.valueOf(800.00));
        differentStats.setCardStats(List.of()); // Empty list instead of null

        when(transferService.getUserTransferStats(userId)).thenReturn(differentStats);

        try (MockedStatic<SecurityUtils> mockedSecurityUtils = mockStatic(SecurityUtils.class)) {
            mockedSecurityUtils.when(() -> SecurityUtils.getEmailFromToken(authentication))
                    .thenReturn(adminEmail);

            // when
            ResponseEntity<UserTransferStatsDto> response = transferController.getUserTransferStats(userId, authentication);

            // then
            assertEquals(HttpStatus.OK, response.getStatusCode());
            assertNotNull(response.getBody());
            assertEquals(userId, response.getBody().getUserId());
            assertEquals("different@example.com", response.getBody().getUserEmail());
            assertEquals(BigDecimal.valueOf(1500.00), response.getBody().getTotalIncome());
            assertEquals(BigDecimal.valueOf(800.00), response.getBody().getTotalExpense());

            verify(transferService).getUserTransferStats(userId);
            mockedSecurityUtils.verify(() -> SecurityUtils.getEmailFromToken(authentication));
        }
    }

    // ================= Edge Cases and Validation Tests =================

    @Test
    @DisplayName("All endpoints return proper HTTP status codes")
    void allEndpoints_ReturnProperStatusCodes() {
        // given
        String userEmail = "user@example.com";
        String adminEmail = "admin@example.com";
        Long cardId = 1L;
        Long userId = 1L;

        when(transferService.transferBetweenCards(any(TransferRequest.class))).thenReturn(transferDto);
        when(transferService.getUserTransfers(anyInt(), anyInt())).thenReturn(transferPage);
        when(transferService.getCardTransfers(anyLong(), anyInt(), anyInt())).thenReturn(transferPage);
        when(transferService.getCardTransfersForAdmin(anyLong(), anyInt(), anyInt())).thenReturn(transferPage);
        when(transferService.getCardTransferStats(anyLong())).thenReturn(cardTransferStatsDto);
        when(transferService.getUserTransferStats(anyLong())).thenReturn(userTransferStatsDto);

        try (MockedStatic<SecurityUtils> mockedSecurityUtils = mockStatic(SecurityUtils.class)) {
            mockedSecurityUtils.when(() -> SecurityUtils.getEmailFromToken(authentication))
                    .thenReturn(userEmail, adminEmail, adminEmail, adminEmail);

            // when & then
            ResponseEntity<TransferDto> transferResponse = transferController.transferBetweenCards(transferRequest, authentication);
            assertEquals(HttpStatus.OK, transferResponse.getStatusCode());

            ResponseEntity<Page<TransferDto>> myTransfersResponse = transferController.getMyTransfers(0, 10, authentication);
            assertEquals(HttpStatus.OK, myTransfersResponse.getStatusCode());

            ResponseEntity<Page<TransferDto>> cardTransfersResponse = transferController.getCardTransfers(cardId, 0, 10, authentication);
            assertEquals(HttpStatus.OK, cardTransfersResponse.getStatusCode());

            ResponseEntity<Page<TransferDto>> adminCardTransfersResponse = transferController.getCardTransfersForAdmin(cardId, 0, 10, authentication);
            assertEquals(HttpStatus.OK, adminCardTransfersResponse.getStatusCode());

            ResponseEntity<CardTransferStatsDto> cardStatsResponse = transferController.getCardTransferStats(cardId, authentication);
            assertEquals(HttpStatus.OK, cardStatsResponse.getStatusCode());

            ResponseEntity<UserTransferStatsDto> userStatsResponse = transferController.getUserTransferStats(userId, authentication);
            assertEquals(HttpStatus.OK, userStatsResponse.getStatusCode());
        }
    }

    @Test
    @DisplayName("Controller properly delegates to TransferService")
    void controller_ProperlyDelegatesToService() {
        // given
        String userEmail = "user@example.com";
        String adminEmail = "admin@example.com";
        Long cardId = 1L;
        Long userId = 1L;

        when(transferService.transferBetweenCards(transferRequest)).thenReturn(transferDto);
        when(transferService.getUserTransfers(0, 10)).thenReturn(transferPage);
        when(transferService.getCardTransfers(cardId, 0, 10)).thenReturn(transferPage);
        when(transferService.getCardTransfersForAdmin(cardId, 0, 10)).thenReturn(transferPage);
        when(transferService.getCardTransferStats(cardId)).thenReturn(cardTransferStatsDto);
        when(transferService.getUserTransferStats(userId)).thenReturn(userTransferStatsDto);

        try (MockedStatic<SecurityUtils> mockedSecurityUtils = mockStatic(SecurityUtils.class)) {
            mockedSecurityUtils.when(() -> SecurityUtils.getEmailFromToken(authentication))
                    .thenReturn(userEmail, userEmail, userEmail, adminEmail, adminEmail, adminEmail);

            // when
            transferController.transferBetweenCards(transferRequest, authentication);
            transferController.getMyTransfers(0, 10, authentication);
            transferController.getCardTransfers(cardId, 0, 10, authentication);
            transferController.getCardTransfersForAdmin(cardId, 0, 10, authentication);
            transferController.getCardTransferStats(cardId, authentication);
            transferController.getUserTransferStats(userId, authentication);

            // then
            verify(transferService).transferBetweenCards(transferRequest);
            verify(transferService).getUserTransfers(0, 10);
            verify(transferService).getCardTransfers(cardId, 0, 10);
            verify(transferService).getCardTransfersForAdmin(cardId, 0, 10);
            verify(transferService).getCardTransferStats(cardId);
            verify(transferService).getUserTransferStats(userId);

            // Verify no other interactions
            verifyNoMoreInteractions(transferService);
        }
    }

    @Test
    @DisplayName("Controller handles service exceptions gracefully")
    void controller_HandlesServiceExceptionsGracefully() {
        // given
        String userEmail = "user@example.com";
        when(transferService.transferBetweenCards(any(TransferRequest.class)))
                .thenThrow(new RuntimeException("Service error"));

        try (MockedStatic<SecurityUtils> mockedSecurityUtils = mockStatic(SecurityUtils.class)) {
            mockedSecurityUtils.when(() -> SecurityUtils.getEmailFromToken(authentication))
                    .thenReturn(userEmail);

            // when & then
            assertThrows(RuntimeException.class, () -> 
                transferController.transferBetweenCards(transferRequest, authentication));

            verify(transferService).transferBetweenCards(transferRequest);
            mockedSecurityUtils.verify(() -> SecurityUtils.getEmailFromToken(authentication));
        }
    }
}