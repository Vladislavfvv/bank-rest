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
@Tag(name = "Transfers", description = "Transfer management between cards")
@SecurityRequirement(name = "bearerAuth")
public class TransferController {

    private final TransferService transferService;

    /**
     * Transfer between own cards
     * Available only for users (USER)
     */
    @Operation(
        summary = "Transfer between cards",
        description = "Performs money transfer between cards. Users can transfer from their own cards to any cards in the system."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Transfer completed successfully",
            content = @Content(schema = @Schema(implementation = TransferDto.class))
        ),
        @ApiResponse(
            responseCode = "400",
            description = "Invalid transfer data"
        ),
        @ApiResponse(
            responseCode = "403",
            description = "No access to sender card"
        ),
        @ApiResponse(
            responseCode = "422",
            description = "Insufficient funds on card"
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
        summary = "Get user transfer history",
        description = "Returns paginated list of all transfers for current user (both incoming and outgoing)."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Transfer history retrieved successfully"
        ),
        @ApiResponse(
            responseCode = "401",
            description = "Unauthorized"
        ),
        @ApiResponse(
            responseCode = "403",
            description = "Access denied - USER role required"
        )
    })
    @PreAuthorize("hasRole('USER')")
    @GetMapping("/my")
    public ResponseEntity<Page<TransferDto>> getMyTransfers(
            @Parameter(description = "Page number") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size") @RequestParam(defaultValue = "10") int size,
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
        summary = "Get card transfer history",
        description = "Returns paginated list of all transfers for specified card. Users can get history only for their own cards."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Card transfer history retrieved successfully"
        ),
        @ApiResponse(
            responseCode = "403",
            description = "No access to this card"
        ),
        @ApiResponse(
            responseCode = "404",
            description = "Card not found"
        )
    })
    @PreAuthorize("hasRole('USER')")
    @GetMapping("/card/{cardId}")
    public ResponseEntity<Page<TransferDto>> getCardTransfers(
            @Parameter(description = "Card ID") @PathVariable Long cardId,
            @Parameter(description = "Page number") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size") @RequestParam(defaultValue = "10") int size,
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
        summary = "Get card transfer history (for administrator)",
        description = "Returns paginated list of all transfers for specified card. Available only to administrators."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Card transfer history retrieved successfully"
        ),
        @ApiResponse(
            responseCode = "403",
            description = "Access denied - administrator rights required"
        ),
        @ApiResponse(
            responseCode = "404",
            description = "Card not found"
        )
    })
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/admin/card/{cardId}")
    public ResponseEntity<Page<TransferDto>> getCardTransfersForAdmin(
            @Parameter(description = "Card ID") @PathVariable Long cardId,
            @Parameter(description = "Page number") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size") @RequestParam(defaultValue = "10") int size,
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
        summary = "Get card transfer statistics",
        description = "Returns transfer statistics for specified card: total income, expense, balance and number of operations. Available only to administrators."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Statistics retrieved successfully",
            content = @Content(schema = @Schema(implementation = com.example.bankcards.dto.transfer.CardTransferStatsDto.class))
        ),
        @ApiResponse(
            responseCode = "403",
            description = "Access denied - administrator rights required"
        ),
        @ApiResponse(
            responseCode = "404",
            description = "Card not found"
        )
    })
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/admin/card/{cardId}/stats")
    public ResponseEntity<com.example.bankcards.dto.transfer.CardTransferStatsDto> getCardTransferStats(
            @Parameter(description = "Card ID") @PathVariable Long cardId,
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
        summary = "Get user transfer statistics",
        description = "Returns transfer statistics for all cards of specified user: total income, expense and breakdown by each card. Available only to administrators."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Statistics retrieved successfully",
            content = @Content(schema = @Schema(implementation = com.example.bankcards.dto.transfer.UserTransferStatsDto.class))
        ),
        @ApiResponse(
            responseCode = "403",
            description = "Access denied - administrator rights required"
        ),
        @ApiResponse(
            responseCode = "404",
            description = "User not found"
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