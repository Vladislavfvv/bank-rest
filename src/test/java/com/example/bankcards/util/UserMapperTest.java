package com.example.bankcards.util;

import com.example.bankcards.dto.card.CardDto;
import com.example.bankcards.dto.user.UserDto;
import com.example.bankcards.entity.Card;
import com.example.bankcards.entity.Status;
import com.example.bankcards.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class UserMapperTest {

    private UserMapper userMapper;
    private UserDto testUserDto;

    @BeforeEach
    void setUp() {
        userMapper = Mappers.getMapper(UserMapper.class);
        
        // Create test DTO
        testUserDto = new UserDto();
        testUserDto.setId(1L);
        testUserDto.setEmail("test@example.com");
        testUserDto.setFirstName("Ivan");
        testUserDto.setLastName("Ivanov");
        testUserDto.setBirthDate(LocalDate.of(1990, 1, 1));
    }

    @Test
    void shouldMapDtoToEntity() {
        // When
        User result = userMapper.toEntity(testUserDto);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(testUserDto.getId());
        assertThat(result.getEmail()).isEqualTo(testUserDto.getEmail());
        assertThat(result.getFirstName()).isEqualTo(testUserDto.getFirstName());
        assertThat(result.getLastName()).isEqualTo(testUserDto.getLastName());
        assertThat(result.getBirthDate()).isEqualTo(testUserDto.getBirthDate());
        
        // Ignored fields should have default values or be null
        assertThat(result.getCreatedAt()).isNotNull(); // Now has default value due to @Builder.Default
        assertThat(result.getIsActive()).isTrue(); // Now has default value due to @Builder.Default
        assertThat(result.getCards()).isNotNull().isEmpty(); // Now has default value due to @Builder.Default
    }

    @Test
    void shouldUpdateUserFromDto() {
        // Given
        UserDto updateDto = new UserDto();
        updateDto.setFirstName("Natasha");
        updateDto.setLastName("Rostova");
        updateDto.setBirthDate(LocalDate.of(1985, 5, 15));

        User originalUser = new User();
        originalUser.setId(1L);
        originalUser.setEmail("original@example.com");
        originalUser.setFirstName("Original");
        originalUser.setLastName("User");
        originalUser.setBirthDate(LocalDate.of(1980, 1, 1));
        originalUser.setCreatedAt(LocalDateTime.now());
        originalUser.setIsActive(true);

        // When
        userMapper.updateUserFromDto(updateDto, originalUser);

        // Then
        assertThat(originalUser.getFirstName()).isEqualTo("Natasha");
        assertThat(originalUser.getLastName()).isEqualTo("Rostova");
        assertThat(originalUser.getBirthDate()).isEqualTo(LocalDate.of(1985, 5, 15));
        
        // Ignored fields should remain unchanged
        assertThat(originalUser.getId()).isEqualTo(1L);
        assertThat(originalUser.getEmail()).isEqualTo("original@example.com");
        assertThat(originalUser.getCreatedAt()).isNotNull();
        assertThat(originalUser.getIsActive()).isTrue();
    }

    @Test
    void shouldUpdateUserFromDtoWithNullValues() {
        // Given
        UserDto updateDto = new UserDto();
        updateDto.setFirstName("Natasha");
        // lastName and birthDate are null

        User originalUser = new User();
        originalUser.setId(1L);
        originalUser.setFirstName("Original");
        originalUser.setLastName("User");
        originalUser.setBirthDate(LocalDate.of(1980, 1, 1));

        // When
        userMapper.updateUserFromDto(updateDto, originalUser);

        // Then
        assertThat(originalUser.getFirstName()).isEqualTo("Natasha");
        // Null values should be ignored
        assertThat(originalUser.getLastName()).isEqualTo("User");
        assertThat(originalUser.getBirthDate()).isEqualTo(LocalDate.of(1980, 1, 1));
    }

    @Test
    void shouldUpdateCardsWithNewCards() {
        // Given
        User user = new User();
        user.setCards(new ArrayList<>());

        CardDto cardDto1 = new CardDto();
        cardDto1.setId(1L);
        cardDto1.setNumber("1111222233334444");
        cardDto1.setHolder("Ivan Ivanov");
        cardDto1.setExpirationDate(LocalDate.of(2025, 12, 31));
        cardDto1.setBalance(BigDecimal.valueOf(1000.00));
        cardDto1.setStatus(Status.ACTIVE);

        CardDto cardDto2 = new CardDto();
        cardDto2.setId(2L);
        cardDto2.setNumber("5555666677778888");
        cardDto2.setHolder("Ivan Ivanov");
        cardDto2.setExpirationDate(LocalDate.of(2026, 6, 30));
        cardDto2.setBalance(BigDecimal.valueOf(500.00));
        cardDto2.setStatus(Status.BLOCKED);

        List<CardDto> cardDtoList = Arrays.asList(cardDto1, cardDto2);

        // When
        List<Card> result = userMapper.updateCards(user, cardDtoList);

        // Then
        assertThat(result).hasSize(2);
        
        Card resultCard1 = result.get(0);
        assertThat(resultCard1.getId()).isEqualTo(1L);
        assertThat(resultCard1.getNumber()).isEqualTo("1111222233334444");
        assertThat(resultCard1.getHolder()).isEqualTo("Ivan Ivanov");
        assertThat(resultCard1.getBalance()).isEqualTo(BigDecimal.valueOf(1000.00));
        assertThat(resultCard1.getStatus()).isEqualTo(Status.ACTIVE);
        assertThat(resultCard1.getUser()).isEqualTo(user);
        assertThat(resultCard1.getCvv()).isNull(); // CVV should not be set from DTO

        Card resultCard2 = result.get(1);
        assertThat(resultCard2.getId()).isEqualTo(2L);
        assertThat(resultCard2.getNumber()).isEqualTo("5555666677778888");
        assertThat(resultCard2.getStatus()).isEqualTo(Status.BLOCKED);
        assertThat(resultCard2.getCvv()).isNull(); // CVV should not be set from DTO
    }

    @Test
    void shouldUpdateCardsWithExistingCards() {
        // Given
        Card existingCard = new Card();
        existingCard.setId(1L);
        existingCard.setNumber("1111222233334444");
        existingCard.setHolder("OLD HOLDER");
        existingCard.setBalance(BigDecimal.valueOf(500.00));
        existingCard.setStatus(Status.BLOCKED);
        existingCard.setCvv("123"); // Existing CVV should be preserved

        User user = new User();
        user.setCards(List.of(existingCard));

        CardDto updateDto = new CardDto();
        updateDto.setId(1L);
        updateDto.setNumber("1111222233334444");
        updateDto.setHolder("NEW HOLDER");
        updateDto.setBalance(BigDecimal.valueOf(1000.00));
        updateDto.setStatus(Status.ACTIVE);
        // CVV is not set in DTO

        // When
        List<Card> result = userMapper.updateCards(user, List.of(updateDto));

        // Then
        assertThat(result).hasSize(1);
        
        Card updatedCard = result.get(0);
        assertThat(updatedCard.getId()).isEqualTo(1L);
        assertThat(updatedCard.getHolder()).isEqualTo("NEW HOLDER");
        assertThat(updatedCard.getBalance()).isEqualTo(BigDecimal.valueOf(1000.00));
        assertThat(updatedCard.getStatus()).isEqualTo(Status.ACTIVE);
        assertThat(updatedCard.getCvv()).isEqualTo("123"); // CVV should be preserved
    }

    @Test
    void shouldUpdateCardsWithMixedNewAndExisting() {
        // Given
        Card existingCard = new Card();
        existingCard.setId(1L);
        existingCard.setNumber("1111222233334444");
        existingCard.setCvv("123");

        User user = new User();
        user.setCards(List.of(existingCard));

        CardDto existingCardDto = new CardDto();
        existingCardDto.setId(1L);
        existingCardDto.setHolder("UPDATED HOLDER");

        CardDto newCardDto = new CardDto();
        newCardDto.setId(2L);
        newCardDto.setNumber("5555666677778888");
        newCardDto.setHolder("NEW HOLDER");

        // When
        List<Card> result = userMapper.updateCards(user, List.of(existingCardDto, newCardDto));

        // Then
        assertThat(result).hasSize(2);
        
        // Check existing card was updated
        Card updatedExisting = result.stream()
                .filter(c -> c.getId().equals(1L))
                .findFirst()
                .orElseThrow();
        assertThat(updatedExisting.getHolder()).isEqualTo("UPDATED HOLDER");
        assertThat(updatedExisting.getCvv()).isEqualTo("123"); // CVV preserved
        
        // Check new card was created
        Card newCard = result.stream()
                .filter(c -> c.getId().equals(2L))
                .findFirst()
                .orElseThrow();
        assertThat(newCard.getHolder()).isEqualTo("NEW HOLDER");
        assertThat(newCard.getUser()).isEqualTo(user);
        assertThat(newCard.getCvv()).isNull(); // CVV not set from DTO
    }

    @Test
    void shouldReturnEmptyListWhenCardDtoListIsNull() {
        // Given
        User user = new User();

        // When
        List<Card> result = userMapper.updateCards(user, null);

        // Then
        assertThat(result).isEmpty();
    }

    @Test
    void shouldReturnEmptyListWhenCardDtoListIsEmpty() {
        // Given
        User user = new User();

        // When
        List<Card> result = userMapper.updateCards(user, new ArrayList<>());

        // Then
        assertThat(result).isEmpty();
    }

    @Test
    void shouldHandleUserWithNullCards() {
        // Given
        User user = new User();
        user.setCards(null);

        CardDto cardDto = new CardDto();
        cardDto.setId(1L);
        cardDto.setNumber("1111222233334444");

        // When
        List<Card> result = userMapper.updateCards(user, List.of(cardDto));

        // Then
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getId()).isEqualTo(1L);
        assertThat(result.get(0).getUser()).isEqualTo(user);
    }

    @Test
    void shouldNeverUpdateCvvFromDto() {
        // Given
        Card existingCard = new Card();
        existingCard.setId(1L);
        existingCard.setCvv("123");

        User user = new User();
        user.setCards(List.of(existingCard));

        CardDto cardDto = new CardDto();
        cardDto.setId(1L);
        cardDto.setCvv("456"); // This should be ignored

        // When
        List<Card> result = userMapper.updateCards(user, List.of(cardDto));

        // Then
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getCvv()).isEqualTo("123"); // Original CVV preserved
    }

    @Test
    void shouldHandleNullDto() {
        // When
        User result = userMapper.toEntity(null);

        // Then
        assertThat(result).isNull();
    }

    // Note: Tests for toDto() method are skipped because UserMapper depends on CardMapper
    // which is not available in lightweight test setup. In a real Spring context,
    // these would work properly with dependency injection.
}