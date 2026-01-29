package com.example.bankcards.controller;

import com.example.bankcards.dto.transfer.TransferDto;
import com.example.bankcards.dto.transfer.TransferRequest;
import com.example.bankcards.service.TransferService;
import com.example.bankcards.util.SecurityUtils;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/v1/transfers")
@RequiredArgsConstructor
public class TransferController {

    private final TransferService transferService;

    /**
     * Выполнить перевод между своими картами
     * Доступно только для пользователей (USER)
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
     * Получить историю переводов пользователя
     * Доступно только для пользователей (USER)
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
     * Получить историю переводов по конкретной карте (для пользователя)
     * Пользователь может видеть только историю своих карт
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
     * Получить историю переводов по конкретной карте (для админа)
     * Админ может видеть историю любой карты
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
     * Получить статистику переводов по карте (для админа)
     * Показывает общий приход, расход, баланс и количество операций
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
     * Получить статистику переводов по пользователю в разрезе карт (для админа)
     * Показывает общий приход/расход по пользователю и детализацию по каждой карте
     */
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/admin/user/{userId}/stats")
    public ResponseEntity<com.example.bankcards.dto.transfer.UserTransferStatsDto> getUserTransferStats(
            @PathVariable Long userId,
            Authentication authentication) {
        
        String adminEmail = SecurityUtils.getEmailFromToken(authentication);
        log.info("Admin {} getting transfer stats for user: {}", adminEmail, userId);

        com.example.bankcards.dto.transfer.UserTransferStatsDto stats = transferService.getUserTransferStats(userId);
        
        log.info("Admin {} retrieved transfer stats for user: {} (total income: {}, total expense: {}, cards: {})", 
                adminEmail, userId, stats.getTotalIncome(), stats.getTotalExpense(), stats.getCardStats().size());
        return ResponseEntity.ok(stats);
    }
}