package com.example.bankcards.dto.transfer;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CardTransferStatsDto {
    private Long cardId;
    private String cardMaskedNumber;
    private BigDecimal totalIncome;    // Общий приход (когда карта получатель)
    private BigDecimal totalExpense;   // Общий расход (когда карта отправитель)
    private BigDecimal balance;        // Текущий баланс карты
    private Long incomeTransfersCount; // Количество входящих переводов
    private Long expenseTransfersCount; // Количество исходящих переводов
}