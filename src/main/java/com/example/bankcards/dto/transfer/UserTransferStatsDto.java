package com.example.bankcards.dto.transfer;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

/**
 * DTO for user transfer statistics.
 * Contains aggregated transfer data for a specific user across all their cards.
 * Includes overall financial summary and detailed statistics for each card.
 * Used for comprehensive financial reporting and user analytics.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserTransferStatsDto {
    private Long userId;
    private String userEmail;
    private String userFullName;
    private BigDecimal totalIncome;    // Total income across all cards
    private BigDecimal totalExpense;   // Total expense across all cards
    private BigDecimal totalBalance;   // Total balance across all cards
    private List<CardTransferStatsDto> cardStats; // Statistics by card
}