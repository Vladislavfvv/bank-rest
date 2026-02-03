package com.example.bankcards.util;

import com.example.bankcards.dto.card.CardDto;
import com.example.bankcards.dto.user.UserDto;
import com.example.bankcards.entity.Card;
import com.example.bankcards.entity.User;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Mapper(componentModel = "spring", uses = {CardMapper.class})
public interface UserMapper {
    
    // User mappings
    @Mapping(target = "fullName", expression = "java(user.getFullName())")
    @Mapping(target = "cardCount", expression = "java(user.getAllCards() != null ? user.getAllCards().size() : 0)")
    UserDto toDto(User user);

    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "isActive", ignore = true)
    @Mapping(target = "cards", ignore = true) // Cards are managed separately
    @Mapping(target = "password", ignore = true) // Password is not mapped from DTO
    User toEntity(UserDto dto);

    // Update method for user
    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "cards", ignore = true) // Cards are updated separately
    @Mapping(target = "password", ignore = true) // Password is updated separately
    @Mapping(target = "allCards", ignore = true) // Virtual field, not mapped
    void updateUserFromDto(UserDto dto, @MappingTarget User user);

    /**
     * Updates card list without dependency on other mappers.
     * SAFE version - CVV is not copied from DTO.
     */
    default List<Card> updateCards(User user, List<CardDto> cardDtoList) {
        if (cardDtoList == null || cardDtoList.isEmpty()) {
            return new ArrayList<>();
        }

        Map<Long, Card> existingCardsMap = Optional.ofNullable(user.getCards())
                .orElse(Collections.emptyList())
                .stream()
                .filter(c -> c.getId() != null)
                .collect(Collectors.toMap(Card::getId, c -> c));

        List<Card> updatedCards = new ArrayList<>();

        for (CardDto dto : cardDtoList) {
            if (dto.getId() != null && existingCardsMap.containsKey(dto.getId())) {
                // Update existing card
                Card existingCard = existingCardsMap.get(dto.getId());
                existingCard.setNumber(dto.getNumber());
                existingCard.setHolder(dto.getHolder());
                existingCard.setExpirationDate(dto.getExpirationDate());
                existingCard.setBalance(dto.getBalance());
                existingCard.setStatus(dto.getStatus());
                // CVV is NOT updated from DTO for security reasons
                updatedCards.add(existingCard);
            } else {
                // Create new card
                Card newCard = new Card();
                newCard.setId(dto.getId());
                newCard.setNumber(dto.getNumber());
                newCard.setHolder(dto.getHolder());
                newCard.setExpirationDate(dto.getExpirationDate());
                newCard.setBalance(dto.getBalance());
                newCard.setStatus(dto.getStatus());
                newCard.setUser(user);
                // CVV will be set separately in service
                updatedCards.add(newCard);
            }
        }
        return updatedCards;
    }
}