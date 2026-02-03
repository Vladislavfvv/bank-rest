package com.example.bankcards.dto.transfer;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * DTO for card transfer statistics.
 * Contains aggregated transfer data for a specific card including income, expense, and balance.
 * Used for financial reporting and analytics by card.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CardTransferStatsDto {
    private Long cardId;
    private String cardMaskedNumber;
    private BigDecimal totalIncome;    // Total income (when card is recipient)
    private BigDecimal totalExpense;   // Total expense (when card is sender)
    private BigDecimal balance;        // Current card balance
    private Long incomeTransfersCount; // Number of incoming transfers
    private Long expenseTransfersCount; // Number of outgoing transfers
}