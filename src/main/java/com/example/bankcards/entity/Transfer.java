package com.example.bankcards.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "transfers", schema = "public")
@Getter
@Setter
@NoArgsConstructor
public class Transfer {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "from_card_id", nullable = false)
    private Card fromCard;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "to_card_id", nullable = false)
    private Card toCard;

    @Column(precision = 15, scale = 2, nullable = false)
    private BigDecimal amount;

    @Column(length = 255)
    private String description;

    @Column(name = "transfer_date", nullable = false)
    private LocalDateTime transferDate = LocalDateTime.now();

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TransferStatus status = TransferStatus.COMPLETED;

    // Проверка, что перевод между картами одного пользователя
    public boolean isSameUserTransfer() {
        return fromCard != null && toCard != null && 
               fromCard.getUser().getId().equals(toCard.getUser().getId());
    }

    // Получить пользователя (владельца карт)
    public User getUser() {
        return fromCard != null ? fromCard.getUser() : null;
    }
}