package com.example.bankcards.service;

import com.example.bankcards.dto.card.CardDto;
import com.example.bankcards.dto.card.BlockRequestDto;
import com.example.bankcards.entity.Card;
import com.example.bankcards.entity.CardBlockRequest;
import com.example.bankcards.entity.RequestStatus;
import com.example.bankcards.entity.User;
import com.example.bankcards.exception.CardNotFoundException;
import com.example.bankcards.exception.UserNotFoundException;
import com.example.bankcards.repository.CardBlockRequestRepository;
import com.example.bankcards.repository.CardRepository;
import com.example.bankcards.repository.UserRepository;
import com.example.bankcards.util.SecurityUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class BlockRequestService {

    private final CardBlockRequestRepository blockRequestRepository;
    private final CardRepository cardRepository;
    private final UserRepository userRepository;
    private final CardService cardService;

    private static final String BLOCK_REQUEST_NOT_FOUND_MESSAGE = "Block request not found with id: ";

    /**
     * Creates a new block request for a card.
     * 
     * @param cardId ID of the card to block
     * @param reason Reason for blocking
     * @param authentication Current user authentication
     * @return Created block request DTO
     */
    @Transactional
    public BlockRequestDto createBlockRequest(Long cardId, String reason, Authentication authentication) {
        String userEmail = SecurityUtils.getEmailFromToken(authentication);
        log.info("Creating block request for card {} by user {}", cardId, userEmail);

        // Get card
        Card card = cardRepository.findById(cardId)
                .orElseThrow(() -> new CardNotFoundException("Card not found with id: " + cardId));

        // Get user
        User user = userRepository.findByEmailNativeQuery(userEmail)
                .orElseThrow(() -> new UserNotFoundException("User not found with email: " + userEmail));

        // Check if there's already a pending request for this card
        List<CardBlockRequest> existingRequests = blockRequestRepository.findByCardIdAndStatus(
                cardId, RequestStatus.PENDING);
        if (!existingRequests.isEmpty()) {
            log.warn("User {} already has a pending block request for card {}", userEmail, cardId);
            throw new IllegalArgumentException("You already have a pending block request for this card");
        }

        // Create block request
        CardBlockRequest blockRequest = CardBlockRequest.builder()
                .card(card)
                .user(user)
                .reason(reason)
                .status(RequestStatus.PENDING)
                .createdAt(LocalDateTime.now())
                .build();

        CardBlockRequest saved = blockRequestRepository.save(blockRequest);
        log.info("Block request created: id={}, cardId={}, userId={}", 
                saved.getId(), cardId, user.getId());

        // Notify admin about new request
        notifyAdminAboutNewRequest(saved);

        return mapToDto(saved);
    }

    /**
     * Gets all block requests with pagination.
     * Used by admin to view all requests.
     * 
     * @param page Page number
     * @param size Page size
     * @param status Optional status filter
     * @return Page of block requests
     */
    @Transactional(readOnly = true)
    public Page<BlockRequestDto> getAllBlockRequests(int page, int size, RequestStatus status) {
        Pageable pageable = PageRequest.of(page, size);
        Page<CardBlockRequest> requests;

        if (status != null) {
            requests = blockRequestRepository.findByStatusOrderByCreatedAtDesc(status, pageable);
        } else {
            requests = blockRequestRepository.findAll(pageable);
        }

        return requests.map(this::mapToDto);
    }

    /**
     * Gets pending block requests count.
     * Used for notifications to admin.
     * 
     * @return Count of pending requests
     */
    @Transactional(readOnly = true)
    public long getPendingRequestsCount() {
        return blockRequestRepository.countByStatus(RequestStatus.PENDING);
    }

    /**
     * Gets card by block request ID.
     * Convenient method for admin to view card details from a block request.
     * 
     * @param requestId ID of the block request
     * @return Card DTO
     */
    @Transactional(readOnly = true)
    public CardDto getCardByBlockRequestId(Long requestId) {
        CardBlockRequest request = blockRequestRepository.findById(requestId)
                .orElseThrow(() -> new IllegalArgumentException(BLOCK_REQUEST_NOT_FOUND_MESSAGE + requestId));
        
        return cardService.getCardById(request.getCard().getId());
    }

    /**
     * Gets all cards that have pending block requests.
     * Used by admin to see which cards users want to block.
     * 
     * @param page Page number
     * @param size Page size
     * @return Page of card DTOs with pending block requests
     */
    @Transactional(readOnly = true)
    public Page<CardDto> getCardsWithPendingBlockRequests(int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        
        // Get all pending block requests
        Page<CardBlockRequest> pendingRequests = blockRequestRepository.findByStatusOrderByCreatedAtDesc(
                RequestStatus.PENDING, pageable);
        
        // Extract unique card IDs from requests
        List<Long> uniqueCardIds = pendingRequests.getContent().stream()
                .map(request -> request.getCard().getId())
                .distinct()
                .toList();
        
        // Convert card IDs to DTOs
        List<CardDto> listCardDto = uniqueCardIds.stream()
                .map(cardService::getCardById)
                .toList();
        
        // Create new page with cards
        // Note: total elements is based on unique cards, not requests
        return new PageImpl<>(listCardDto, pageable, uniqueCardIds.size());
    }

    /**
     * Approves a block request and blocks the card.
     * 
     * @param requestId ID of the block request
     * @param adminComment Optional admin comment
     * @param authentication Admin authentication
     * @return Updated block request DTO
     */
    @Transactional
    public BlockRequestDto approveBlockRequest(Long requestId, String adminComment, Authentication authentication) {
        String adminEmail = SecurityUtils.getEmailFromToken(authentication);
        log.info("Admin {} approving block request {}", adminEmail, requestId);

        CardBlockRequest request = blockRequestRepository.findById(requestId)
                .orElseThrow(() -> new IllegalArgumentException(BLOCK_REQUEST_NOT_FOUND_MESSAGE + requestId));

        if (request.getStatus() != RequestStatus.PENDING) {
            throw new IllegalArgumentException("Only pending requests can be approved");
        }

        // Get admin user
        User admin = userRepository.findByEmailNativeQuery(adminEmail)
                .orElseThrow(() -> new UserNotFoundException("Admin user not found with email: " + adminEmail));

        // Block the card
        cardService.blockCard(request.getCard().getId());

        // Update request
        request.setStatus(RequestStatus.APPROVED);
        request.setProcessedAt(LocalDateTime.now());
        request.setProcessedByAdmin(admin);
        request.setAdminComment(adminComment);

        CardBlockRequest saved = blockRequestRepository.save(request);
        log.info("Block request {} approved, card {} blocked", requestId, request.getCard().getId());

        return mapToDto(saved);
    }

    /**
     * Rejects a block request.
     * 
     * @param requestId ID of the block request
     * @param adminComment Admin comment explaining rejection
     * @param authentication Admin authentication
     * @return Updated block request DTO
     */
    @Transactional
    public BlockRequestDto rejectBlockRequest(Long requestId, String adminComment, Authentication authentication) {
        String adminEmail = SecurityUtils.getEmailFromToken(authentication);
        log.info("Admin {} rejecting block request {}", adminEmail, requestId);

        CardBlockRequest request = blockRequestRepository.findById(requestId)
                .orElseThrow(() -> new IllegalArgumentException(BLOCK_REQUEST_NOT_FOUND_MESSAGE + requestId));

        if (request.getStatus() != RequestStatus.PENDING) {
            throw new IllegalArgumentException("Only pending requests can be rejected");
        }

        // Get admin user
        User admin = userRepository.findByEmailNativeQuery(adminEmail)
                .orElseThrow(() -> new UserNotFoundException("Admin user not found with email: " + adminEmail));

        // Update request
        request.setStatus(RequestStatus.REJECTED);
        request.setProcessedAt(LocalDateTime.now());
        request.setProcessedByAdmin(admin);
        request.setAdminComment(adminComment);

        CardBlockRequest saved = blockRequestRepository.save(request);
        log.info("Block request {} rejected", requestId);

        return mapToDto(saved);
    }

    /**
     * Maps CardBlockRequest entity to DTO.
     * Includes card information for admin to evaluate the request.
     */
    private BlockRequestDto mapToDto(CardBlockRequest request) {
        String cardMaskedNumber = cardService.getMaskedNumberForCard(request.getCard());
        Card card = request.getCard();
        
        return BlockRequestDto.builder()
                .id(request.getId())
                .cardId(card.getId())
                .cardMaskedNumber(cardMaskedNumber)
                .userId(request.getUser().getId())
                .userEmail(request.getUser().getEmail())
                .reason(request.getReason())
                .status(request.getStatus())
                .createdAt(request.getCreatedAt())
                .processedAt(request.getProcessedAt())
                .processedByAdminId(request.getProcessedByAdmin() != null ? request.getProcessedByAdmin().getId() : null)
                .processedByAdminEmail(request.getProcessedByAdmin() != null ? request.getProcessedByAdmin().getEmail() : null)
                .adminComment(request.getAdminComment())
                // Additional card information for admin
                .cardStatus(card.getStatus())
                .cardBalance(card.getBalance())
                .cardExpirationDate(card.getExpirationDate())
                .build();
    }

    /**
     * Notifies admin about new block request
     * Currently logs the notification, but can be extended to send emails, etc.
     */
    private void notifyAdminAboutNewRequest(CardBlockRequest request) {
        // Use repository directly to avoid @Transactional self-invocation issue
        long pendingCount = blockRequestRepository.countByStatus(RequestStatus.PENDING);
        log.warn("⚠️ NEW BLOCK REQUEST: Request ID={}, Card ID={}, User={}, Reason={}. " +
                "Total pending requests: {}", 
                request.getId(), 
                request.getCard().getId(), 
                request.getUser().getEmail(),
                request.getReason(),
                pendingCount);
    }
}
