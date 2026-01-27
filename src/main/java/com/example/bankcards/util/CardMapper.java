package com.example.bankcards.util;

import com.example.bankcards.dto.CardDto;
import com.example.bankcards.entity.Card;
import org.mapstruct.*;

import java.util.List;

@Mapper(componentModel = "spring")
public interface CardMapper {

    // Entity -> DTO mapping
    @Mapping(target = "userId", expression = "java(card.getUser() != null ? card.getUser().getId() : null)")
    @Mapping(target = "maskedNumber", expression = "java(card.getMaskedNumber())")
    @Mapping(target = "maskedExpirationDate", expression = "java(card.getMaskedExpirationDate())")
    @Mapping(target = "cvv", ignore = true) // Никогда не маппим CVV в DTO
    CardDto toDto(Card card);

    // DTO -> Entity mapping
    @Mapping(target = "user", ignore = true) // User устанавливается отдельно в сервисе
    @Mapping(target = "cvv", ignore = true) // CVV устанавливается отдельно
    @Mapping(target = "maskedNumber", ignore = true) // Вычисляемое поле
    @Mapping(target = "maskedExpirationDate", ignore = true) // Вычисляемое поле
    Card toEntity(CardDto dto);

    // Update existing entity from DTO
    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "user", ignore = true)
    @Mapping(target = "cvv", ignore = true)
    void updateCardFromDto(CardDto dto, @MappingTarget Card card);

    // List mappings
    List<CardDto> cardsToDto(List<Card> cards);
    List<Card> cardsToEntity(List<CardDto> cardDtos);
}
