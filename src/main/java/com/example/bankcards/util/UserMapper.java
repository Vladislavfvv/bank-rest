package com.example.bankcards.util;

import com.example.bankcards.dto.CardDto;
import com.example.bankcards.dto.UserDto;
import com.example.bankcards.entity.Card;
import com.example.bankcards.entity.User;
import org.mapstruct.*;

import java.util.*;
import java.util.stream.Collectors;

@Mapper(componentModel = "spring", uses = {CardMapper.class})
public interface UserMapper {
    
    // User mappings
    @Mapping(target = "fullName", expression = "java(user.getFullName())")
    @Mapping(target = "cardCount", expression = "java(user.getAllCards() != null ? user.getAllCards().size() : 0)")
    UserDto toDto(User user);

    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "isActive", ignore = true)
    @Mapping(target = "cards", ignore = true) // Карты управляются отдельно
    User toEntity(UserDto dto);

    // Update method для пользователя
    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "cards", ignore = true) // Карты обновляются отдельно
    @Mapping(target = "password", ignore = true) // Пароль обновляется отдельно
    void updateUserFromDto(UserDto dto, @MappingTarget User user);

    /**
     * Обновление списка карт без зависимости от других мапперов.
     * БЕЗОПАСНАЯ версия - CVV не копируется из DTO.
     */
    default List<Card> updateCards(User user, List<CardDto> cardDtos) {
        if (cardDtos == null || cardDtos.isEmpty()) {
            return new ArrayList<>();
        }

        Map<Long, Card> existingCardsMap = Optional.ofNullable(user.getCards())
                .orElse(Collections.emptyList())
                .stream()
                .filter(c -> c.getId() != null)
                .collect(Collectors.toMap(Card::getId, c -> c));

        List<Card> updatedCards = new ArrayList<>();

        for (CardDto dto : cardDtos) {
            if (dto.getId() != null && existingCardsMap.containsKey(dto.getId())) {
                // Обновляем существующую карту
                Card existingCard = existingCardsMap.get(dto.getId());
                existingCard.setNumber(dto.getNumber());
                existingCard.setHolder(dto.getHolder());
                existingCard.setExpirationDate(dto.getExpirationDate());
                existingCard.setBalance(dto.getBalance());
                existingCard.setStatus(dto.getStatus());
                // CVV НЕ обновляется из DTO по соображениям безопасности
                updatedCards.add(existingCard);
            } else {
                // Создаем новую карту
                Card newCard = new Card();
                newCard.setId(dto.getId());
                newCard.setNumber(dto.getNumber());
                newCard.setHolder(dto.getHolder());
                newCard.setExpirationDate(dto.getExpirationDate());
                newCard.setBalance(dto.getBalance());
                newCard.setStatus(dto.getStatus());
                newCard.setUser(user);
                // CVV будет установлен отдельно в сервисе
                updatedCards.add(newCard);
            }
        }
        return updatedCards;
    }
}