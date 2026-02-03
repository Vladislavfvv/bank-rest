package com.example.bankcards.service;

import com.example.bankcards.dto.card.CardDto;
import com.example.bankcards.dto.card.CreateCardRequest;
import com.example.bankcards.entity.Card;
import com.example.bankcards.entity.Status;
import com.example.bankcards.entity.User;
import com.example.bankcards.exception.CardAlreadyExistsException;
import com.example.bankcards.exception.CardNotFoundException;
import com.example.bankcards.exception.EncryptionException;
import com.example.bankcards.exception.UserNotFoundException;
import com.example.bankcards.repository.CardRepository;
import com.example.bankcards.repository.UserRepository;
import com.example.bankcards.util.CardMapper;
import com.example.bankcards.util.CardMaskingUtils;
import com.example.bankcards.util.EncryptionService;
import com.example.bankcards.util.SecurityUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class CardService {
    private final CardRepository cardRepository;
    private final CardMapper cardMapper;
    private final UserRepository userRepository;
    private final EncryptionService encryptionService;
    private final CardMaskingUtils cardMaskingUtils;

    private static final String NOT_FOUND_SUFFIX = " not found";
    private static final String PREFIX_CARD_WITH_ID = "Card with id ";
    private static final String USER_NOT_FOUND_WITH_ID = "User not found with id:";

    @PreAuthorize("hasAnyRole('ADMIN')")
    public CardDto save(CardDto dto) {
        User user = userRepository.findById(dto.getUserId())
                .orElseThrow(
                        () -> new UserNotFoundException(USER_NOT_FOUND_WITH_ID + dto.getUserId()));

        ensureCurrentUserCanAccessUser(user);

        Card entity = cardMapper.toEntity(dto);
        entity.setUser(user);
        
        // Encrypt sensitive data before saving
        if (entity.getNumber() != null) {
            entity.setNumber(encryptionService.encrypt(entity.getNumber()));
        }
        if (entity.getCvv() != null) {
            entity.setCvv(encryptionService.encrypt(entity.getCvv()));
        }

        Card saved = cardRepository.save(entity);
        return mapToDtoWithDecryption(saved);
    }

    @PreAuthorize("hasAnyRole('ADMIN','USER')")
    public CardDto getCardById(Long id) {
        Card card = cardRepository.findByIdWithUser(id)
                .orElseThrow(() -> new CardNotFoundException(PREFIX_CARD_WITH_ID + id + NOT_FOUND_SUFFIX));

        ensureCurrentUserCanAccessCard(card);
        return mapToDtoWithDecryption(card);
    }

    /**
     * Get card CVV for transfer verification.
     * USER: can get CVV only for their own cards.
     * ADMIN: can get CVV for any card.
     */
    @PreAuthorize("hasAnyRole('ADMIN','USER')")
    public String getCardCvv(Long id) {
        Card card = cardRepository.findByIdWithUser(id)
                .orElseThrow(() -> new CardNotFoundException(PREFIX_CARD_WITH_ID + id + NOT_FOUND_SUFFIX));

        ensureCurrentUserCanAccessCard(card);
        
        try {
            return encryptionService.decrypt(card.getCvv());
        } catch (Exception e) {
            log.error("Failed to decrypt CVV for card: {}", id, e);
            throw new EncryptionException("Failed to decrypt card CVV", e);
        }
    }

    @PreAuthorize("hasAnyRole('ADMIN','USER')")
    public Page<CardDto> getAllCards(int page, int size, Status status, 
                                      LocalDate expirationDateFrom, LocalDate expirationDateTo, 
                                      String lastFourDigits) {
        Authentication authentication = requireAuthentication();
        boolean isAdmin = isAdmin(authentication);

        try {
            if (isAdmin) {
                log.debug("Admin user requested all cards with filters: status={}, expirationDateFrom={}, expirationDateTo={}, lastFourDigits={}", 
                         status, expirationDateFrom, expirationDateTo, lastFourDigits);
                
                // Use filtered query for admin
                Page<Card> cards = cardRepository.findAllWithFilters(
                        status, expirationDateFrom, expirationDateTo, 
                        PageRequest.of(page, size));
                
                // Convert to DTOs with decryption
                Page<CardDto> dto = cards.map(this::mapToDtoWithDecryption);
                
                // Apply number filter if provided (after decryption)
                if (lastFourDigits != null && !lastFourDigits.isEmpty()) {
                    dto = filterByLastFourDigits(dto, lastFourDigits, page, size);
                }
                
                log.debug("Admin retrieved {} cards after filtering", dto.getTotalElements());
                return dto;
            } else {
                String userEmail = resolveCurrentUserIdentifier(authentication);
                log.debug("User {} requested their cards with filters: status={}, expirationDateFrom={}, expirationDateTo={}, lastFourDigits={}", 
                         userEmail, status, expirationDateFrom, expirationDateTo, lastFourDigits);
                
                // Use filtered query
                Page<Card> cards = cardRepository.findAllByUser_EmailIgnoreCaseWithFilters(
                        userEmail, status, expirationDateFrom, expirationDateTo, 
                        PageRequest.of(page, size));
                
                // Convert to DTOs with decryption
                Page<CardDto> dto = cards.map(this::mapToDtoWithDecryption);
                
                // Apply number filter if provided (after decryption)
                if (lastFourDigits != null && !lastFourDigits.isEmpty()) {
                    dto = filterByLastFourDigits(dto, lastFourDigits, page, size);
                }
                
                log.debug("Found {} cards for user {} after filtering", dto.getTotalElements(), userEmail);
                return dto;
            }
        } catch (Exception e) {
            log.error("Error getting cards for page {} size {} with filters", page, size, e);
            throw e;
        }
    }

    /**
     * Filters cards by last four digits of card number (after decryption).
     * This is done in memory after decryption since numbers are encrypted in DB.
     */
    private Page<CardDto> filterByLastFourDigits(Page<CardDto> page, String lastFourDigits, int pageNum, int pageSize) {
        List<CardDto> filtered = page.getContent().stream()
                .filter(card -> {
                    if (card.getMaskedNumber() == null) {
                        return false;
                    }
                    // Extract last 4 digits from masked number (format: "**** **** **** 1234")
                    String masked = card.getMaskedNumber();
                    if (masked.length() >= 4) {
                        String lastFour = masked.substring(masked.length() - 4);
                        return lastFour.equals(lastFourDigits);
                    }
                    return false;
                })
                .toList();
        
        // Create new page with filtered results
        return new PageImpl<>(filtered, PageRequest.of(pageNum, pageSize), filtered.size());
    }

    @PreAuthorize("hasAnyRole('ADMIN','USER')")
    @Transactional
    public CardDto updateCard(Long id, CardDto dto) {
        Card existing = cardRepository.findById(id)
                .orElseThrow(() -> new CardNotFoundException(PREFIX_CARD_WITH_ID + id + NOT_FOUND_SUFFIX));

        ensureCurrentUserCanAccessCard(existing);

        // Encrypt new card number if provided and different from existing
        if (dto.getNumber() != null) {
            // Decrypt existing number for comparison
            String decryptedExistingNumber = encryptionService.decrypt(existing.getNumber());
            // Only update if new number is different
            if (!dto.getNumber().equals(decryptedExistingNumber)) {
                existing.setNumber(encryptionService.encrypt(dto.getNumber()));
            }
        }
        existing.setHolder(dto.getHolder());
        existing.setExpirationDate(dto.getExpirationDate());

        Authentication authentication = requireAuthentication();
        boolean isAdmin = isAdmin(authentication);

        if (dto.getUserId() != null) {
            User currentUser = existing.getUser();
            if (!isAdmin && (currentUser == null || !currentUser.getId().equals(dto.getUserId()))) {
                throw new AccessDeniedException("Only administrators can reassign card ownership");
            }
            if (isAdmin && (currentUser == null || !currentUser.getId().equals(dto.getUserId()))) {
                User user = userRepository.findById(dto.getUserId())
                        .orElseThrow(() -> new UserNotFoundException(USER_NOT_FOUND_WITH_ID + dto.getUserId()));
                existing.setUser(user);
            }
        }

        Card saved = cardRepository.save(existing);
        return mapToDtoWithDecryption(saved);
    }

    @PreAuthorize("hasAnyRole('ADMIN')")
    @Transactional
    public void deleteCard(Long id) {
        Card card = cardRepository.findById(id)
                .orElseThrow(() -> new CardNotFoundException(PREFIX_CARD_WITH_ID + id + NOT_FOUND_SUFFIX));

        ensureCurrentUserCanAccessCard(card);

        cardRepository.delete(card);

    }

    private Authentication requireAuthentication() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null) {
            throw new AccessDeniedException("Authentication required");
        }
        return authentication;
    }

    private void ensureCurrentUserCanAccessUser(User user) {
        Authentication authentication = requireAuthentication();
        if (isAdmin(authentication)) {
            return;
        }

        String currentIdentifier = resolveCurrentUserIdentifier(authentication);
        if (user == null || user.getEmail() == null
                || !user.getEmail().equalsIgnoreCase(currentIdentifier)) {
            throw new AccessDeniedException("Access denied");
        }
    }

    private void ensureCurrentUserCanAccessCard(Card card) {
        if (card == null) {
            throw new AccessDeniedException("Access denied");
        }
        ensureCurrentUserCanAccessUser(card.getUser());
    }

    private boolean isAdmin(Authentication authentication) {
        return authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch("ROLE_ADMIN"::equals);
    }

    private String resolveCurrentUserIdentifier(Authentication authentication) {
        // Use SecurityUtils for consistency with other parts of the application
        try {
            return SecurityUtils.getEmailFromToken(authentication);
        } catch (IllegalStateException e) {
            // If failed to extract email through SecurityUtils, try alternative methods
            String identifier = tryExtractFromJwtToken(authentication);
            if (identifier != null) {
                return identifier;
            }
            
            identifier = tryExtractFromPrincipal(authentication);
            if (identifier != null) {
                return identifier;
            }
            
            identifier = tryExtractFromAuthenticationName(authentication);
            if (identifier != null) {
                return identifier;
            }

            throw new AccessDeniedException("Cannot determine current user: " + e.getMessage());
        }
    }
    
    /**
     * Attempts to extract user identifier from JWT token claims.
     */
    private String tryExtractFromJwtToken(Authentication authentication) {
        if (authentication instanceof JwtAuthenticationToken jwtAuthenticationToken) {
            Jwt jwt = jwtAuthenticationToken.getToken();
            
            // Try email claim first
            String email = jwt.getClaimAsString("email");
            if (email != null && !email.isBlank()) {
                return email;
            }
            
            // Fallback to preferred_username claim
            String preferredUsername = jwt.getClaimAsString("preferred_username");
            if (preferredUsername != null && !preferredUsername.isBlank()) {
                return preferredUsername;
            }
        }
        return null;
    }
    
    /**
     * Attempts to extract user identifier from authentication principal.
     */
    private String tryExtractFromPrincipal(Authentication authentication) {
        Object principal = authentication.getPrincipal();
        if (principal instanceof UserDetails userDetails) {
            return userDetails.getUsername();
        }
        return null;
    }
    
    /**
     * Attempts to extract user identifier from authentication name.
     */
    private String tryExtractFromAuthenticationName(Authentication authentication) {
        String name = authentication.getName();
        if (name != null && !name.isBlank()) {
            return name;
        }
        return null;
    }

    // ========== ADMIN METHODS ==========

    /**
     * Create card for user (only for admin)
     */
    @PreAuthorize("hasRole('ADMIN')")
    @Transactional
    public CardDto createCardForUser(Long userId, CreateCardRequest request) {
        log.info("Admin creating card for user: {}", userId);
        
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException(USER_NOT_FOUND_WITH_ID + userId));

        // Use card number from request
        String cardNumber = request.getNumber();
        
        // Encrypt card number for storage and checking
        String encryptedNumber = encryptionService.encrypt(cardNumber);
        
        // Check if card with this encrypted number already exists
        if (cardRepository.findByNumber(encryptedNumber).isPresent()) {
            throw new CardAlreadyExistsException("Card with number " + cardMaskingUtils.getMaskedNumber(cardNumber) + " already exists");
        }

        // Create CardDto to use existing logic
        CardDto cardDto = new CardDto();
        cardDto.setUserId(userId);
        cardDto.setNumber(cardNumber); // Plain number for DTO (will be encrypted before saving)
        cardDto.setHolder(request.getHolder() != null ? request.getHolder() : 
                         (user.getFirstName() + " " + user.getLastName()));
        cardDto.setExpirationDate(request.getExpirationDate()); // Use expiration date from request
        cardDto.setBalance(request.getBalance()); // Use balance from request
        cardDto.setStatus(Status.ACTIVE); // Set status to ACTIVE by default (new cards are always active)

        Card entity = cardMapper.toEntity(cardDto);
        entity.setUser(user);
        
        // Encrypt CVV from request
        entity.setCvv(encryptionService.encrypt(request.getCvv()));
        
        // Set encrypted card number (already encrypted above)
        entity.setNumber(encryptedNumber);

        Card saved = cardRepository.save(entity);
        String maskedNumber = getMaskedNumberForCard(saved);
        log.info("Admin created card: {} for user: {} with balance: {}", maskedNumber, userId, request.getBalance());
        
        return mapToDtoWithDecryption(saved);
    }


    /**
     * Updates status of expired cards to EXPIRED
     * Called by scheduled task to automatically mark expired cards.
     * 
     * @return Number of cards updated
     */
    @Transactional
    public int updateExpiredCardsStatus() {
        LocalDate currentDate = LocalDate.now();
        int updatedCount = cardRepository.updateExpiredCardsStatus(currentDate, Status.EXPIRED);
        
        if (updatedCount > 0) {
            log.info("Updated {} cards to EXPIRED status (expiration date < {})", updatedCount, currentDate);
        } else {
            log.debug("No expired cards found to update (current date: {})", currentDate);
        }
        
        return updatedCount;
    }

    /**
     * Block card (only for admin)
     */
    @PreAuthorize("hasRole('ADMIN')")
    @Transactional
    public CardDto blockCard(Long cardId) {
        log.info("Admin blocking card: {}", cardId);
        
        Card card = cardRepository.findById(cardId)
                .orElseThrow(() -> new CardNotFoundException(PREFIX_CARD_WITH_ID + cardId + NOT_FOUND_SUFFIX));

        card.block();
        Card saved = cardRepository.save(card);
        
        String maskedNumber = getMaskedNumberForCard(saved);
        log.info("Admin blocked card: {}", maskedNumber);
        return mapToDtoWithDecryption(saved);
    }

    /**
     * Activate card (only for admin)
     */
    @PreAuthorize("hasRole('ADMIN')")
    @Transactional
    public CardDto activateCard(Long cardId) {
        log.info("Admin activating card: {}", cardId);
        
        Card card = cardRepository.findById(cardId)
                .orElseThrow(() -> new CardNotFoundException(PREFIX_CARD_WITH_ID + cardId + NOT_FOUND_SUFFIX));

        card.activate();
        Card saved = cardRepository.save(card);
        
        String maskedNumber = getMaskedNumberForCard(saved);
        log.info("Admin activated card: {}", maskedNumber);
        return mapToDtoWithDecryption(saved);
    }

    /**
     * Get user cards (only for admin)
     */
    @PreAuthorize("hasRole('ADMIN')")
    @Transactional(readOnly = true)
    public Page<CardDto> getUserCardsForAdmin(Long userId, int page, int size) {
        log.info("Admin requesting cards for user: {}", userId);
        
        // Check user existence
        if (!userRepository.existsById(userId)) {
            throw new UserNotFoundException(USER_NOT_FOUND_WITH_ID + userId);
        }

        Page<Card> cards = cardRepository.findAllByUserId(userId, PageRequest.of(page, size));
        Page<CardDto> result = cards.map(this::mapToDtoWithDecryption);
        
        log.info("Admin retrieved {} cards for user: {}", result.getTotalElements(), userId);
        return result;
    }

    /**
     * Maps Card entity to DTO with decryption of sensitive data.
     * Card number is decrypted for masking, but only last 4 digits are used.
     * Full card number is never exposed in DTO for security.
     */
    private CardDto mapToDtoWithDecryption(Card card) {
        CardDto dto = cardMapper.toDto(card);
        
        // Check if DTO is null (should not happen, but safety check)
        if (dto == null) {
            log.warn("CardMapper returned null DTO for card: {}", card.getId());
            return null;
        }
        
        // Decrypt card number for masking (only last 4 digits needed)
        if (card.getNumber() != null) {
            String decryptedNumber = encryptionService.decrypt(card.getNumber());
            // Set masked number in DTO
            dto.setMaskedNumber("**** **** **** " + decryptedNumber.substring(decryptedNumber.length() - 4));
            // Never expose full number in DTO - set to null for security
            dto.setNumber(null);
        }
        
        return dto;
    }

    /**
     * Gets masked card number from encrypted card data.
     */
    public String getMaskedNumberForCard(Card card) {
        return cardMaskingUtils.getMaskedNumber(card);
    }

    /**
     * Verifies CVV for a card with encrypted CVV storage.
     */
    public boolean verifyCvvForCard(Card card, String inputCvv) {
        if (card.getCvv() == null || inputCvv == null) {
            return false;
        }
        String decryptedCvv = encryptionService.decrypt(card.getCvv());
        return decryptedCvv.equals(inputCvv);
    }

}
