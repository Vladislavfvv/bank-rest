package com.example.bankcards.util;

import com.example.bankcards.dto.card.CardDto;
import com.example.bankcards.entity.Card;
import com.example.bankcards.entity.Status;
import com.example.bankcards.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class CardMapperTest {

    private CardMapper cardMapper;
    private Card testCard;
    private CardDto testCardDto;
    private User testUser;

    @BeforeEach
    void setUp() {
        cardMapper = Mappers.getMapper(CardMapper.class);
        
        // Create test user
        testUser = new User();
        testUser.setId(1L);
        testUser.setEmail("test@example.com");
        testUser.setFirstName("Ivan");
        testUser.setLastName("Ivanov");

        // Create test card
        testCard = new Card();
        testCard.setId(1L);
        testCard.setNumber("1234567890123456");
        testCard.setHolder("Ivan Ivanov");
        testCard.setExpirationDate(LocalDate.of(2025, 12, 31));
        testCard.setCvv("123");
        testCard.setBalance(BigDecimal.valueOf(1000.00));
        testCard.setStatus(Status.ACTIVE);
        testCard.setUser(testUser);

        // Create test DTO
        testCardDto = new CardDto();
        testCardDto.setId(1L);
        testCardDto.setNumber("1234567890123456");
        testCardDto.setHolder("Ivan Ivanov");
        testCardDto.setExpirationDate(LocalDate.of(2025, 12, 31));
        testCardDto.setBalance(BigDecimal.valueOf(1000.00));
        testCardDto.setStatus(Status.ACTIVE);
        testCardDto.setUserId(1L);
        // CVV should not be set in DTO
    }

    @Test
    void shouldMapCardToDto() {
        // When
        CardDto result = cardMapper.toDto(testCard);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(testCard.getId());
        assertThat(result.getNumber()).isEqualTo(testCard.getNumber());
        assertThat(result.getHolder()).isEqualTo(testCard.getHolder());
        assertThat(result.getExpirationDate()).isEqualTo(testCard.getExpirationDate());
        assertThat(result.getBalance()).isEqualTo(testCard.getBalance());
        assertThat(result.getStatus()).isEqualTo(testCard.getStatus());
        assertThat(result.getUserId()).isEqualTo(testUser.getId());
        
        // CRITICAL: CVV should never be mapped to DTO
        assertThat(result.getCvv()).isNull();
    }

    @Test
    void shouldMapCardToDtoWithNullUser() {
        // Given
        testCard.setUser(null);

        // When
        CardDto result = cardMapper.toDto(testCard);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getUserId()).isNull();
        assertThat(result.getCvv()).isNull();
    }

    @Test
    void shouldMapDtoToEntity() {
        // When
        Card result = cardMapper.toEntity(testCardDto);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(testCardDto.getId());
        assertThat(result.getNumber()).isEqualTo(testCardDto.getNumber());
        assertThat(result.getHolder()).isEqualTo(testCardDto.getHolder());
        assertThat(result.getExpirationDate()).isEqualTo(testCardDto.getExpirationDate());
        assertThat(result.getBalance()).isEqualTo(testCardDto.getBalance());
        assertThat(result.getStatus()).isEqualTo(testCardDto.getStatus());
        
        // Ignored fields should be null
        assertThat(result.getUser()).isNull();
        assertThat(result.getCvv()).isNull();
    }

    @Test
    void shouldUpdateCardFromDto() {
        // Given
        CardDto updateDto = new CardDto();
        updateDto.setHolder("Natasha Rostova");
        updateDto.setBalance(BigDecimal.valueOf(2000.00));
        updateDto.setStatus(Status.BLOCKED);
        updateDto.setCvv("456"); // This should be ignored

        Card originalCard = new Card();
        originalCard.setId(1L);
        originalCard.setNumber("1234567890123456");
        originalCard.setHolder("Ivan Ivanov");
        originalCard.setBalance(BigDecimal.valueOf(1000.00));
        originalCard.setStatus(Status.ACTIVE);
        originalCard.setCvv("123");
        originalCard.setUser(testUser);

        // When
        cardMapper.updateCardFromDto(updateDto, originalCard);

        // Then
        assertThat(originalCard.getHolder()).isEqualTo("Natasha Rostova");
        assertThat(originalCard.getBalance()).isEqualTo(BigDecimal.valueOf(2000.00));
        assertThat(originalCard.getStatus()).isEqualTo(Status.BLOCKED);
        
        // Ignored fields should remain unchanged
        assertThat(originalCard.getId()).isEqualTo(1L);
        assertThat(originalCard.getNumber()).isEqualTo("1234567890123456");
        assertThat(originalCard.getUser()).isEqualTo(testUser);
        assertThat(originalCard.getCvv()).isEqualTo("123"); // CVV should not be updated
    }

    @Test
    void shouldUpdateCardFromDtoWithNullValues() {
        // Given
        CardDto updateDto = new CardDto();
        updateDto.setHolder("Natasha Rostova");
        // balance and status are null

        Card originalCard = new Card();
        originalCard.setId(1L);
        originalCard.setHolder("Ivan Ivanov");
        originalCard.setBalance(BigDecimal.valueOf(1000.00));
        originalCard.setStatus(Status.ACTIVE);

        // When
        cardMapper.updateCardFromDto(updateDto, originalCard);

        // Then
        assertThat(originalCard.getHolder()).isEqualTo("Natasha Rostova");
        // Null values should be ignored
        assertThat(originalCard.getBalance()).isEqualTo(BigDecimal.valueOf(1000.00));
        assertThat(originalCard.getStatus()).isEqualTo(Status.ACTIVE);
    }

    @Test
    void shouldMapCardListToDto() {
        // Given
        Card card1 = new Card();
        card1.setId(1L);
        card1.setNumber("1111222233334444");
        card1.setHolder("Ivan Ivanov");
        card1.setCvv("123");
        card1.setUser(testUser);

        Card card2 = new Card();
        card2.setId(2L);
        card2.setNumber("5555666677778888");
        card2.setHolder("Natasha Rostova");
        card2.setCvv("456");
        card2.setUser(testUser);

        List<Card> cards = Arrays.asList(card1, card2);

        // When
        List<CardDto> result = cardMapper.cardsToDto(cards);

        // Then
        assertThat(result).hasSize(2);
        
        CardDto dto1 = result.get(0);
        assertThat(dto1.getId()).isEqualTo(1L);
        assertThat(dto1.getNumber()).isEqualTo("1111222233334444");
        assertThat(dto1.getHolder()).isEqualTo("Ivan Ivanov");
        assertThat(dto1.getUserId()).isEqualTo(testUser.getId());
        assertThat(dto1.getCvv()).isNull(); // CVV should not be mapped

        CardDto dto2 = result.get(1);
        assertThat(dto2.getId()).isEqualTo(2L);
        assertThat(dto2.getNumber()).isEqualTo("5555666677778888");
        assertThat(dto2.getHolder()).isEqualTo("Natasha Rostova");
        assertThat(dto2.getCvv()).isNull(); // CVV should not be mapped
    }

    @Test
    void shouldMapCardDtoListToEntity() {
        // Given
        CardDto dto1 = new CardDto();
        dto1.setId(1L);
        dto1.setNumber("1111222233334444");
        dto1.setHolder("Ivan Ivanov");
        dto1.setCvv("123"); // This should be ignored

        CardDto dto2 = new CardDto();
        dto2.setId(2L);
        dto2.setNumber("5555666677778888");
        dto2.setHolder("Natasha Rostova");
        dto2.setCvv("456"); // This should be ignored

        List<CardDto> cardDtoList = Arrays.asList(dto1, dto2);

        // When
        List<Card> result = cardMapper.cardsToEntity(cardDtoList);

        // Then
        assertThat(result).hasSize(2);
        
        Card card1 = result.get(0);
        assertThat(card1.getId()).isEqualTo(1L);
        assertThat(card1.getNumber()).isEqualTo("1111222233334444");
        assertThat(card1.getHolder()).isEqualTo("Ivan Ivanov");
        assertThat(card1.getUser()).isNull(); // User should not be mapped
        assertThat(card1.getCvv()).isNull(); // CVV should not be mapped

        Card card2 = result.get(1);
        assertThat(card2.getId()).isEqualTo(2L);
        assertThat(card2.getNumber()).isEqualTo("5555666677778888");
        assertThat(card2.getHolder()).isEqualTo("Natasha Rostova");
        assertThat(card2.getCvv()).isNull(); // CVV should not be mapped
    }

    @Test
    void shouldHandleNullCardInToDto() {
        // When
        CardDto result = cardMapper.toDto(null);

        // Then
        assertThat(result).isNull();
    }

    @Test
    void shouldHandleNullDtoInToEntity() {
        // When
        Card result = cardMapper.toEntity(null);

        // Then
        assertThat(result).isNull();
    }

    @Test
    void shouldNeverMapCvvInAnyDirection() {
        // Given
        Card cardWithCvv = new Card();
        cardWithCvv.setId(1L);
        cardWithCvv.setNumber("1234567890123456");
        cardWithCvv.setCvv("123");

        CardDto dtoWithCvv = new CardDto();
        dtoWithCvv.setId(1L);
        dtoWithCvv.setNumber("1234567890123456");
        dtoWithCvv.setCvv("456");

        // When mapping entity to DTO
        CardDto resultDto = cardMapper.toDto(cardWithCvv);
        
        // When mapping DTO to entity
        Card resultEntity = cardMapper.toEntity(dtoWithCvv);

        // Then - CVV should NEVER be mapped in any direction
        assertThat(resultDto.getCvv()).isNull();
        assertThat(resultEntity.getCvv()).isNull();
    }

    @Test
    void shouldPreserveCvvDuringUpdate() {
        // Given
        Card existingCard = new Card();
        existingCard.setId(1L);
        existingCard.setNumber("1234567890123456");
        existingCard.setHolder("Ivan Ivanov");
        existingCard.setCvv("123");

        CardDto updateDto = new CardDto();
        updateDto.setHolder("Natasha Rostova");
        updateDto.setCvv("456"); // This should be ignored

        // When
        cardMapper.updateCardFromDto(updateDto, existingCard);

        // Then
        assertThat(existingCard.getHolder()).isEqualTo("Natasha Rostova");
        assertThat(existingCard.getCvv()).isEqualTo("123"); // Original CVV preserved
    }
}