package com.example.bankcards.dto.card;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * DTO for card block request creation.
 * Used to receive and validate card blocking request data from API clients.
 * Contains reason for blocking with validation constraints.
 */
@Data
public class CreateBlockRequest {
    @NotBlank(message = "Reason for blocking is required")
    @Size(min = 5, max = 500, message = "Reason must be between 5 and 500 characters")
    private String reason;
}
