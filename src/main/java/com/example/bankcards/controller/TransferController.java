package com.example.bankcards.controller;

import com.example.bankcards.dto.transfer.TransferDto;
import com.example.bankcards.dto.transfer.TransferRequest;
import com.example.bankcards.dto.transfer.UserTransferStatsDto;
import com.example.bankcards.service.TransferService;
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
@Tag(name = "Transfers", description = "Управление переводами между картами")
@SecurityRequirement(name = "bearerAuth")
public class TransferController {

    private final TransferService transferService;

    /**
     * Transfer between own cards
     * Available only for users (USER)
     */
    @Operation(
        summary = "Перевод между картами",
        description = "Выполнение перевода денежных средств между картами. Пользователи могут переводить с собственных карт на любые карты в системе."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Перевод выполнен успешно",
            content = @Content(schema = @Schema(implementation = TransferDto.class))
        ),
        @ApiResponse(
            responseCode = "400",
            description = "Некорректные данные перевода"
        ),
        @ApiResponse(
            responseCode = "403",
            description = "Нет доступа к карте отправителя"
        ),
        @ApiResponse(
            responseCode = "422",
            description = "Недостаточно средств на карте"
        )
    })
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
    @Operation(
        summary = "Получить историю переводов пользователя",
        description = "Возвращает постраничный список всех переводов текущего пользователя (как входящих, так и исходящих)."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "История переводов получена успешно"
        ),
        @ApiResponse(
            responseCode = "401",
            description = "Не авторизован"
        ),
        @ApiResponse(
            responseCode = "403",
            description = "Доступ запрещен - требуется роль USER"
        )
    })
    @PreAuthorize("hasRole('USER')")
    @GetMapping("/my")
    public ResponseEntity<Page<TransferDto>> getMyTransfers(
            @Parameter(description = "Номер страницы") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Размер страницы") @RequestParam(defaultValue = "10") int size,
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
    @Operation(
        summary = "Получить историю переводов карты",
        description = "Возвращает постраничный список всех переводов указанной карты. Пользователи могут получать историю только своих карт."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "История переводов карты получена успешно"
        ),
        @ApiResponse(
            responseCode = "403",
            description = "Нет доступа к данной карте"
        ),
        @ApiResponse(
            responseCode = "404",
            description = "Карта не найдена"
        )
    })
    @PreAuthorize("hasRole('USER')")
    @GetMapping("/card/{cardId}")
    public ResponseEntity<Page<TransferDto>> getCardTransfers(
            @Parameter(description = "ID карты") @PathVariable Long cardId,
            @Parameter(description = "Номер страницы") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Размер страницы") @RequestParam(defaultValue = "10") int size,
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
    @Operation(
        summary = "Получить историю переводов карты (для администратора)",
        description = "Возвращает постраничный список всех переводов указанной карты. Доступно только администраторам."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "История переводов карты получена успешно"
        ),
        @ApiResponse(
            responseCode = "403",
            description = "Доступ запрещен - требуются права администратора"
        ),
        @ApiResponse(
            responseCode = "404",
            description = "Карта не найдена"
        )
    })
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/admin/card/{cardId}")
    public ResponseEntity<Page<TransferDto>> getCardTransfersForAdmin(
            @Parameter(description = "ID карты") @PathVariable Long cardId,
            @Parameter(description = "Номер страницы") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Размер страницы") @RequestParam(defaultValue = "10") int size,
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
    @Operation(
        summary = "Получить статистику переводов карты",
        description = "Возвращает статистику переводов для указанной карты: общий доход, расход, баланс и количество операций. Доступно только администраторам."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Статистика получена успешно",
            content = @Content(schema = @Schema(implementation = com.example.bankcards.dto.transfer.CardTransferStatsDto.class))
        ),
        @ApiResponse(
            responseCode = "403",
            description = "Доступ запрещен - требуются права администратора"
        ),
        @ApiResponse(
            responseCode = "404",
            description = "Карта не найдена"
        )
    })
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/admin/card/{cardId}/stats")
    public ResponseEntity<com.example.bankcards.dto.transfer.CardTransferStatsDto> getCardTransferStats(
            @Parameter(description = "ID карты") @PathVariable Long cardId,
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
    @Operation(
        summary = "Получить статистику переводов пользователя",
        description = "Возвращает статистику переводов для всех карт указанного пользователя: общий доход, расход и детализацию по каждой карте. Доступно только администраторам."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Статистика получена успешно",
            content = @Content(schema = @Schema(implementation = com.example.bankcards.dto.transfer.UserTransferStatsDto.class))
        ),
        @ApiResponse(
            responseCode = "403",
            description = "Доступ запрещен - требуются права администратора"
        ),
        @ApiResponse(
            responseCode = "404",
            description = "Пользователь не найден"
        )
    })
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