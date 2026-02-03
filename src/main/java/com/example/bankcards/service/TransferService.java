package com.example.bankcards.service;

import com.example.bankcards.dto.transfer.CardTransferStatsDto;
import com.example.bankcards.dto.transfer.TransferDto;
import com.example.bankcards.dto.transfer.TransferRequest;
import com.example.bankcards.dto.transfer.UserTransferStatsDto;
import com.example.bankcards.entity.Card;
import com.example.bankcards.entity.Transfer;
import com.example.bankcards.entity.TransferStatus;
import com.example.bankcards.entity.User;
import com.example.bankcards.exception.CardNotFoundException;
import com.example.bankcards.exception.InsufficientFundsException;
import com.example.bankcards.exception.InvalidTransferException;
import com.example.bankcards.exception.UserNotFoundException;
import com.example.bankcards.repository.CardRepository;
import com.example.bankcards.repository.TransferRepository;
import com.example.bankcards.repository.UserRepository;
import com.example.bankcards.util.SecurityUtils;
import com.example.bankcards.util.TransferMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class TransferService {
    
    private final TransferRepository transferRepository;
    private final CardRepository cardRepository;
    private final UserRepository userRepository;
    private final TransferMapper transferMapper;
    private final com.example.bankcards.service.CardService cardService;

    /**
     * Creates a PageRequest for transfer queries with descending order by transfer date.
     */
    private PageRequest createTransferPageRequest(int page, int size) {
        return PageRequest.of(page, size, Sort.by("transferDate").descending());
    }

    /**
     * Maps a Transfer entity to DTO with masked card numbers.
     */
    private TransferDto mapTransferToDto(Transfer transfer) {
        TransferDto dto = transferMapper.toDto(transfer);
        // Set masked numbers manually since TransferMapper can't use CardService
        dto.setFromCardMaskedNumber(cardService.getMaskedNumberForCard(transfer.getFromCard()));
        dto.setToCardMaskedNumber(cardService.getMaskedNumberForCard(transfer.getToCard()));
        return dto;
    }

    /**
     * Execute transfer between user cards
     */
    @Transactional
    @PreAuthorize("hasRole('USER')")
    public TransferDto transferBetweenCards(TransferRequest request) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String userEmail = SecurityUtils.getEmailFromToken(auth);
        
        log.info("Processing transfer request from user: {}", userEmail);

        // Get and validate sender card
        Card fromCard = validateCardAccess(request.getFromCardId(), userEmail, "sender");
        
        // Get recipient card - for security, don't reveal if it exists or not
        Card toCard = cardRepository.findById(request.getToCardId()).orElse(null);
        if (toCard == null) {
            throw new com.example.bankcards.exception.AccessDeniedException("No access to recipient card");
        }

        // Check CVV if provided (optional additional security)
        if (request.getCvv() != null && !request.getCvv().isEmpty()) {
            if (!cardService.verifyCvvForCard(fromCard, request.getCvv())) {
                throw new InvalidTransferException("Invalid CVV code");
            }
        }

        // Check transfer possibility
        validateTransfer(fromCard, toCard, request.getAmount());

        // Execute transfer
        if (!fromCard.debit(request.getAmount())) {
            throw new InsufficientFundsException("Failed to debit amount from card");
        }
        toCard.credit(request.getAmount());

        // Save cards
        cardRepository.save(fromCard);
        cardRepository.save(toCard);

        // Create transfer record
        Transfer transfer = new Transfer();
        transfer.setFromCard(fromCard);
        transfer.setToCard(toCard);
        transfer.setAmount(request.getAmount());
        transfer.setDescription(request.getDescription());
        transfer.setStatus(TransferStatus.COMPLETED);

        Transfer savedTransfer = transferRepository.save(transfer);

        String fromMasked = cardService.getMaskedNumberForCard(fromCard);
        String toMasked = cardService.getMaskedNumberForCard(toCard);
        log.info("Transfer completed successfully: {} -> {}, amount: {}", 
                fromMasked, toMasked, request.getAmount());

        return mapTransferToDto(savedTransfer);
    }

    /**
     * Get user transfer history
     */
    @Transactional(readOnly = true)
    @PreAuthorize("hasRole('USER')")
    public Page<TransferDto> getUserTransfers(int page, int size) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String userEmail = SecurityUtils.getEmailFromToken(auth);

        PageRequest pageRequest = createTransferPageRequest(page, size);
        Page<Transfer> transfers = transferRepository.findByUserEmail(userEmail, pageRequest);
        
        return transfers.map(this::mapTransferToDto);
    }

    /**
     * Get transfer history for specific card (for user)
     */
    @Transactional(readOnly = true)
    @PreAuthorize("hasRole('USER')")
    public Page<TransferDto> getCardTransfers(Long cardId, int page, int size) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String userEmail = SecurityUtils.getEmailFromToken(auth);

        // Check that card belongs to user
        Card card = cardRepository.findById(cardId)
                .orElseThrow(() -> new CardNotFoundException("Card not found"));
        
        if (!card.getUser().getEmail().equals(userEmail)) {
            throw new com.example.bankcards.exception.AccessDeniedException("No access to transfer history of this card");
        }

        PageRequest pageRequest = createTransferPageRequest(page, size);
        Page<Transfer> transfers = transferRepository.findByCardId(cardId, pageRequest);
        
        return transfers.map(this::mapTransferToDto);
    }

    /**
     * Get transfer history for specific card (for admin)
     */
    @Transactional(readOnly = true)
    @PreAuthorize("hasRole('ADMIN')")
    public Page<TransferDto> getCardTransfersForAdmin(Long cardId, int page, int size) {
        // Check that card exists
        if (!cardRepository.existsById(cardId)) {
            throw new CardNotFoundException("Card not found");
        }

        PageRequest pageRequest = createTransferPageRequest(page, size);
        Page<Transfer> transfers = transferRepository.findByCardId(cardId, pageRequest);
        
        return transfers.map(this::mapTransferToDto);
    }

    /**
     * Get transfer statistics by card (for admin)
     */
    @Transactional(readOnly = true)
    @PreAuthorize("hasRole('ADMIN')")
    public CardTransferStatsDto getCardTransferStats(Long cardId) {
        // Check that card exists
        Card card = cardRepository.findById(cardId)
                .orElseThrow(() -> new CardNotFoundException("Card not found"));

        BigDecimal totalIncome = transferRepository.getTotalIncomeByCardId(cardId);
        BigDecimal totalExpense = transferRepository.getTotalExpenseByCardId(cardId);
        Long incomeCount = transferRepository.getIncomeTransfersCountByCardId(cardId);
        Long expenseCount = transferRepository.getExpenseTransfersCountByCardId(cardId);

        String maskedNumber = cardService.getMaskedNumberForCard(card);
        return new CardTransferStatsDto(
                cardId,
                maskedNumber,
                totalIncome,
                totalExpense,
                card.getBalance(),
                incomeCount,
                expenseCount
        );
    }

    /**
     * Get transfer statistics by user broken down by cards (for admin)
     */
    @Transactional(readOnly = true)
    @PreAuthorize("hasRole('ADMIN')")
    public UserTransferStatsDto getUserTransferStats(Long userId) {
        // Check that user exists and get user
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("User not found"));
        
        // Get general statistics by user
        BigDecimal totalUserIncome = transferRepository.getTotalIncomeByUserId(userId);
        BigDecimal totalUserExpense = transferRepository.getTotalExpenseByUserId(userId);
        
        // Get statistics for each user card
        List<CardTransferStatsDto> cardStats = user.getCards().stream()
                .map(card -> {
                    BigDecimal cardIncome = transferRepository.getTotalIncomeByCardId(card.getId());
                    BigDecimal cardExpense = transferRepository.getTotalExpenseByCardId(card.getId());
                    Long incomeCount = transferRepository.getIncomeTransfersCountByCardId(card.getId());
                    Long expenseCount = transferRepository.getExpenseTransfersCountByCardId(card.getId());
                    
                    String maskedNumber = cardService.getMaskedNumberForCard(card);
                    return new CardTransferStatsDto(
                            card.getId(),
                            maskedNumber,
                            cardIncome,
                            cardExpense,
                            card.getBalance(),
                            incomeCount,
                            expenseCount
                    );
                })
                .toList();

        // Total balance across all cards
        BigDecimal totalBalance = user.getCards().stream()
                .map(Card::getBalance)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return new UserTransferStatsDto(
                userId,
                user.getEmail(),
                user.getFullName(),
                totalUserIncome,
                totalUserExpense,
                totalBalance,
                cardStats
        );
    }

    private Card validateCardAccess(Long cardId, String userEmail, String cardType) {
        Card card = cardRepository.findById(cardId).orElse(null);
        
        // Always return access denied - don't reveal if card exists or not
        if (card == null || !card.getUser().getEmail().equals(userEmail)) {
            throw new com.example.bankcards.exception.AccessDeniedException("No access to " + cardType + " card");
        }
        
        return card;
    }

    private void validateTransfer(Card fromCard, Card toCard, BigDecimal amount) {
        // Check that cards are different
        if (fromCard.getId().equals(toCard.getId())) {
            throw new InvalidTransferException("Cannot transfer to the same card");
        }

        // Check card status
        if (!fromCard.isActive()) {
            throw new InvalidTransferException("Sender card is inactive");
        }
        if (!toCard.isActive()) {
            throw new InvalidTransferException("Recipient card is inactive");
        }

        // Check balance
        if (!fromCard.canDebit(amount)) {
            throw new InsufficientFundsException("Insufficient funds on card");
        }
    }
}