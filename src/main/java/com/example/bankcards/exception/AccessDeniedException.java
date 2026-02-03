package com.example.bankcards.exception;

/**
 * Exception thrown when user tries to access resources they don't have permission for.
 * This is different from InvalidTransferException which is for business logic violations.
 */
public class AccessDeniedException extends RuntimeException {
    public AccessDeniedException(String message) {
        super(message);
    }
}