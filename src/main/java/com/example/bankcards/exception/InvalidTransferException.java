package com.example.bankcards.exception;

/**
 * Exception thrown when a transfer request contains invalid data or violates business rules.
 * This includes scenarios like invalid card IDs, blocked cards, same source and destination cards,
 * invalid amounts, or other transfer validation failures not related to insufficient funds.
 */
public class InvalidTransferException extends RuntimeException {
    /**
     * Creates a new InvalidTransferException with the specified error message.
     * 
     * @param message detailed error message explaining what makes the transfer invalid
     */
    public InvalidTransferException(String message) {
        super(message);
    }
}