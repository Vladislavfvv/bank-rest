package com.example.bankcards.controller;

import com.example.bankcards.dto.CardDto;
import com.example.bankcards.dto.UserDto;
import com.example.bankcards.service.CardService;
import com.example.bankcards.service.UserService;
import com.example.bankcards.util.SecurityUtils;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/v1/cards")
@RequiredArgsConstructor
public class CardController {

    private final CardService cardService;
    private final UserService userService;

    /**
     * Создание карты.
     * ADMIN: может создать карту для любого пользователя.
     * USER: не может создать карту.
     */
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping
    public ResponseEntity<CardDto> addCard(
            @Valid @RequestBody CardDto cardDto,
            Authentication authentication) {

        CardDto created = cardService.save(cardDto);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    /**
     * Получение карты по ID.
     * ADMIN: может получить любую карту.
     * USER: может получить только свои карты.
     */
    @GetMapping("/{id}")
    public ResponseEntity<CardDto> getCardById(
            @PathVariable Long id,
            Authentication authentication) {
        CardDto card = cardService.getCardById(id);

        // Получаем информацию о владельце карты
        UserDto cardOwner = userService.findUserById(card.getUserId());

        // Проверка доступа: USER может получить только свои карты
        if (!SecurityUtils.hasAccess(authentication, cardOwner.getEmail())) {
            throw new AccessDeniedException("Access denied: You can only access your own cards");
        }

        return ResponseEntity.ok(card);
    }

    /**
     * Получение списка всех карт.
     * ADMIN: может получить все карты.
     * USER: может получить только свои карты (фильтрация выполняется в сервисе).
     */
    @GetMapping
    public ResponseEntity<Page<CardDto>> getAllCards(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            Authentication authentication) {
        // Сервис сам отфильтрует карты: ADMIN получит все карты, USER - только свои
        return ResponseEntity.ok(cardService.getAllCards(page, size));
    }

    /**
     * Обновление карты.
     * ADMIN: может обновить любую карту.
     * USER: не может обновить карты.
     */
    @PutMapping("/{id}")
    public ResponseEntity<CardDto> updateCard(
            @PathVariable Long id,
            @Valid @RequestBody CardDto dto,
            Authentication authentication) {
        // Получаем текущую карту для проверки доступа
        CardDto currentCard = cardService.getCardById(id);
        UserDto cardOwner = userService.findUserById(currentCard.getUserId());

        // Проверка доступа: USER может обновить только свои карты
        if (!SecurityUtils.isAdmin(authentication)) {
            throw new AccessDeniedException("Access denied: Only administrators can update all cards");
        }

        return ResponseEntity.ok(cardService.updateCard(id, dto));
    }

    /**
     * Удаление карты.
     * Только ADMIN может удалять карты.
     * USER не может удалять никакие карты.
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteCardInfo(
            @PathVariable Long id,
            Authentication authentication) {
        // Проверка доступа: только ADMIN может удалять карты
        if (!SecurityUtils.isAdmin(authentication)) {
            throw new AccessDeniedException("Access denied: Only administrators can delete cards");
        }

        // Проверяем, что карта существует
        cardService.getCardById(id);

        cardService.deleteCard(id);
        return ResponseEntity.noContent().build();
    }

    // ========== ADMIN METHODS ==========

    /**
     * Создать карту для пользователя (только для админа)
     */
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/admin/create-for-user/{userId}")
    public ResponseEntity<CardDto> createCardForUser(
            @PathVariable Long userId,
            @Valid @RequestBody com.example.bankcards.dto.card.CreateCardRequest request,
            Authentication authentication) {
        
        String adminEmail = SecurityUtils.getEmailFromToken(authentication);
        log.info("Admin {} creating card for user: {}", adminEmail, userId);

        CardDto card = cardService.createCardForUser(userId, request);
        
        log.info("Admin {} created card: {} for user: {}", adminEmail, card.getId(), userId);
        return ResponseEntity.status(HttpStatus.CREATED).body(card);
    }

    /**
     * Заблокировать карту (только для админа)
     */
    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/admin/{id}/block")
    public ResponseEntity<CardDto> blockCard(
            @PathVariable Long id,
            Authentication authentication) {
        
        String adminEmail = SecurityUtils.getEmailFromToken(authentication);
        log.info("Admin {} blocking card: {}", adminEmail, id);

        CardDto card = cardService.blockCard(id);
        
        log.info("Admin {} blocked card: {}", adminEmail, id);
        return ResponseEntity.ok(card);
    }

    /**
     * Активировать (разблокировать) карту (только для админа)
     */
    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/admin/{id}/activate")
    public ResponseEntity<CardDto> activateCard(
            @PathVariable Long id,
            Authentication authentication) {
        
        String adminEmail = SecurityUtils.getEmailFromToken(authentication);
        log.info("Admin {} activating (unblocking) card: {}", adminEmail, id);

        CardDto card = cardService.activateCard(id);
        
        log.info("Admin {} activated (unblocked) card: {}", adminEmail, id);
        return ResponseEntity.ok(card);
    }

    /**
     * Получить карты пользователя (только для админа)
     */
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/admin/user/{userId}")
    public ResponseEntity<Page<CardDto>> getUserCardsForAdmin(
            @PathVariable Long userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            Authentication authentication) {
        
        String adminEmail = SecurityUtils.getEmailFromToken(authentication);
        log.info("Admin {} requesting cards for user: {}", adminEmail, userId);

        Page<CardDto> cards = cardService.getUserCardsForAdmin(userId, page, size);
        
        log.info("Admin {} retrieved {} cards for user: {}", adminEmail, cards.getTotalElements(), userId);
        return ResponseEntity.ok(cards);
    }

    // ========== USER METHODS ==========

    /**
     * Запросить блокировку своей карты (для пользователей)
     */
    @PreAuthorize("hasRole('USER')")
    @PutMapping("/{id}/request-block")
    public ResponseEntity<CardDto> requestCardBlock(
            @PathVariable Long id,
            Authentication authentication) {
        
        String userEmail = SecurityUtils.getEmailFromToken(authentication);
        log.info("User {} requesting block for card: {}", userEmail, id);

        // Получаем карту и проверяем доступ
        CardDto card = cardService.getCardById(id);
        UserDto cardOwner = userService.findUserById(card.getUserId());

        // Проверка доступа: USER может блокировать только свои карты
        if (!SecurityUtils.hasAccess(authentication, cardOwner.getEmail())) {
            throw new AccessDeniedException("Access denied: You can only block your own cards");
        }

        // Блокируем карту (пользователь может заблокировать свою карту)
        CardDto blockedCard = cardService.blockCard(id);
        
        log.info("User {} blocked their card: {}", userEmail, id);
        return ResponseEntity.ok(blockedCard);
    }
}
