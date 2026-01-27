package com.example.bankcards.dto;

import jakarta.validation.constraints.*;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class CardOperationRequest {
    @NotNull(message = "Card ID is required")
    private Long cardId;

    @NotNull(message = "Amount is required")
    @DecimalMin(value = "0.01", message = "Amount must be greater than 0")
    @Digits(integer = 10, fraction = 2, message = "Amount format is invalid")
    private BigDecimal amount;

    @Size(max = 255, message = "Description cannot exceed 255 characters")
    private String description;

    // Для операций, требующих подтверждения (например, снятие средств)
    @Size(min = 3, max = 3, message = "CVV must be exactly 3 digits")
    @Pattern(regexp = "\\d{3}", message = "CVV must contain only digits")
    private String cvv;
}