package com.example.bankcards.dto.transfer;

import jakarta.validation.constraints.*;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class TransferRequest {
    
    @NotNull(message = "ID карты отправителя обязателен")
    private Long fromCardId;

    @NotNull(message = "ID карты получателя обязателен")
    private Long toCardId;

    @NotNull(message = "Сумма перевода обязательна")
    @DecimalMin(value = "0.01", message = "Сумма должна быть больше 0")
    @Digits(integer = 13, fraction = 2, message = "Неверный формат суммы")
    private BigDecimal amount;

    @Size(max = 255, message = "Описание не может превышать 255 символов")
    private String description;

    @NotBlank(message = "CVV код обязателен для подтверждения перевода")
    @Size(min = 3, max = 3, message = "CVV должен содержать ровно 3 цифры")
    @Pattern(regexp = "\\d{3}", message = "CVV должен содержать только цифры")
    private String cvv;
}