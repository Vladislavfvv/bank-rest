package com.example.bankcards.util;

import com.example.bankcards.dto.transfer.TransferDto;
import com.example.bankcards.entity.Card;
import com.example.bankcards.entity.Status;
import com.example.bankcards.entity.Transfer;
import com.example.bankcards.entity.TransferStatus;
import com.example.bankcards.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mapstruct.factory.Mappers;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

class TransferMapperTest {

    private TransferMapper transferMapper;
    private Transfer testTransfer;
    private Card fromCard;
    private Card toCard;
    private User fromUser;

    @BeforeEach
    void setUp() {
        transferMapper = Mappers.getMapper(TransferMapper.class);
        
        // Create test users
        fromUser = new User();
        fromUser.setId(1L);
        fromUser.setEmail("ivan.ivanov@example.com");
        fromUser.setFirstName("Ivan");
        fromUser.setLastName("Ivanov");

        User toUser = new User();
        toUser.setId(2L);
        toUser.setEmail("to@example.com");
        toUser.setFirstName("Natasha");
        toUser.setLastName("Rostova");

        // Create test cards
        fromCard = new Card();
        fromCard.setId(1L);
        fromCard.setNumber("1234567890123456");
        fromCard.setHolder("Ivan Ivanov");
        fromCard.setExpirationDate(LocalDate.of(2025, 12, 31));
        fromCard.setCvv("123");
        fromCard.setBalance(BigDecimal.valueOf(1000.00));
        fromCard.setStatus(Status.ACTIVE);
        fromCard.setUser(fromUser);

        toCard = new Card();
        toCard.setId(2L);
        toCard.setNumber("9876543210987654");
        toCard.setHolder("Natasha Rostova");
        toCard.setExpirationDate(LocalDate.of(2026, 6, 30));
        toCard.setCvv("456");
        toCard.setBalance(BigDecimal.valueOf(500.00));
        toCard.setStatus(Status.ACTIVE);
        toCard.setUser(toUser);

        // Create test transfer
        testTransfer = new Transfer();
        testTransfer.setId(1L);
        testTransfer.setFromCard(fromCard);
        testTransfer.setToCard(toCard);
        testTransfer.setAmount(BigDecimal.valueOf(100.00));
        testTransfer.setDescription("Test transfer");
        testTransfer.setTransferDate(LocalDateTime.of(2024, 1, 15, 10, 30, 0));
        testTransfer.setStatus(TransferStatus.COMPLETED);
    }

    @Test
    void shouldMapTransferToDto() {
        // When
        TransferDto result = transferMapper.toDto(testTransfer);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(testTransfer.getId());
        assertThat(result.getFromCardId()).isEqualTo(fromCard.getId());
        // Masked numbers are ignored in mapper (set manually in TransferService due to encryption)
        assertThat(result.getFromCardMaskedNumber()).isNull();
        assertThat(result.getToCardId()).isEqualTo(toCard.getId());
        assertThat(result.getToCardMaskedNumber()).isNull();
        assertThat(result.getAmount()).isEqualTo(testTransfer.getAmount());
        assertThat(result.getDescription()).isEqualTo(testTransfer.getDescription());
        assertThat(result.getTransferDate()).isEqualTo(testTransfer.getTransferDate());
        assertThat(result.getStatus()).isEqualTo(testTransfer.getStatus());
    }

    @ParameterizedTest(name = "{2}")
    @MethodSource("cardNumberTestData")
    void shouldMapTransferToDtoWithVariousCardNumbers(String fromCardNumber, String toCardNumber, String testDescription) {
        // Given
        fromCard.setNumber(fromCardNumber);
        toCard.setNumber(toCardNumber);

        // When
        TransferDto result = transferMapper.toDto(testTransfer);

        // Then
        assertThat(result).isNotNull();
        // Masked numbers are ignored in mapper (set manually in TransferService due to encryption)
        assertThat(result.getFromCardMaskedNumber()).isNull();
        assertThat(result.getToCardMaskedNumber()).isNull();
    }

    private static Stream<Arguments> cardNumberTestData() {
        return Stream.of(
            Arguments.of("1111222233334444", "5555666677778888", "Different card numbers"),
            Arguments.of("123", "456", "Short card numbers"),
            Arguments.of(null, null, "Null card numbers")
        );
    }

    @Test
    void shouldMapTransferToDtoWithNullDescription() {
        // Given
        testTransfer.setDescription(null);

        // When
        TransferDto result = transferMapper.toDto(testTransfer);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getDescription()).isNull();
        assertThat(result.getId()).isEqualTo(testTransfer.getId());
        assertThat(result.getAmount()).isEqualTo(testTransfer.getAmount());
    }

    @Test
    void shouldMapTransferToDtoWithDifferentStatuses() {
        // Test FAILED status
        testTransfer.setStatus(TransferStatus.FAILED);
        TransferDto result1 = transferMapper.toDto(testTransfer);
        assertThat(result1.getStatus()).isEqualTo(TransferStatus.FAILED);

        // Test COMPLETED status
        testTransfer.setStatus(TransferStatus.COMPLETED);
        TransferDto result2 = transferMapper.toDto(testTransfer);
        assertThat(result2.getStatus()).isEqualTo(TransferStatus.COMPLETED);
    }

    @Test
    void shouldMapTransferToDtoWithZeroAmount() {
        // Given
        testTransfer.setAmount(BigDecimal.ZERO);

        // When
        TransferDto result = transferMapper.toDto(testTransfer);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getAmount()).isEqualTo(BigDecimal.ZERO);
    }

    @Test
    void shouldMapTransferToDtoWithLargeAmount() {
        // Given
        BigDecimal largeAmount = new BigDecimal("999999999999.99");
        testTransfer.setAmount(largeAmount);

        // When
        TransferDto result = transferMapper.toDto(testTransfer);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getAmount()).isEqualTo(largeAmount);
    }

    @Test
    void shouldMapTransferToDtoWithSameUserCards() {
        // Given - both cards belong to the same user
        toCard.setUser(fromUser);

        // When
        TransferDto result = transferMapper.toDto(testTransfer);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getFromCardId()).isEqualTo(fromCard.getId());
        assertThat(result.getToCardId()).isEqualTo(toCard.getId());
        // Masked numbers are ignored in mapper (set manually in TransferService due to encryption)
        assertThat(result.getFromCardMaskedNumber()).isNull();
        assertThat(result.getToCardMaskedNumber()).isNull();
    }

    @Test
    void shouldHandleNullTransfer() {
        // When
        TransferDto result = transferMapper.toDto(null);

        // Then
        assertThat(result).isNull();
    }

    @Test
    void shouldMapTransferToDtoWithLongDescription() {
        // Given
        String longDescription = "This is a very long description that contains many characters and should be handled properly by the mapper without any issues or truncation problems";
        testTransfer.setDescription(longDescription);

        // When
        TransferDto result = transferMapper.toDto(testTransfer);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getDescription()).isEqualTo(longDescription);
    }

    @Test
    void shouldMapTransferToDtoWithSpecialCharactersInDescription() {
        // Given
        String specialDescription = "Transfer with special chars: @#$%^&*()_+-=[]{}|;':\",./<>?";
        testTransfer.setDescription(specialDescription);

        // When
        TransferDto result = transferMapper.toDto(testTransfer);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getDescription()).isEqualTo(specialDescription);
    }

    @Test
    void shouldMapTransferToDtoWithEmptyDescription() {
        // Given
        testTransfer.setDescription("");

        // When
        TransferDto result = transferMapper.toDto(testTransfer);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getDescription()).isEmpty();
    }

    @Test
    void shouldMapTransferToDtoPreservingPrecision() {
        // Given
        BigDecimal preciseAmount = new BigDecimal("123.45");
        testTransfer.setAmount(preciseAmount);

        // When
        TransferDto result = transferMapper.toDto(testTransfer);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getAmount()).isEqualTo(preciseAmount);
        assertThat(result.getAmount().scale()).isEqualTo(2);
    }
}