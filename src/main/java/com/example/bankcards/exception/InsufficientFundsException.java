package com.example.bankcards.exception;

/**
 * Exception thrown when a transfer cannot be completed due to insufficient funds on the sender's card.
 * This is a business logic exception that occurs during transfer validation when the card balance
 * is less than the requested transfer amount.
 */
public class InsufficientFundsException extends RuntimeException {
    /**
     * Creates a new InsufficientFundsException with the specified error message.
     * 
     * @param message detailed error message explaining the insufficient funds situation
     */
    public InsufficientFundsException(String message) {
        super(message);
    }
}