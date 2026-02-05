package com.example.bankcards.controller;

import com.example.bankcards.dto.card.BlockRequestDto;
import com.example.bankcards.dto.card.CardDto;
import com.example.bankcards.dto.card.CreateBlockRequest;
import com.example.bankcards.dto.card.CreateCardRequest;
import com.example.bankcards.dto.user.UserDto;
import com.example.bankcards.entity.RequestStatus;
import com.example.bankcards.entity.Status;
import com.example.bankcards.service.BlockRequestService;
import com.example.bankcards.service.CardService;
import com.example.bankcards.service.UserService;
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
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.format.annotation.DateTimeFormat;

import java.math.BigDecimal;
import java.time.LocalDate;

@Slf4j
@RestController
@RequestMapping("/cards")
@RequiredArgsConstructor
@SuppressWarnings({"all", "SimilarLogMessages"})
@Tag(name = "Cards", description = "Bank card management")
@SecurityRequirement(name = "bearerAuth")
public class CardController {

    private final CardService cardService;
    private final UserService userService;
    private final BlockRequestService blockRequestService;

    public static final String ADMIN_RETRIEVED = "Admin {} retrieved {}";

    /**
     * Create card.
     * ADMIN: can create card for any user.
     * USER: cannot create card.
     */
//    @PreAuthorize("hasRole('ADMIN')")
//    @PostMapping
//    public ResponseEntity<CardDto> addCard(
//            @Valid @RequestBody CardDto cardDto,
//            Authentication authentication) {
//
//        String adminEmail = SecurityUtils.getEmailFromToken(authentication);
//        log.info("Admin {} creating card for user: {}", adminEmail, cardDto.getUserId());
//
//        CardDto created = cardService.save(cardDto);
//
//        log.info("Admin {} created card: {} for user: {}", adminEmail, created.getId(), created.getUserId());
//        return ResponseEntity.status(HttpStatus.CREATED).body(created);
//    }

    /**
     * Get card by ID.
     * ADMIN: can get any card.
     * USER: can get only their own cards.
     */
    @Operation(
        summary = "Get card by ID",
        description = "Returns bank card information. Users can only get their own cards, administrators can get any card."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Card found",
            content = @Content(schema = @Schema(implementation = CardDto.class))
        ),
        @ApiResponse(
            responseCode = "403",
            description = "No access to card"
        ),
        @ApiResponse(
            responseCode = "404",
            description = "Card not found"
        )
    })
    @GetMapping("/{id}")
    public ResponseEntity<CardDto> getCardById(
            @Parameter(description = "Card ID") @PathVariable Long id,
            Authentication authentication) {
        CardDto card = cardService.getCardById(id);

        // Get card owner information
        UserDto cardOwner = userService.findUserById(card.getUserId());

        // Access check: USER can get only their own cards
        if (!SecurityUtils.hasAccess(authentication, cardOwner.getEmail())) {
            throw new AccessDeniedException("Access denied: You can only access your own cards");
        }

        return ResponseEntity.ok(card);
    }

    /**
     * Get list of all cards with filtering.
     * ADMIN: can get all cards.
     * USER: can get only their own cards (filtering is performed in service).
     * Filter parameters:
     * - status: filter by card status (ACTIVE, BLOCKED, EXPIRED)
     * - expirationDateFrom: filter by expiration date (from)
     * - expirationDateTo: filter by expiration date (to)
     * - lastFourDigits: search by last 4 digits of card number
     */
    @Operation(
        summary = "Get list of cards",
        description = "Returns list of bank cards with filtering options. Users see only their cards, administrators see all cards."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Card list retrieved successfully"
        ),
        @ApiResponse(
            responseCode = "401",
            description = "Unauthorized"
        )
    })
    @GetMapping
    public ResponseEntity<Page<CardDto>> getAllCards(
            @Parameter(description = "Page number") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size") @RequestParam(defaultValue = "10") int size,
            @Parameter(description = "Card status") @RequestParam(required = false) Status status,
            @Parameter(description = "Expiration date from") @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate expirationDateFrom,
            @Parameter(description = "Expiration date to") @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate expirationDateTo,
            @Parameter(description = "Last 4 digits of card number") @RequestParam(required = false) String lastFourDigits,
            Authentication authentication) {
        
        String userEmail = SecurityUtils.getEmailFromToken(authentication);
        boolean isAdmin = SecurityUtils.isAdmin(authentication);
        
        if (isAdmin) {
            log.info("Admin {} requesting all cards with filters: status={}, expirationDateFrom={}, expirationDateTo={}, lastFourDigits={}", 
                    userEmail, status, expirationDateFrom, expirationDateTo, lastFourDigits);
        } else {
            log.info("User {} requesting their cards with filters: status={}, expirationDateFrom={}, expirationDateTo={}, lastFourDigits={}", 
                    userEmail, status, expirationDateFrom, expirationDateTo, lastFourDigits);
        }
        
        // Service will filter cards itself: ADMIN gets all cards, USER - only their own
        Page<CardDto> cards = cardService.getAllCards(page, size, status,
                expirationDateFrom, expirationDateTo,
                lastFourDigits);
        
        if (isAdmin) {
            log.info(ADMIN_RETRIEVED + "cards", userEmail, cards.getTotalElements());
        } else {
            log.info("User {} retrieved {} cards", userEmail, cards.getTotalElements());
        }
        
        return ResponseEntity.ok(cards);
    }

    /**
     * Get card CVV for transfer verification.
     * USER: can get CVV only for their own cards.
     * ADMIN: can get CVV for any card.
     */
    @Operation(
        summary = "Get card CVV code",
        description = "Returns card CVV code for transfer verification. Users can get CVV only for their own cards."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "CVV code retrieved successfully",
            content = @Content(schema = @Schema(type = "string", example = "123"))
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
    @GetMapping("/{id}/cvv")
    public ResponseEntity<String> getCardCvv(
            @Parameter(description = "Card ID") @PathVariable Long id,
            Authentication authentication) {
        
        String userEmail = SecurityUtils.getEmailFromToken(authentication);
        log.info("User {} requesting CVV for card: {}", userEmail, id);

        String cvv = cardService.getCardCvv(id);
        
        log.info("CVV retrieved for card: {} by user: {}", id, userEmail);
        return ResponseEntity.ok(cvv);
    }

    /**
     * Update card.
     * ADMIN: can update any card.
     * USER: cannot update cards.
     */
    @Operation(
        summary = "Update card",
        description = "Updates bank card data. Available only to administrators."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Card successfully updated",
            content = @Content(schema = @Schema(implementation = CardDto.class))
        ),
        @ApiResponse(
            responseCode = "400",
            description = "Invalid card data"
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
    @PutMapping("/{id}")
    public ResponseEntity<CardDto> updateCard(
            @Parameter(description = "Card ID") @PathVariable Long id,
            @Valid @RequestBody CardDto dto,
            Authentication authentication) {
        // Get current card for access check
        CardDto currentCard = cardService.getCardById(id);
        userService.findUserById(currentCard.getUserId());

        // Access check: only ADMIN can update cards
        if (!SecurityUtils.isAdmin(authentication)) {
            throw new AccessDeniedException("Access denied: Only administrators can update all cards");
        }

        return ResponseEntity.ok(cardService.updateCard(id, dto));
    }

    /**
     * Delete card.
     * Only ADMIN can delete cards.
     * USER cannot delete cards.
     */
    @Operation(
        summary = "Delete card",
        description = "Deletes bank card from the system. Also deletes all related transfers and block requests. Available only to administrators."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Card successfully deleted"
        ),
        @ApiResponse(
            responseCode = "403",
            description = "Access denied - administrator rights required"
        ),
        @ApiResponse(
            responseCode = "404",
            description = "Card not found"
        ),
        @ApiResponse(
            responseCode = "409",
            description = "Cannot delete card - related data exists"
        )
    })
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteCard(
            @Parameter(description = "Card ID") @PathVariable Long id,
            Authentication authentication) {
        // Access check: only ADMIN can delete cards
        if (!SecurityUtils.isAdmin(authentication)) {
            throw new AccessDeniedException("Access denied: Only administrators can delete cards");
        }

        // Check that card exists
        cardService.getCardById(id);

        cardService.deleteCard(id);
        return ResponseEntity.noContent().build();
    }

    // ========== ADMIN METHODS ==========

    /**
     * Create card for user (admin only)
     */
    @Operation(
        summary = "Create card for user",
        description = "Creates new bank card for specified user. Available only to administrators."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "201",
            description = "Card successfully created",
            content = @Content(schema = @Schema(implementation = CardDto.class))
        ),
        @ApiResponse(
            responseCode = "403",
            description = "Access denied - administrator rights required"
        ),
        @ApiResponse(
            responseCode = "404",
            description = "User not found"
        ),
        @ApiResponse(
            responseCode = "400",
            description = "Invalid card data"
        )
    })
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/admin/create-for-user/{userId}")
    public ResponseEntity<CardDto> createCardForUser(
            @Parameter(description = "User ID") @PathVariable Long userId,
            @Valid @RequestBody CreateCardRequest request,
            Authentication authentication) {

        String adminEmail = SecurityUtils.getEmailFromToken(authentication);
        log.info("Admin {} creating card for user: {}", adminEmail, userId);

        CardDto card = cardService.createCardForUser(userId, request);

        log.info("Admin {} created card: {} for user: {}", adminEmail, card.getId(), userId);
        return ResponseEntity.status(HttpStatus.CREATED).body(card);
    }

    /**
     * Block card (admin only)
     */
    @Operation(
        summary = "Block card",
        description = "Blocks bank card. Available only to administrators."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Card successfully blocked",
            content = @Content(schema = @Schema(implementation = CardDto.class))
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
    @PutMapping("/admin/{id}/block")
    public ResponseEntity<CardDto> blockCard(
            @Parameter(description = "Card ID") @PathVariable Long id,
            Authentication authentication) {

        String adminEmail = SecurityUtils.getEmailFromToken(authentication);
        log.info("Admin {} blocking card: {}", adminEmail, id);

        CardDto card = cardService.blockCard(id);

        log.info("Admin {} blocked card: {}", adminEmail, id);
        return ResponseEntity.ok(card);
    }

    /**
     * Activate (unblock) card (admin only)
     */
    @Operation(
        summary = "Activate card",
        description = "Activates (unblocks) bank card. Available only to administrators."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Card successfully activated",
            content = @Content(schema = @Schema(implementation = CardDto.class))
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
    @PutMapping("/admin/{id}/activate")
    public ResponseEntity<CardDto> activateCard(
            @Parameter(description = "Card ID") @PathVariable Long id,
            Authentication authentication) {

        String adminEmail = SecurityUtils.getEmailFromToken(authentication);
        log.info("Admin {} activating (unblocking) card: {}", adminEmail, id);

        CardDto card = cardService.activateCard(id);

        log.info("Admin {} activated (unblocked) card: {}", adminEmail, id);
        return ResponseEntity.ok(card);
    }

    /**
     * Get user cards
     * ADMIN: can get cards of any user
     * USER: can get only their own cards
     */
    @PreAuthorize("hasAnyRole('ADMIN','USER')")
    @GetMapping("/admin/user/{userId}")
    public ResponseEntity<Page<CardDto>> getUserCards(
            @PathVariable Long userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            Authentication authentication) {

        String userEmail = SecurityUtils.getEmailFromToken(authentication);
        boolean isAdmin = SecurityUtils.isAdmin(authentication);

        // Access check: USER can view only their own cards
        if (!isAdmin) {
            UserDto currentUser = userService.getUserByEmail(userEmail);
            if (!currentUser.getId().equals(userId)) {
                log.warn("User {} attempted to access cards for user: {}", userEmail, userId);
                throw new AccessDeniedException("Access denied: You can only view your own cards");
            }
        }

        if (isAdmin) {
            log.info("Admin {} requesting cards for user: {}", userEmail, userId);
        } else {
            log.info("User {} requesting their own cards", userEmail);
        }

        Page<CardDto> cards = cardService.getUserCardsForAdmin(userId, page, size);

        if (isAdmin) {
            log.info(ADMIN_RETRIEVED + "cards for user: {}", userEmail, cards.getTotalElements(), userId);
        } else {
            log.info("User {} retrieved {} cards", userEmail, cards.getTotalElements());
        }
        return ResponseEntity.ok(cards);
    }

    // ========== USER METHODS ==========

    /**
     * Request blocking of own card (for users)
     * User cannot block card themselves, but can send request to admin
     */
    @Operation(
        summary = "Request card blocking",
        description = "Creates card blocking request. Users can request blocking only for their own cards. Request will be reviewed by administrator."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "201",
            description = "Block request created successfully",
            content = @Content(schema = @Schema(implementation = BlockRequestDto.class))
        ),
        @ApiResponse(
            responseCode = "403",
            description = "No access to this card"
        ),
        @ApiResponse(
            responseCode = "404",
            description = "Card not found"
        ),
        @ApiResponse(
            responseCode = "409",
            description = "Block request already exists"
        )
    })
    @PreAuthorize("hasRole('USER')")
    @PostMapping("/{id}/request-block")
    public ResponseEntity<BlockRequestDto> requestCardBlock(
            @Parameter(description = "Card ID") @PathVariable Long id,
            @Valid @RequestBody CreateBlockRequest request,
            Authentication authentication) {

        String userEmail = SecurityUtils.getEmailFromToken(authentication);
        log.info("User {} requesting block for card: {}", userEmail, id);

        // Get card and check access
        CardDto card = cardService.getCardById(id);
        UserDto cardOwner = userService.findUserById(card.getUserId());

        // Access check: USER can request blocking only of their own cards
        if (!SecurityUtils.hasAccess(authentication, cardOwner.getEmail())) {
            throw new AccessDeniedException("Access denied: You can only request blocking of your own cards");
        }

        // Create and save block request
        BlockRequestDto blockRequest = blockRequestService.createBlockRequest(id, request.getReason(), authentication);

        log.info("User {} requested blocking of card {} with reason: {}. Request ID: {}",
                userEmail, id, request.getReason(), blockRequest.getId());

        return ResponseEntity.status(HttpStatus.CREATED).body(blockRequest);
    }

    /**
     * View card balance (for users)
     */
    @PreAuthorize("hasAnyRole('USER','ADMIN')")
    @GetMapping("/{id}/balance")
    public ResponseEntity<BigDecimal> getCardBalance(
            @PathVariable Long id,
            Authentication authentication) {

        String userEmail = SecurityUtils.getEmailFromToken(authentication);
        log.info("User {} requesting balance for card: {}", userEmail, id);

        // Get card and check access
        CardDto card = cardService.getCardById(id);
        
        // ADMIN can view balance of any card, USER can view only their own cards
        boolean isAdmin = SecurityUtils.isAdmin(authentication);
        if (!isAdmin) {
            UserDto cardOwner = userService.findUserById(card.getUserId());
            if (!SecurityUtils.hasAccess(authentication, cardOwner.getEmail())) {
                throw new AccessDeniedException("Access denied: You can only view balance of your own cards");
            }
        }

        return ResponseEntity.ok(card.getBalance());
    }

    // ========== BLOCK REQUEST METHODS ==========

    /**
     * Get all block requests (admin only)
     */
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/admin/block-requests")
    public ResponseEntity<Page<BlockRequestDto>> getAllBlockRequests(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) RequestStatus status,
            Authentication authentication) {

        String adminEmail = SecurityUtils.getEmailFromToken(authentication);
        log.info("Admin {} requesting block requests, page: {}, size: {}, status: {}",
                adminEmail, page, size, status);

        Page<BlockRequestDto> requests = blockRequestService.getAllBlockRequests(page, size, status);

        log.info(ADMIN_RETRIEVED + "block requests", adminEmail, requests.getTotalElements());
        return ResponseEntity.ok(requests);
    }

    /**
     * Get count of pending requests (admin only)
     * Used for notifications
     */
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/admin/block-requests/pending-count")
    public ResponseEntity<Long> getPendingBlockRequestsCount(Authentication authentication) {
        String adminEmail = SecurityUtils.getEmailFromToken(authentication);
        long count = blockRequestService.getPendingRequestsCount();
        log.info("Admin {} checking pending block requests count: {}", adminEmail, count);
        return ResponseEntity.ok(count);
    }

    /**
     * Approve block request (admin only)
     */
    @Operation(
        summary = "Approve card block request",
        description = "Approves user's card block request and automatically blocks the card. Available only to administrators."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Request approved, card blocked",
            content = @Content(schema = @Schema(implementation = BlockRequestDto.class))
        ),
        @ApiResponse(
            responseCode = "403",
            description = "Access denied - administrator rights required"
        ),
        @ApiResponse(
            responseCode = "404",
            description = "Block request not found"
        ),
        @ApiResponse(
            responseCode = "409",
            description = "Request already processed"
        )
    })
    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/admin/block-requests/{requestId}/approve")
    public ResponseEntity<BlockRequestDto> approveBlockRequest(
            @Parameter(description = "Block request ID") @PathVariable Long requestId,
            @RequestBody(required = false) String adminComment,
            Authentication authentication) {

        String adminEmail = SecurityUtils.getEmailFromToken(authentication);
        log.info("Admin {} approving block request {}", adminEmail, requestId);

        BlockRequestDto approved = blockRequestService.approveBlockRequest(requestId, adminComment, authentication);

        log.info("Admin {} approved block request {}, card {} is now blocked",
                adminEmail, requestId, approved.getCardId());
        return ResponseEntity.ok(approved);
    }

    /**
     * Reject block request (admin only)
     */
    @Operation(
        summary = "Reject card block request",
        description = "Rejects user's card block request. Card remains active. Available only to administrators."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Request rejected",
            content = @Content(schema = @Schema(implementation = BlockRequestDto.class))
        ),
        @ApiResponse(
            responseCode = "403",
            description = "Access denied - administrator rights required"
        ),
        @ApiResponse(
            responseCode = "404",
            description = "Block request not found"
        ),
        @ApiResponse(
            responseCode = "409",
            description = "Request already processed"
        )
    })
    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/admin/block-requests/{requestId}/reject")
    public ResponseEntity<BlockRequestDto> rejectBlockRequest(
            @Parameter(description = "Block request ID") @PathVariable Long requestId,
            @RequestBody(required = false) String adminComment,
            Authentication authentication) {

        String adminEmail = SecurityUtils.getEmailFromToken(authentication);
        log.info("Admin {} rejecting block request {}", adminEmail, requestId);

        BlockRequestDto rejected = blockRequestService.rejectBlockRequest(requestId, adminComment, authentication);

        log.info("Admin {} rejected block request {}", adminEmail, requestId);
        return ResponseEntity.ok(rejected);
    }

    /**
     * Get card by block request ID (admin only)
     * Convenient way for admin to view card details from request
     * for assessing blocking necessity
     */
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/admin/block-requests/{requestId}/card")
    public ResponseEntity<CardDto> getCardByBlockRequestId(
            @PathVariable Long requestId,
            Authentication authentication) {

        String adminEmail = SecurityUtils.getEmailFromToken(authentication);
        log.info("Admin {} viewing card details for block request {}", adminEmail, requestId);

        CardDto card = blockRequestService.getCardByBlockRequestId(requestId);

        log.info("Admin {} retrieved card {} (status: {}, balance: {}) for block request {}",
                adminEmail, card.getId(), card.getStatus(), card.getBalance(), requestId);
        return ResponseEntity.ok(card);
    }

    /**
     * Get all cards with pending block requests (admin only)
     * Allows admin to see list of all cards that users want to block
     */
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/admin/cards/with-pending-block-requests")
    public ResponseEntity<Page<CardDto>> getCardsWithPendingBlockRequests(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            Authentication authentication) {

        String adminEmail = SecurityUtils.getEmailFromToken(authentication);
        log.info("Admin {} requesting cards with pending block requests, page: {}, size: {}",
                adminEmail, page, size);

        Page<CardDto> cards = blockRequestService.getCardsWithPendingBlockRequests(page, size);

        log.info(ADMIN_RETRIEVED + "cards with pending block requests",
                adminEmail, cards.getTotalElements());
        return ResponseEntity.ok(cards);
    }
}
