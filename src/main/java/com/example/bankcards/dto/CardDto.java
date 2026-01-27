package com.example.bankcards.dto;

import com.example.bankcards.entity.Status;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.validation.constraints.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

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

    // Маскированный номер карты (только для отображения)
    private String maskedNumber;

    // Маскированная дата истечения MM/YY (только для отображения)
    private String maskedExpirationDate;

    // CVV НЕ включаем в DTO по соображениям безопасности
    @JsonIgnore
    private String cvv; // Только для внутреннего использования, если необходимо
}
