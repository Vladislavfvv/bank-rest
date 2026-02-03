package com.example.bankcards.util;

import com.example.bankcards.dto.card.CardDto;
import com.example.bankcards.entity.Card;
import org.mapstruct.Mapping;
import org.mapstruct.Mapper;
import org.mapstruct.BeanMapping;
import org.mapstruct.NullValuePropertyMappingStrategy;
import org.mapstruct.MappingTarget;

import java.util.List;

@Mapper(componentModel = "spring")
public interface CardMapper {

    // Entity -> DTO mapping
    @Mapping(target = "userId", expression = "java(card.getUser() != null ? card.getUser().getId() : null)")
    @Mapping(target = "cvv", ignore = true) // Never map CVV to DTO
    CardDto toDto(Card card);

    // DTO -> Entity mapping
    @Mapping(target = "user", ignore = true) // User is set separately in service
    @Mapping(target = "cvv", ignore = true) // CVV is set separately
    Card toEntity(CardDto dto);

    // Update existing entity from DTO
    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "user", ignore = true)
    @Mapping(target = "cvv", ignore = true)
    void updateCardFromDto(CardDto dto, @MappingTarget Card card);

    // List mappings
    List<CardDto> cardsToDto(List<Card> cards);
    @SuppressWarnings("unused")
    List<Card> cardsToEntity(List<CardDto> cardDtoList);
}
