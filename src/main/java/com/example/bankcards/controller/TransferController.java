package com.example.bankcards.controller;

import com.example.bankcards.dto.transfer.TransferDto;
import com.example.bankcards.dto.transfer.TransferRequest;
import com.example.bankcards.dto.transfer.UserTransferStatsDto;
import com.example.bankcards.service.TransferService;
import com.example.bankcards.util.SecurityUtils;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/transfers")
@RequiredArgsConstructor
public class TransferController {

    private final TransferService transferService;

    /**
     * Transfer between own cards
     * Available only for users (USER)
     */
    @PreAuthorize("hasRole('USER')")
    @PostMapping
    public ResponseEntity<TransferDto> transferBetweenCards(
            @Valid @RequestBody TransferRequest request,
            Authentication authentication) {
        
        String userEmail = SecurityUtils.getEmailFromToken(authentication);
        log.info("Transfer request from user: {}, from card: {}, to card: {}, amount: {}", 
                userEmail, request.getFromCardId(), request.getToCardId(), request.getAmount());

        TransferDto transfer = transferService.transferBetweenCards(request);
        
        log.info("Transfer completed successfully: {}", transfer.getId());
        return ResponseEntity.ok(transfer);
    }

    /**
     * Get user's transfer history
     * Available only for users (USER)
     */
    @PreAuthorize("hasRole('USER')")
    @GetMapping("/my")
    public ResponseEntity<Page<TransferDto>> getMyTransfers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            Authentication authentication) {
        
        String userEmail = SecurityUtils.getEmailFromToken(authentication);
        log.info("Getting transfer history for user: {}, page: {}, size: {}", userEmail, page, size);

        Page<TransferDto> transfers = transferService.getUserTransfers(page, size);
        
        log.info("Retrieved {} transfers for user: {}", transfers.getTotalElements(), userEmail);
        return ResponseEntity.ok(transfers);
    }

    /**
     * Get transfer history for specific card (for user)
     * User can only see history of their own cards
     */
    @PreAuthorize("hasRole('USER')")
    @GetMapping("/card/{cardId}")
    public ResponseEntity<Page<TransferDto>> getCardTransfers(
            @PathVariable Long cardId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            Authentication authentication) {
        
        String userEmail = SecurityUtils.getEmailFromToken(authentication);
        log.info("Getting transfer history for card: {} by user: {}, page: {}, size: {}", 
                cardId, userEmail, page, size);

        Page<TransferDto> transfers = transferService.getCardTransfers(cardId, page, size);
        
        log.info("Retrieved {} transfers for card: {} by user: {}", 
                transfers.getTotalElements(), cardId, userEmail);
        return ResponseEntity.ok(transfers);
    }

    /**
     * Get transfer history for specific card (for admin)
     * Admin can see history of any card
     */
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/admin/card/{cardId}")
    public ResponseEntity<Page<TransferDto>> getCardTransfersForAdmin(
            @PathVariable Long cardId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            Authentication authentication) {
        
        String adminEmail = SecurityUtils.getEmailFromToken(authentication);
        log.info("Admin {} getting transfer history for card: {}, page: {}, size: {}", 
                adminEmail, cardId, page, size);

        Page<TransferDto> transfers = transferService.getCardTransfersForAdmin(cardId, page, size);
        
        log.info("Admin {} retrieved {} transfers for card: {}", 
                adminEmail, transfers.getTotalElements(), cardId);
        return ResponseEntity.ok(transfers);
    }

    /**
     * Get transfer statistics for card (for admin)
     * Shows total income, expense, balance and number of operations
     */
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/admin/card/{cardId}/stats")
    public ResponseEntity<com.example.bankcards.dto.transfer.CardTransferStatsDto> getCardTransferStats(
            @PathVariable Long cardId,
            Authentication authentication) {
        
        String adminEmail = SecurityUtils.getEmailFromToken(authentication);
        log.info("Admin {} getting transfer stats for card: {}", adminEmail, cardId);

        com.example.bankcards.dto.transfer.CardTransferStatsDto stats = transferService.getCardTransferStats(cardId);
        
        log.info("Admin {} retrieved transfer stats for card: {} (income: {}, expense: {})", 
                adminEmail, cardId, stats.getTotalIncome(), stats.getTotalExpense());
        return ResponseEntity.ok(stats);
    }

    /**
     * Get transfer statistics for user by cards (for admin)
     * Shows total income/expense for user and breakdown by each card
     */
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/admin/user/{userId}/stats")
    public ResponseEntity<com.example.bankcards.dto.transfer.UserTransferStatsDto> getUserTransferStats(
            @PathVariable Long userId,
            Authentication authentication) {
        
        String adminEmail = SecurityUtils.getEmailFromToken(authentication);
        log.info("Admin {} getting transfer stats for user: {}", adminEmail, userId);

        UserTransferStatsDto stats = transferService.getUserTransferStats(userId);
        
        log.info("Admin {} retrieved transfer stats for user: {} (total income: {}, total expense: {}, cards: {})", 
                adminEmail, userId, stats.getTotalIncome(), stats.getTotalExpense(), stats.getCardStats().size());
        return ResponseEntity.ok(stats);
    }
}