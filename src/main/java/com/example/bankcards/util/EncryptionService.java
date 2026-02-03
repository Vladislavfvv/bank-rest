package com.example.bankcards.util;

import com.example.bankcards.exception.EncryptionException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * Service for encrypting and decrypting sensitive card data (card numbers and CVV).
 * Uses AES-256 encryption algorithm.
 */
@Slf4j
@Service
public class EncryptionService {

    private static final String ALGORITHM = "AES";
    private static final String TRANSFORMATION = "AES";
    private static final int KEY_SIZE = 256;
    private static final int KEY_SIZE_BYTES = KEY_SIZE / 8; // 256 bits = 32 bytes

    private final SecretKey secretKey;

    public EncryptionService(@Value("${encryption.secret-key:}") String secretKeyString) {
        if (secretKeyString == null || secretKeyString.trim().isEmpty()) {
            log.warn("Encryption secret key is not configured. Generating a new key. " +
                    "WARNING: This key will be different on each application restart!");
            this.secretKey = generateSecretKey();
        } else {
            // Use provided key (must be 32 bytes for AES-256)
            byte[] keyBytes = secretKeyString.getBytes(StandardCharsets.UTF_8);
            if (keyBytes.length != KEY_SIZE_BYTES) {
                log.warn("Secret key length is not {} bytes. Padding or truncating to {} bytes.", 
                        KEY_SIZE_BYTES, KEY_SIZE_BYTES);
                keyBytes = adjustKeyLength(keyBytes);
            }
            this.secretKey = new SecretKeySpec(keyBytes, ALGORITHM);
        }
    }

    /**
     * Encrypts the given plain text using AES encryption.
     *
     * @param plainText the text to encrypt
     * @return Base64 encoded encrypted string
     */
    public String encrypt(String plainText) {
        if (plainText == null || plainText.isEmpty()) {
            return plainText;
        }

        try {
            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.ENCRYPT_MODE, secretKey);
            byte[] encryptedBytes = cipher.doFinal(plainText.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(encryptedBytes);
        } catch (Exception e) {
            log.error("Error encrypting data", e);
            throw new EncryptionException("Failed to encrypt data", e);
        }
    }

    /**
     * Decrypts the given encrypted text using AES decryption.
     *
     * @param encryptedText Base64 encoded encrypted string
     * @return decrypted plain text
     */
    public String decrypt(String encryptedText) {
        if (encryptedText == null || encryptedText.isEmpty()) {
            return encryptedText;
        }

        try {
            // Check if the text is already encrypted (Base64 format)
            // If not, assume it's plain text (for backward compatibility with existing data)
            if (!isBase64(encryptedText)) {
                log.debug("Text is not encrypted (not Base64), returning as-is for backward compatibility");
                return encryptedText;
            }

            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.DECRYPT_MODE, secretKey);
            byte[] decryptedBytes = cipher.doFinal(Base64.getDecoder().decode(encryptedText));
            return new String(decryptedBytes, StandardCharsets.UTF_8);
        } catch (Exception e) {
            log.error("Error decrypting data. Text might not be encrypted: {}", encryptedText, e);
            // For backward compatibility, if decryption fails, return original text
            return encryptedText;
        }
    }

    /**
     * Decrypts only the last N characters of the encrypted text.
     * Used for masking card numbers (showing only last 4 digits).
     *
     * @param encryptedText Base64 encoded encrypted string
     * @param lastChars number of last characters to decrypt
     * @return last N characters of decrypted text
     */
    public String decryptLastChars(String encryptedText, int lastChars) {
        String decrypted = decrypt(encryptedText);
        if (decrypted == null || decrypted.length() <= lastChars) {
            return decrypted;
        }
        return decrypted.substring(decrypted.length() - lastChars);
    }

    /**
     * Checks if a string is Base64 encoded.
     */
    private boolean isBase64(String str) {
        if (str == null || str.isEmpty()) {
            return false;
        }
        try {
            Base64.getDecoder().decode(str);
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    /**
     * Generates a new secret key for encryption.
     * WARNING: This key will be different on each application restart!
     */
    private SecretKey generateSecretKey() {
        try {
            KeyGenerator keyGenerator = KeyGenerator.getInstance(ALGORITHM);
            keyGenerator.init(KEY_SIZE, new SecureRandom());
            return keyGenerator.generateKey();
        } catch (Exception e) {
            log.error("Error generating secret key", e);
            throw new EncryptionException("Failed to generate secret key", e);
        }
    }

    /**
     * Adjusts key length to KEY_SIZE_BYTES (32 bytes for AES-256) by padding or truncating.
     */
    private byte[] adjustKeyLength(byte[] key) {
        byte[] adjustedKey = new byte[KEY_SIZE_BYTES];
        int copyLength = Math.min(key.length, KEY_SIZE_BYTES);
        System.arraycopy(key, 0, adjustedKey, 0, copyLength);
        // If key.length < KEY_SIZE_BYTES, remaining bytes are already zeros (default byte array value)
        return adjustedKey;
    }
}
