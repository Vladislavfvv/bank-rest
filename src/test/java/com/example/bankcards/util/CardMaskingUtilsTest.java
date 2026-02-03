package com.example.bankcards.util;

import com.example.bankcards.entity.Card;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("CardMaskingUtils Tests")
class CardMaskingUtilsTest {

    @Mock
    private EncryptionService encryptionService;

    @InjectMocks
    private CardMaskingUtils cardMaskingUtils;

    private Card testCard;

    @BeforeEach
    void setUp() {
        testCard = new Card();
        testCard.setNumber("encryptedCardNumber");
    }

    @Test
    @DisplayName("getMaskedNumber - With valid card - Should return masked number")
    void getMaskedNumber_WithValidCard_ShouldReturnMaskedNumber() {
        // given
        String decryptedNumber = "1234567890123456";
        when(encryptionService.decrypt("encryptedCardNumber")).thenReturn(decryptedNumber);

        // when
        String result = cardMaskingUtils.getMaskedNumber(testCard);

        // then
        assertEquals("**** **** **** 3456", result);
    }

    @Test
    @DisplayName("getMaskedNumber - With null card - Should return default mask")
    void getMaskedNumber_WithNullCard_ShouldReturnDefaultMask() {
        // when
        String result = cardMaskingUtils.getMaskedNumber((Card) null);

        // then
        assertEquals("****", result);
    }

    @Test
    @DisplayName("getMaskedNumber - With null card number - Should return default mask")
    void getMaskedNumber_WithNullCardNumber_ShouldReturnDefaultMask() {
        // given
        testCard.setNumber(null);

        // when
        String result = cardMaskingUtils.getMaskedNumber(testCard);

        // then
        assertEquals("****", result);
    }

    @Test
    @DisplayName("getMaskedNumber - With short decrypted number - Should return default mask")
    void getMaskedNumber_WithShortDecryptedNumber_ShouldReturnDefaultMask() {
        // given
        String shortNumber = "123";
        when(encryptionService.decrypt("encryptedCardNumber")).thenReturn(shortNumber);

        // when
        String result = cardMaskingUtils.getMaskedNumber(testCard);

        // then
        assertEquals("****", result);
    }

    @Test
    @DisplayName("getMaskedNumber - With null decrypted number - Should return default mask")
    void getMaskedNumber_WithNullDecryptedNumber_ShouldReturnDefaultMask() {
        // given
        when(encryptionService.decrypt("encryptedCardNumber")).thenReturn(null);

        // when
        String result = cardMaskingUtils.getMaskedNumber(testCard);

        // then
        assertEquals("****", result);
    }

    @Test
    @DisplayName("getMaskedNumber - With plain text card number - Should return masked number")
    void getMaskedNumber_WithPlainTextCardNumber_ShouldReturnMaskedNumber() {
        // given
        String cardNumber = "1234567890123456";

        // when
        String result = cardMaskingUtils.getMaskedNumber(cardNumber);

        // then
        assertEquals("**** **** **** 3456", result);
    }

    @Test
    @DisplayName("getMaskedNumber - With null plain text - Should return default mask")
    void getMaskedNumber_WithNullPlainText_ShouldReturnDefaultMask() {
        // when
        String result = cardMaskingUtils.getMaskedNumber((String) null);

        // then
        assertEquals("****", result);
    }

    @Test
    @DisplayName("getMaskedNumber - With short plain text - Should return default mask")
    void getMaskedNumber_WithShortPlainText_ShouldReturnDefaultMask() {
        // given
        String shortNumber = "123";

        // when
        String result = cardMaskingUtils.getMaskedNumber(shortNumber);

        // then
        assertEquals("****", result);
    }

    @Test
    @DisplayName("getMaskedNumber - With exactly 4 digits - Should return masked number")
    void getMaskedNumber_WithExactly4Digits_ShouldReturnMaskedNumber() {
        // given
        String cardNumber = "1234";

        // when
        String result = cardMaskingUtils.getMaskedNumber(cardNumber);

        // then
        assertEquals("**** **** **** 1234", result);
    }
}