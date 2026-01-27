package com.example.bankcards.service;

import com.example.bankcards.dto.CardDto;
import com.example.bankcards.entity.Card;
import com.example.bankcards.entity.User;
import com.example.bankcards.exception.CardNotFoundException;
import com.example.bankcards.exception.UserNotFoundException;
import com.example.bankcards.repository.CardRepository;
import com.example.bankcards.repository.UserRepository;
import com.example.bankcards.util.CardMapper;
import com.example.bankcards.util.SecurityUtils;
import jakarta.transaction.Transactional;
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

@Slf4j
@Service
@RequiredArgsConstructor
public class CardService {
    private final CardRepository cardRepository;
    private final CardMapper cardMapper;
    private final UserRepository userRepository;

    private static final String NOT_FOUND_SUFFIX = " not found";
    private static final String PREFIX_CARD_WITH_ID = "Card with id ";


    @PreAuthorize("hasAnyRole('ADMIN','USER')")
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
    public CardDto getCardInfoById(Long id) {
        Card card = cardRepository.findById(id)
                .orElseThrow(() -> new CardNotFoundException(PREFIX_CARD_WITH_ID + id + NOT_FOUND_SUFFIX));

        ensureCurrentUserCanAccessCard(card);
        return cardMapper.toDto(card);
    }

    @PreAuthorize("hasAnyRole('ADMIN','USER')")
    public Page<CardDto> getAllCardInfos(int page, int size) {
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
    public CardDto updateCardInfo(Long id, CardDto dto) {
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

    @PreAuthorize("hasAnyRole('ADMIN','USER')")
    @Transactional
    public void deleteCardInfo(Long id) {
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

}
