package com.example.bankcards.dto.transfer;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;

/**
 * DTO for transfer request creation.
 * Used to receive and validate transfer data from API clients.
 * Contains all required fields for money transfer with comprehensive validation.
 * CVV field is optional for additional security verification.
 */
@Data
public class TransferRequest {
    
    @NotNull(message = "Sender card ID is required")
    @Schema(description = "ID of the sender card", example = "1")
    private Long fromCardId;

    @NotNull(message = "Recipient card ID is required")
    @Schema(description = "ID of the recipient card", example = "2")
    private Long toCardId;

    @NotNull(message = "Transfer amount is required")
    @DecimalMin(value = "0.01", message = "Amount must be greater than 0")
    @Digits(integer = 13, fraction = 2, message = "Invalid amount format")
    @Schema(description = "Transfer amount", example = "100.50")
    private BigDecimal amount;

    @Size(max = 255, message = "Description cannot exceed 255 characters")
    @Schema(description = "Transfer description", example = "Payment for services")
    private String description;

    // CVV is optional for additional security verification
    @Size(min = 3, max = 3, message = "CVV must contain exactly 3 digits")
    @Pattern(regexp = "\\d{3}", message = "CVV must contain only digits")
    @Schema(description = "Optional CVV for additional security", example = "123", hidden = true)
    private String cvv;
}