package com.example.bankcards.dto.card;

import com.example.bankcards.entity.RequestStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * DTO for card block request information.
 * Used to transfer data about card blocking requests between layers.
 * Contains request details, user information, admin processing data and additional card info.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BlockRequestDto {
    private Long id;
    private Long cardId;
    private String cardMaskedNumber;
    private Long userId;
    private String userEmail;
    private String reason;
    private RequestStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime processedAt;
    private Long processedByAdminId;
    private String processedByAdminEmail;
    private String adminComment;
    // Additional card information for admin
    private com.example.bankcards.entity.Status cardStatus;
    private java.math.BigDecimal cardBalance;
    private java.time.LocalDate cardExpirationDate;
}
