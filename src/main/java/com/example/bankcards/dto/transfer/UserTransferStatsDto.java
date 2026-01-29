package com.example.bankcards.dto.transfer;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserTransferStatsDto {
    private Long userId;
    private String userEmail;
    private String userFullName;
    private BigDecimal totalIncome;    // Общий приход по всем картам
    private BigDecimal totalExpense;   // Общий расход по всем картам
    private BigDecimal totalBalance;   // Общий баланс по всем картам
    private List<CardTransferStatsDto> cardStats; // Статистика в разрезе карт
}