package com.example.bankcards.util;

import com.example.bankcards.entity.Card;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Utility class for card number masking operations.
 * Provides secure masking of sensitive card information.
 */
@Component
@RequiredArgsConstructor
public class CardMaskingUtils {

    private final EncryptionService encryptionService;

    /**
     * Masks card number showing only last 4 digits.
     * Format: "**** **** **** 1234"
     * 
     * @param card the card entity with encrypted number
     * @return masked card number string
     */
    public String getMaskedNumber(Card card) {
        if (card == null || card.getNumber() == null) {
            return "****";
        }
        
        String decryptedNumber = encryptionService.decrypt(card.getNumber());
        if (decryptedNumber == null || decryptedNumber.length() < 4) {
            return "****";
        }
        
        return "**** **** **** " + decryptedNumber.substring(decryptedNumber.length() - 4);
    }

    /**
     * Masks card number from plain text.
     * Format: "**** **** **** 1234"
     * 
     * @param cardNumber plain text card number
     * @return masked card number string
     */
    public String getMaskedNumber(String cardNumber) {
        if (cardNumber == null || cardNumber.length() < 4) {
            return "****";
        }
        
        return "**** **** **** " + cardNumber.substring(cardNumber.length() - 4);
    }
}