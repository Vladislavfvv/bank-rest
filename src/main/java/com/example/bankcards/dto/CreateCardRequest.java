package com.example.bankcards.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CreateCardRequest {
    @NotBlank(message = "Card holder name is required")
    @Size(min = 2, max = 100, message = "Card holder name must be between 2 and 100 characters")
    private String holder;

    // Номер карты, дата истечения, CVV и баланс будут генерироваться автоматически
    // при создании карты в сервисе
}