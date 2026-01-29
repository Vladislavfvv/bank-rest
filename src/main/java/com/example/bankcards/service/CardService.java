package com.example.bankcards.service;

import com.example.bankcards.dto.CardDto;
import com.example.bankcards.dto.card.CreateCardRequest;
import com.example.bankcards.entity.Card;
import com.example.bankcards.entity.User;
import com.example.bankcards.exception.CardNotFoundException;
import com.example.bankcards.exception.UserNotFoundException;
import com.example.bankcards.repository.CardRepository;
import com.example.bankcards.repository.UserRepository;
import com.example.bankcards.util.CardMapper;
import com.example.bankcards.util.SecurityUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
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

@Slf4j
@Service
@RequiredArgsConstructor
public class CardService {
    private final CardRepository cardRepository;
    private final CardMapper cardMapper;
    private final UserRepository userRepository;

    private static final String NOT_FOUND_SUFFIX = " not found";
    private static final String PREFIX_CARD_WITH_ID = "Card with id ";


    @PreAuthorize("hasAnyRole('ADMIN')")
    public CardDto save(CardDto dto) {
        User user = userRepository.findById(dto.getUserId())
                .orElseThrow(
                        () -> new UserNotFoundException("User not found with id: " + dto.getUserId()));

        ensureCurrentUserCanAccessUser(user);

        Card entity = cardMapper.toEntity(dto);
        entity.setUser(user);

        Card saved = cardRepository.save(entity);
        return cardMapper.toDto(saved);

    }

    @PreAuthorize("hasAnyRole('ADMIN','USER')")
    public CardDto getCardById(Long id) {
        Card card = cardRepository.findById(id)
                .orElseThrow(() -> new CardNotFoundException(PREFIX_CARD_WITH_ID + id + NOT_FOUND_SUFFIX));

        ensureCurrentUserCanAccessCard(card);
        return cardMapper.toDto(card);
    }

    @PreAuthorize("hasAnyRole('ADMIN','USER')")
    public Page<CardDto> getAllCards(int page, int size) {
        Authentication authentication = requireAuthentication();
        boolean isAdmin = isAdmin(authentication);

        try {
            if (isAdmin) {
                log.debug("Admin user requested all cards");
                Page<CardDto> dto = cardRepository.findAll(PageRequest.of(page, size)).map(cardMapper::toDto);
                return dto;
            } else {
                String userEmail = resolveCurrentUserIdentifier(authentication);
                log.debug("User {} requested their cards", userEmail);
                Page<CardDto> dto = cardRepository.findAllByUser_EmailIgnoreCase(userEmail,
                                PageRequest.of(page, size))
                        .map(cardMapper::toDto);
                log.debug("Found {} cards for user {}", dto.getTotalElements(), userEmail);
                return dto;
            }
        } catch (Exception e) {
            log.error("Error getting card infos for page {} size {}", page, size, e);
            throw e;
        }
    }

    @PreAuthorize("hasAnyRole('ADMIN','USER')")
    @Transactional
    public CardDto updateCard(Long id, CardDto dto) {
        Card existing = cardRepository.findById(id)
                .orElseThrow(() -> new CardNotFoundException(PREFIX_CARD_WITH_ID + id + NOT_FOUND_SUFFIX));

        ensureCurrentUserCanAccessCard(existing);

        existing.setNumber(dto.getNumber());
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
                        .orElseThrow(() -> new UserNotFoundException("User not found with id: " + dto.getUserId()));
                existing.setUser(user);
            }
        }

        return cardMapper.toDto(cardRepository.save(existing));
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
        // Используем SecurityUtils для единообразия с другими частями приложения
        try {
            return SecurityUtils.getEmailFromToken(authentication);
        } catch (IllegalStateException e) {
            // Если не удалось извлечь email через SecurityUtils, пробуем альтернативные способы
            if (authentication instanceof JwtAuthenticationToken jwtAuthenticationToken) {
                Jwt jwt = jwtAuthenticationToken.getToken();
                // Fallback на другие claims
                String email = jwt.getClaimAsString("email");
                if (email != null && !email.isBlank()) {
                    return email;
                }
                String preferredUsername = jwt.getClaimAsString("preferred_username");
                if (preferredUsername != null && !preferredUsername.isBlank()) {
                    return preferredUsername;
                }
            }

            Object principal = authentication.getPrincipal();
            if (principal instanceof UserDetails userDetails) {
                return userDetails.getUsername();
            }

            String name = authentication.getName();
            if (name != null && !name.isBlank()) {
                return name;
            }

            throw new AccessDeniedException("Cannot determine current user: " + e.getMessage());
        }
    }

    // ========== ADMIN METHODS ==========

    /**
     * Создать карту для пользователя (только для админа)
     */
    @PreAuthorize("hasRole('ADMIN')")
    @Transactional
    public CardDto createCardForUser(Long userId, CreateCardRequest request) {
        log.info("Admin creating card for user: {}", userId);
        
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("User not found with id: " + userId));

        // Генерируем номер карты (в реальном приложении это будет более сложная логика)
        String cardNumber = generateCardNumber();
        
        // Проверяем, не существует ли уже карта с таким номером
        if (cardRepository.findByNumber(cardNumber).isPresent()) {
            // Если номер занят, генерируем новый (в реальном приложении - цикл)
            cardNumber = generateCardNumber();
        }

        // Создаем CardDto для использования существующей логики
        CardDto cardDto = new CardDto();
        cardDto.setUserId(userId);
        cardDto.setNumber(cardNumber);
        cardDto.setHolder(request.getHolder() != null ? request.getHolder() : 
                         (user.getFirstName() + " " + user.getLastName()));
        cardDto.setExpirationDate(generateExpirationDate()); // Генерируем дату истечения
        cardDto.setBalance(BigDecimal.ZERO); // Начальный баланс

        Card entity = cardMapper.toEntity(cardDto);
        entity.setUser(user);
        entity.setCvv(generateCvv()); // Генерируем CVV

        Card saved = cardRepository.save(entity);
        log.info("Admin created card: {} for user: {}", saved.getMaskedNumber(), userId);
        
        return cardMapper.toDto(saved);
    }

    // Вспомогательные методы для генерации данных карты
    private String generateCardNumber() {
        // Простая генерация номера карты (в реальном приложении - более сложная логика)
        return "4000" + String.format("%012d", (long)(Math.random() * 1000000000000L));
    }

    private LocalDate generateExpirationDate() {
        // Карта действительна 3 года
        return LocalDate.now().plusYears(3);
    }

    private String generateCvv() {
        // Генерируем 3-значный CVV
        return String.format("%03d", (int)(Math.random() * 1000));
    }

    /**
     * Заблокировать карту (только для админа)
     */
    @PreAuthorize("hasRole('ADMIN')")
    @Transactional
    public CardDto blockCard(Long cardId) {
        log.info("Admin blocking card: {}", cardId);
        
        Card card = cardRepository.findById(cardId)
                .orElseThrow(() -> new CardNotFoundException(PREFIX_CARD_WITH_ID + cardId + NOT_FOUND_SUFFIX));

        card.block();
        Card saved = cardRepository.save(card);
        
        log.info("Admin blocked card: {}", saved.getMaskedNumber());
        return cardMapper.toDto(saved);
    }

    /**
     * Активировать карту (только для админа)
     */
    @PreAuthorize("hasRole('ADMIN')")
    @Transactional
    public CardDto activateCard(Long cardId) {
        log.info("Admin activating card: {}", cardId);
        
        Card card = cardRepository.findById(cardId)
                .orElseThrow(() -> new CardNotFoundException(PREFIX_CARD_WITH_ID + cardId + NOT_FOUND_SUFFIX));

        card.activate();
        Card saved = cardRepository.save(card);
        
        log.info("Admin activated card: {}", saved.getMaskedNumber());
        return cardMapper.toDto(saved);
    }

    /**
     * Получить карты пользователя (только для админа)
     */
    @PreAuthorize("hasRole('ADMIN')")
    @Transactional(readOnly = true)
    public Page<CardDto> getUserCardsForAdmin(Long userId, int page, int size) {
        log.info("Admin requesting cards for user: {}", userId);
        
        // Проверяем существование пользователя
        if (!userRepository.existsById(userId)) {
            throw new UserNotFoundException("User not found with id: " + userId);
        }

        Page<Card> cards = cardRepository.findAllByUserId(userId, PageRequest.of(page, size));
        Page<CardDto> result = cards.map(cardMapper::toDto);
        
        log.info("Admin retrieved {} cards for user: {}", result.getTotalElements(), userId);
        return result;
    }

}
