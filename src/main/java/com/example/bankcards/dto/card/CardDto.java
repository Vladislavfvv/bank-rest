package com.example.bankcards.dto.card;

import com.example.bankcards.entity.Status;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * DTO for card information transfer between layers.
 * Used for API responses and internal data exchange.
 * Contains card details with validation constraints and security considerations.
 * CVV is excluded from JSON serialization for security reasons.
 */
@Data
public class CardDto {
    private Long id;

    private Long userId;

    @NotBlank(message = "Card number must not be blank")
    @Size(min = 16, max = 16, message = "Card number must be 16 digits")
    private String number;

    @NotBlank(message = "Card holder name must not be blank")
    private String holder;

    @NotNull(message = "Expiration date is required")
    @Future(message = "Expiration date must be in the future")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
    private LocalDate expirationDate;

    @NotNull(message = "Balance is required")
    @DecimalMin(value = "0.0", message = "Balance cannot be negative")
    @Digits(integer = 13, fraction = 2, message = "Balance format is invalid")
    private BigDecimal balance;

    @NotNull(message = "Status is required")
    private Status status;

    // Masked card number (for display only)
    private String maskedNumber;

    // Masked expiration date MM/YY (for display only)
    private String maskedExpirationDate;

    // CVV is NOT included in DTO for security reasons
    @JsonIgnore
    private String cvv; // For internal use only, if necessary
}
