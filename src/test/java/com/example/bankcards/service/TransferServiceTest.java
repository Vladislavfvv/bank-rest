package com.example.bankcards.service;

import com.example.bankcards.dto.transfer.CardTransferStatsDto;
import com.example.bankcards.dto.transfer.TransferDto;
import com.example.bankcards.dto.transfer.TransferRequest;
import com.example.bankcards.dto.transfer.UserTransferStatsDto;
import com.example.bankcards.entity.Card;
import com.example.bankcards.entity.Role;
import com.example.bankcards.entity.Status;
import com.example.bankcards.entity.Transfer;
import com.example.bankcards.entity.TransferStatus;
import com.example.bankcards.entity.User;
import com.example.bankcards.exception.AccessDeniedException;
import com.example.bankcards.exception.CardNotFoundException;
import com.example.bankcards.exception.InsufficientFundsException;
import com.example.bankcards.exception.InvalidTransferException;
import com.example.bankcards.exception.UserNotFoundException;
import com.example.bankcards.repository.CardRepository;
import com.example.bankcards.repository.TransferRepository;
import com.example.bankcards.repository.UserRepository;
import com.example.bankcards.util.SecurityUtils;
import com.example.bankcards.util.TransferMapper;
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
import org.springframework.data.domain.Sort;
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
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class TransferServiceTest {

    @InjectMocks
    private TransferService transferService;

    @Mock
    private TransferRepository transferRepository;

    @Mock
    private CardRepository cardRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private TransferMapper transferMapper;

    @Mock
    private CardService cardService;

    @Mock
    private SecurityContext securityContext;

    @Mock
    private Authentication authentication;

    private MockedStatic<SecurityUtils> securityUtilsMock;

    private User user;
    private Card fromCard;
    private Card toCard;
    private Transfer transfer;
    private TransferRequest transferRequest;
    private TransferDto transferDto;

    @BeforeEach
    void setUp() {
        // Mock SecurityUtils static method
        securityUtilsMock = mockStatic(SecurityUtils.class);
        // Default mock - return email from any authentication (lenient to avoid UnnecessaryStubbing)
        securityUtilsMock.when(() -> SecurityUtils.getEmailFromToken(any(Authentication.class)))
                .thenReturn("test@example.com");

        // Setup SecurityContext mock
        when(securityContext.getAuthentication()).thenReturn(authentication);
        SecurityContextHolder.setContext(securityContext);
        
        user = new User();
        user.setId(1L);
        user.setEmail("test@example.com");
        user.setFirstName("Ivan");
        user.setLastName("Ivanov");
        user.setBirthDate(LocalDate.of(1990, 1, 1));
        user.setRole(Role.ROLE_USER);
        user.setCreatedAt(LocalDateTime.now());
        user.setIsActive(true);

        fromCard = new Card();
        fromCard.setId(1L);
        // Card number and CVV are stored encrypted in DB
        fromCard.setNumber("encrypted_1234567890123456");
        fromCard.setHolder("Ivan Ivanov");
        fromCard.setExpirationDate(LocalDate.of(2030, 12, 31));
        fromCard.setCvv("encrypted_cvv_123");
        fromCard.setBalance(BigDecimal.valueOf(1000.00));
        fromCard.setStatus(Status.ACTIVE);
        fromCard.setUser(user);

        toCard = new Card();
        toCard.setId(2L);
        toCard.setNumber("encrypted_6543210987654321");
        toCard.setHolder("Ivan Ivanov");
        toCard.setExpirationDate(LocalDate.of(2030, 12, 31));
        toCard.setCvv("encrypted_cvv_456");
        toCard.setBalance(BigDecimal.valueOf(500.00));
        toCard.setStatus(Status.ACTIVE);
        toCard.setUser(user);
        
        // Mock CardService methods for encryption
        when(cardService.verifyCvvForCard(fromCard, "123")).thenReturn(true);
        when(cardService.verifyCvvForCard(fromCard, "999")).thenReturn(false);
        when(cardService.getMaskedNumberForCard(fromCard)).thenReturn("**** **** **** 3456");
        when(cardService.getMaskedNumberForCard(toCard)).thenReturn("**** **** **** 4321");

        transfer = new Transfer();
        transfer.setId(1L);
        transfer.setFromCard(fromCard);
        transfer.setToCard(toCard);
        transfer.setAmount(BigDecimal.valueOf(100.00));
        transfer.setDescription("Test transfer");
        transfer.setStatus(TransferStatus.COMPLETED);
        transfer.setTransferDate(LocalDateTime.now());

        transferRequest = new TransferRequest();
        transferRequest.setFromCardId(1L);
        transferRequest.setToCardId(2L);
        transferRequest.setAmount(BigDecimal.valueOf(100.00));
        transferRequest.setDescription("Test transfer");
        transferRequest.setCvv("123");

        transferDto = new TransferDto();
        transferDto.setId(1L);
        transferDto.setFromCardId(1L);
        transferDto.setFromCardMaskedNumber("**** **** **** 3456");
        transferDto.setToCardId(2L);
        transferDto.setToCardMaskedNumber("**** **** **** 4321");
        transferDto.setAmount(BigDecimal.valueOf(100.00));
        transferDto.setDescription("Test transfer");
        transferDto.setStatus(TransferStatus.COMPLETED);
        transferDto.setTransferDate(LocalDateTime.now());
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

    // ================= transferBetweenCards Tests =================

    @DisplayName("transferBetweenCards - Valid transfer - Completes successfully")
    @Test
    void transferBetweenCards_ValidTransfer_CompletesSuccessfully() {
        // given
        // No need for mockUserAuthentication since we mock SecurityUtils directly
        
        when(cardRepository.findById(1L)).thenReturn(Optional.of(fromCard));
        when(cardRepository.findById(2L)).thenReturn(Optional.of(toCard));
        when(cardRepository.save(fromCard)).thenReturn(fromCard);
        when(cardRepository.save(toCard)).thenReturn(toCard);
        when(transferRepository.save(any(Transfer.class))).thenReturn(transfer);
        when(transferMapper.toDto(transfer)).thenReturn(transferDto);

        // when
        TransferDto result = transferService.transferBetweenCards(transferRequest);

        // then
        assertNotNull(result);
        assertEquals(transferDto.getId(), result.getId());
        assertEquals(transferDto.getAmount(), result.getAmount());
        assertEquals(transferDto.getDescription(), result.getDescription());
        assertEquals("**** **** **** 3456", result.getFromCardMaskedNumber());
        assertEquals("**** **** **** 4321", result.getToCardMaskedNumber());

        verify(cardRepository).findById(1L);
        verify(cardRepository).findById(2L);
        verify(cardService).verifyCvvForCard(fromCard, "123");
        verify(cardService, times(2)).getMaskedNumberForCard(fromCard); // Called for logging and DTO mapping
        verify(cardService, times(2)).getMaskedNumberForCard(toCard);   // Called for logging and DTO mapping
        verify(cardRepository).save(fromCard);
        verify(cardRepository).save(toCard);
        verify(transferRepository).save(any(Transfer.class));
        verify(transferMapper).toDto(transfer);
    }

    @DisplayName("transferBetweenCards - From card not found - Throws AccessDeniedException")
    @Test
    void transferBetweenCards_FromCardNotFound_ThrowsAccessDeniedException() {
        // given
        mockUserAuthentication("test@example.com", "USER");
        when(cardRepository.findById(1L)).thenReturn(Optional.empty());

        // when & then
        AccessDeniedException exception = assertThrows(
                AccessDeniedException.class,
                () -> transferService.transferBetweenCards(transferRequest)
        );

        assertEquals("No access to sender card", exception.getMessage());
        verify(cardRepository).findById(1L);
        verify(cardRepository, never()).findById(2L);
        verify(transferRepository, never()).save(any(Transfer.class));
    }

    @DisplayName("transferBetweenCards - To card not found - Throws AccessDeniedException")
    @Test
    void transferBetweenCards_ToCardNotFound_ThrowsAccessDeniedException() {
        // given
        mockUserAuthentication("test@example.com", "USER");
        when(cardRepository.findById(1L)).thenReturn(Optional.of(fromCard));
        when(cardRepository.findById(2L)).thenReturn(Optional.empty());

        // when & then
        AccessDeniedException exception = assertThrows(
                AccessDeniedException.class,
                () -> transferService.transferBetweenCards(transferRequest)
        );

        assertEquals("No access to recipient card", exception.getMessage());
        verify(cardRepository).findById(1L);
        verify(cardRepository).findById(2L);
        verify(transferRepository, never()).save(any(Transfer.class));
    }

    @DisplayName("transferBetweenCards - Invalid CVV - Throws InvalidTransferException")
    @Test
    void transferBetweenCards_InvalidCvv_ThrowsInvalidTransferException() {
        // given
        mockUserAuthentication("test@example.com", "USER");
        transferRequest.setCvv("999"); // Wrong CVV
        
        when(cardRepository.findById(1L)).thenReturn(Optional.of(fromCard));
        when(cardRepository.findById(2L)).thenReturn(Optional.of(toCard));
        when(cardService.verifyCvvForCard(fromCard, "999")).thenReturn(false);

        // when & then
        InvalidTransferException exception = assertThrows(
                InvalidTransferException.class,
                () -> transferService.transferBetweenCards(transferRequest)
        );

        assertEquals("Invalid CVV code", exception.getMessage());
        verify(cardRepository).findById(1L);
        verify(cardRepository).findById(2L);
        verify(cardService).verifyCvvForCard(fromCard, "999");
        verify(transferRepository, never()).save(any(Transfer.class));
    }

    @DisplayName("transferBetweenCards - Same card transfer - Throws InvalidTransferException")
    @Test
    void transferBetweenCards_SameCardTransfer_ThrowsInvalidTransferException() {
        // given
        mockUserAuthentication("test@example.com", "USER");
        transferRequest.setToCardId(1L); // Same as fromCardId
        
        when(cardRepository.findById(1L)).thenReturn(Optional.of(fromCard));

        // when & then
        InvalidTransferException exception = assertThrows(
                InvalidTransferException.class,
                () -> transferService.transferBetweenCards(transferRequest)
        );

        assertEquals("Cannot transfer to the same card", exception.getMessage());
        verify(cardRepository, times(2)).findById(1L);
        verify(transferRepository, never()).save(any(Transfer.class));
    }

    @DisplayName("transferBetweenCards - Insufficient funds - Throws InsufficientFundsException")
    @Test
    void transferBetweenCards_InsufficientFunds_ThrowsInsufficientFundsException() {
        // given
        mockUserAuthentication("test@example.com", "USER");
        transferRequest.setAmount(BigDecimal.valueOf(2000.00)); // More than balance
        
        when(cardRepository.findById(1L)).thenReturn(Optional.of(fromCard));
        when(cardRepository.findById(2L)).thenReturn(Optional.of(toCard));

        // when & then
        InsufficientFundsException exception = assertThrows(
                InsufficientFundsException.class,
                () -> transferService.transferBetweenCards(transferRequest)
        );

        assertEquals("Insufficient funds on card", exception.getMessage());
        verify(cardRepository).findById(1L);
        verify(cardRepository).findById(2L);
        verify(transferRepository, never()).save(any(Transfer.class));
    }

    @DisplayName("transferBetweenCards - From card blocked - Throws InvalidTransferException")
    @Test
    void transferBetweenCards_FromCardBlocked_ThrowsInvalidTransferException() {
        // given
        mockUserAuthentication("test@example.com", "USER");
        fromCard.setStatus(Status.BLOCKED);
        
        when(cardRepository.findById(1L)).thenReturn(Optional.of(fromCard));
        when(cardRepository.findById(2L)).thenReturn(Optional.of(toCard));

        // when & then
        InvalidTransferException exception = assertThrows(
                InvalidTransferException.class,
                () -> transferService.transferBetweenCards(transferRequest)
        );

        assertEquals("Sender card is inactive", exception.getMessage());
        verify(cardRepository).findById(1L);
        verify(cardRepository).findById(2L);
        verify(transferRepository, never()).save(any(Transfer.class));
    }

    @DisplayName("transferBetweenCards - To card blocked - Throws InvalidTransferException")
    @Test
    void transferBetweenCards_ToCardBlocked_ThrowsInvalidTransferException() {
        // given
        mockUserAuthentication("test@example.com", "USER");
        toCard.setStatus(Status.BLOCKED);
        
        when(cardRepository.findById(1L)).thenReturn(Optional.of(fromCard));
        when(cardRepository.findById(2L)).thenReturn(Optional.of(toCard));

        // when & then
        InvalidTransferException exception = assertThrows(
                InvalidTransferException.class,
                () -> transferService.transferBetweenCards(transferRequest)
        );

        assertEquals("Recipient card is inactive", exception.getMessage());
        verify(cardRepository).findById(1L);
        verify(cardRepository).findById(2L);
        verify(transferRepository, never()).save(any(Transfer.class));
    }

    @DisplayName("transferBetweenCards - Access denied to from card - Throws AccessDeniedException")
    @Test
    void transferBetweenCards_AccessDeniedToFromCard_ThrowsAccessDeniedException() {
        // given
        mockUserAuthentication("other@example.com", "USER");
        // Override the default mock for this test
        securityUtilsMock.when(() -> SecurityUtils.getEmailFromToken(any(Authentication.class)))
                .thenReturn("other@example.com");
        
        when(cardRepository.findById(1L)).thenReturn(Optional.of(fromCard));
        when(cardRepository.findById(2L)).thenReturn(Optional.of(toCard));

        // when & then
        AccessDeniedException exception = assertThrows(
                AccessDeniedException.class,
                () -> transferService.transferBetweenCards(transferRequest)
        );

        assertEquals("No access to sender card", exception.getMessage());
        verify(cardRepository).findById(1L);
        verify(cardRepository, never()).findById(2L);
        verify(transferRepository, never()).save(any(Transfer.class));
    }

    // ================= getUserTransfers Tests =================

    @DisplayName("getUserTransfers - Valid request - Returns paginated transfers")
    @Test
    void getUserTransfers_ValidRequest_ReturnsPaginatedTransfers() {
        // given
        mockUserAuthentication("test@example.com", "USER");
        int page = 0;
        int size = 10;
        List<Transfer> transfers = List.of(transfer);
        Page<Transfer> transferPage = new PageImpl<>(transfers, 
                PageRequest.of(page, size, Sort.by("transferDate").descending()), 1);

        when(transferRepository.findByUserEmail("test@example.com", 
                PageRequest.of(page, size, Sort.by("transferDate").descending())))
                .thenReturn(transferPage);
        when(transferMapper.toDto(transfer)).thenReturn(transferDto);

        // when
        Page<TransferDto> result = transferService.getUserTransfers(page, size);

        // then
        assertNotNull(result);
        assertEquals(1, result.getContent().size());
        TransferDto resultDto = result.getContent().get(0);
        assertEquals(transferDto.getId(), resultDto.getId());
        assertEquals("**** **** **** 3456", resultDto.getFromCardMaskedNumber());
        assertEquals("**** **** **** 4321", resultDto.getToCardMaskedNumber());

        verify(transferRepository).findByUserEmail("test@example.com", 
                PageRequest.of(page, size, Sort.by("transferDate").descending()));
        verify(transferMapper).toDto(transfer);
        verify(cardService).getMaskedNumberForCard(fromCard);
        verify(cardService).getMaskedNumberForCard(toCard);
    }

    // ================= getCardTransfers Tests =================

    @DisplayName("getCardTransfers - Valid request - Returns card transfers")
    @Test
    void getCardTransfers_ValidRequest_ReturnsCardTransfers() {
        // given
        mockUserAuthentication("test@example.com", "USER");
        Long cardId = 1L;
        int page = 0;
        int size = 10;
        List<Transfer> transfers = List.of(transfer);
        Page<Transfer> transferPage = new PageImpl<>(transfers, 
                PageRequest.of(page, size, Sort.by("transferDate").descending()), 1);

        when(cardRepository.findById(cardId)).thenReturn(Optional.of(fromCard));
        when(transferRepository.findByCardId(cardId, 
                PageRequest.of(page, size, Sort.by("transferDate").descending())))
                .thenReturn(transferPage);
        when(transferMapper.toDto(transfer)).thenReturn(transferDto);

        // when
        Page<TransferDto> result = transferService.getCardTransfers(cardId, page, size);

        // then
        assertNotNull(result);
        assertEquals(1, result.getContent().size());
        TransferDto resultDto = result.getContent().get(0);
        assertEquals(transferDto.getId(), resultDto.getId());
        assertEquals("**** **** **** 3456", resultDto.getFromCardMaskedNumber());
        assertEquals("**** **** **** 4321", resultDto.getToCardMaskedNumber());

        verify(cardRepository).findById(cardId);
        verify(transferRepository).findByCardId(cardId, 
                PageRequest.of(page, size, Sort.by("transferDate").descending()));
        verify(transferMapper).toDto(transfer);
        verify(cardService).getMaskedNumberForCard(fromCard);
        verify(cardService).getMaskedNumberForCard(toCard);
    }

    @DisplayName("getCardTransfers - Card not found - Throws CardNotFoundException")
    @Test
    void getCardTransfers_CardNotFound_ThrowsCardNotFoundException() {
        // given
        mockUserAuthentication("test@example.com", "USER");
        Long cardId = 999L;
        int page = 0;
        int size = 10;

        when(cardRepository.findById(cardId)).thenReturn(Optional.empty());

        // when & then
        CardNotFoundException exception = assertThrows(
                CardNotFoundException.class,
                () -> transferService.getCardTransfers(cardId, page, size)
        );

        assertEquals("Card not found", exception.getMessage());
        verify(cardRepository).findById(cardId);
        verify(transferRepository, never()).findByCardId(anyLong(), any());
    }

    @DisplayName("getCardTransfers - Access denied - Throws AccessDeniedException")
    @Test
    void getCardTransfers_AccessDenied_ThrowsAccessDeniedException() {
        // given
        mockUserAuthentication("other@example.com", "USER");
        // Override the default mock for this test
        securityUtilsMock.when(() -> SecurityUtils.getEmailFromToken(any(Authentication.class)))
                .thenReturn("other@example.com");
        Long cardId = 1L;
        int page = 0;
        int size = 10;

        when(cardRepository.findById(cardId)).thenReturn(Optional.of(fromCard));

        // when & then
        AccessDeniedException exception = assertThrows(
                AccessDeniedException.class,
                () -> transferService.getCardTransfers(cardId, page, size)
        );

        assertEquals("No access to transfer history of this card", exception.getMessage());
        verify(cardRepository).findById(cardId);
        verify(transferRepository, never()).findByCardId(anyLong(), any());
    }

    // ================= getCardTransfersForAdmin Tests =================

    @DisplayName("getCardTransfersForAdmin - Valid request - Returns card transfers")
    @Test
    void getCardTransfersForAdmin_ValidRequest_ReturnsCardTransfers() {
        // given
        Long cardId = 1L;
        int page = 0;
        int size = 10;
        List<Transfer> transfers = List.of(transfer);
        Page<Transfer> transferPage = new PageImpl<>(transfers, 
                PageRequest.of(page, size, Sort.by("transferDate").descending()), 1);

        when(cardRepository.existsById(cardId)).thenReturn(true);
        when(transferRepository.findByCardId(cardId, 
                PageRequest.of(page, size, Sort.by("transferDate").descending())))
                .thenReturn(transferPage);
        when(transferMapper.toDto(transfer)).thenReturn(transferDto);

        // when
        Page<TransferDto> result = transferService.getCardTransfersForAdmin(cardId, page, size);

        // then
        assertNotNull(result);
        assertEquals(1, result.getContent().size());
        TransferDto resultDto = result.getContent().get(0);
        assertEquals(transferDto.getId(), resultDto.getId());
        assertEquals("**** **** **** 3456", resultDto.getFromCardMaskedNumber());
        assertEquals("**** **** **** 4321", resultDto.getToCardMaskedNumber());

        verify(cardRepository).existsById(cardId);
        verify(transferRepository).findByCardId(cardId, 
                PageRequest.of(page, size, Sort.by("transferDate").descending()));
        verify(transferMapper).toDto(transfer);
        verify(cardService).getMaskedNumberForCard(fromCard);
        verify(cardService).getMaskedNumberForCard(toCard);
    }

    @DisplayName("getCardTransfersForAdmin - Card not found - Throws CardNotFoundException")
    @Test
    void getCardTransfersForAdmin_CardNotFound_ThrowsCardNotFoundException() {
        // given
        Long cardId = 999L;
        int page = 0;
        int size = 10;

        when(cardRepository.existsById(cardId)).thenReturn(false);

        // when & then
        CardNotFoundException exception = assertThrows(
                CardNotFoundException.class,
                () -> transferService.getCardTransfersForAdmin(cardId, page, size)
        );

        assertEquals("Card not found", exception.getMessage());
        verify(cardRepository).existsById(cardId);
        verify(transferRepository, never()).findByCardId(anyLong(), any());
    }

    // ================= getCardTransferStats Tests =================

    @DisplayName("getCardTransferStats - Valid request - Returns card stats")
    @Test
    void getCardTransferStats_ValidRequest_ReturnsCardStats() {
        // given
        Long cardId = 1L;
        BigDecimal totalIncome = BigDecimal.valueOf(500.00);
        BigDecimal totalExpense = BigDecimal.valueOf(300.00);
        Long incomeCount = 5L;
        Long expenseCount = 3L;

        when(cardRepository.findById(cardId)).thenReturn(Optional.of(fromCard));
        when(transferRepository.getTotalIncomeByCardId(cardId)).thenReturn(totalIncome);
        when(transferRepository.getTotalExpenseByCardId(cardId)).thenReturn(totalExpense);
        when(transferRepository.getIncomeTransfersCountByCardId(cardId)).thenReturn(incomeCount);
        when(transferRepository.getExpenseTransfersCountByCardId(cardId)).thenReturn(expenseCount);
        when(cardService.getMaskedNumberForCard(fromCard)).thenReturn("**** **** **** 3456");

        // when
        CardTransferStatsDto result = transferService.getCardTransferStats(cardId);

        // then
        assertNotNull(result);
        assertEquals(cardId, result.getCardId());
        assertEquals("**** **** **** 3456", result.getCardMaskedNumber());
        assertEquals(totalIncome, result.getTotalIncome());
        assertEquals(totalExpense, result.getTotalExpense());
        assertEquals(fromCard.getBalance(), result.getBalance());
        assertEquals(incomeCount, result.getIncomeTransfersCount());
        assertEquals(expenseCount, result.getExpenseTransfersCount());

        verify(cardRepository).findById(cardId);
        verify(cardService).getMaskedNumberForCard(fromCard);
        verify(transferRepository).getTotalIncomeByCardId(cardId);
        verify(transferRepository).getTotalExpenseByCardId(cardId);
        verify(transferRepository).getIncomeTransfersCountByCardId(cardId);
        verify(transferRepository).getExpenseTransfersCountByCardId(cardId);
    }

    @DisplayName("getCardTransferStats - Card not found - Throws CardNotFoundException")
    @Test
    void getCardTransferStats_CardNotFound_ThrowsCardNotFoundException() {
        // given
        Long cardId = 999L;
        when(cardRepository.findById(cardId)).thenReturn(Optional.empty());

        // when & then
        CardNotFoundException exception = assertThrows(
                CardNotFoundException.class,
                () -> transferService.getCardTransferStats(cardId)
        );

        assertEquals("Card not found", exception.getMessage());
        verify(cardRepository).findById(cardId);
        verify(transferRepository, never()).getTotalIncomeByCardId(anyLong());
    }

    // ================= getUserTransferStats Tests =================

    @DisplayName("getUserTransferStats - Valid request - Returns user stats")
    @Test
    void getUserTransferStats_ValidRequest_ReturnsUserStats() {
        // given
        Long userId = 1L;
        user.setCards(List.of(fromCard, toCard));
        BigDecimal totalUserIncome = BigDecimal.valueOf(800.00);
        BigDecimal totalUserExpense = BigDecimal.valueOf(600.00);
        BigDecimal cardIncome = BigDecimal.valueOf(400.00);
        BigDecimal cardExpense = BigDecimal.valueOf(300.00);
        Long incomeCount = 4L;
        Long expenseCount = 3L;

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(transferRepository.getTotalIncomeByUserId(userId)).thenReturn(totalUserIncome);
        when(transferRepository.getTotalExpenseByUserId(userId)).thenReturn(totalUserExpense);
        when(transferRepository.getTotalIncomeByCardId(anyLong())).thenReturn(cardIncome);
        when(transferRepository.getTotalExpenseByCardId(anyLong())).thenReturn(cardExpense);
        when(transferRepository.getIncomeTransfersCountByCardId(anyLong())).thenReturn(incomeCount);
        when(transferRepository.getExpenseTransfersCountByCardId(anyLong())).thenReturn(expenseCount);
        when(cardService.getMaskedNumberForCard(fromCard)).thenReturn("**** **** **** 3456");
        when(cardService.getMaskedNumberForCard(toCard)).thenReturn("**** **** **** 4321");

        // when
        UserTransferStatsDto result = transferService.getUserTransferStats(userId);

        // then
        assertNotNull(result);
        assertEquals(userId, result.getUserId());
        assertEquals(user.getEmail(), result.getUserEmail());
        assertEquals(user.getFullName(), result.getUserFullName());
        assertEquals(totalUserIncome, result.getTotalIncome());
        assertEquals(totalUserExpense, result.getTotalExpense());
        assertEquals(2, result.getCardStats().size());
        // Verify masked numbers in card stats
        assertEquals("**** **** **** 3456", result.getCardStats().get(0).getCardMaskedNumber());
        assertEquals("**** **** **** 4321", result.getCardStats().get(1).getCardMaskedNumber());

        verify(userRepository).findById(userId);
        verify(transferRepository).getTotalIncomeByUserId(userId);
        verify(transferRepository).getTotalExpenseByUserId(userId);
        verify(transferRepository, times(2)).getTotalIncomeByCardId(anyLong());
        verify(transferRepository, times(2)).getTotalExpenseByCardId(anyLong());
        verify(cardService, times(2)).getMaskedNumberForCard(any(Card.class));
    }

    @DisplayName("getUserTransferStats - User not found - Throws UserNotFoundException")
    @Test
    void getUserTransferStats_UserNotFound_ThrowsUserNotFoundException() {
        // given
        Long userId = 999L;
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        // when & then
        UserNotFoundException exception = assertThrows(
                UserNotFoundException.class,
                () -> transferService.getUserTransferStats(userId)
        );

        assertEquals("User not found", exception.getMessage());
        verify(userRepository).findById(userId);
        verify(transferRepository, never()).getTotalIncomeByUserId(anyLong());
    }
}