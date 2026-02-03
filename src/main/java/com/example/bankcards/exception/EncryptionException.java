package com.example.bankcards.exception;

/**
 * Exception thrown when encryption or decryption operations fail.
 */
public class EncryptionException extends RuntimeException {
    public EncryptionException(String message, Throwable cause) {
        super(message, cause);
    }
}
