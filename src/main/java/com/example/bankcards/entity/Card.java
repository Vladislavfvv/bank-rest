package com.example.bankcards.entity;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "cards", schema = "public")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
@SuppressWarnings({"JpaDataSourceORMInspection"}) // Suppress database connection warnings in IDE
public class Card {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    @JsonBackReference
    private User user;

    @Column(nullable = false, length = 256) // For encrypted number
    private String number;

    @Column(nullable = false)
    private String holder;

    @Column(name = "expiration_date", nullable = false)
    private LocalDate expirationDate;

    @Column(nullable = false, length = 256) // CVV encrypted, needs more space
    @JsonIgnore // NEVER send CVV in JSON responses!
    private String cvv;

    @Column(precision = 15, scale = 2, nullable = false)
    @Builder.Default
    private BigDecimal balance = BigDecimal.ZERO;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private Status status = Status.ACTIVE;

    // Method to display masked card number
    @SuppressWarnings("unused")
    public String getMaskedNumber() {
        if (number == null || number.length() < 4) {
            return "****";
        }
        return "**** **** **** " + number.substring(number.length() - 4);
    }



    // Masked expiration date (MM/YY)
    @SuppressWarnings("unused")
    public String getMaskedExpirationDate() {
        if (expirationDate == null) {
            return "**/**";
        }
        return String.format("%02d/%02d",
            expirationDate.getMonthValue(),
            expirationDate.getYear() % 100);
    }

    // Check card activity
    public boolean isActive() {
        return status == Status.ACTIVE && expirationDate.isAfter(LocalDate.now());
    }

    // Balance operations
    public boolean canDebit(BigDecimal amount) {
        return balance.compareTo(amount) >= 0 && isActive();
    }

    public void credit(BigDecimal amount) {
        if (amount.compareTo(BigDecimal.ZERO) > 0) {
            this.balance = this.balance.add(amount);
        }
    }

    public boolean debit(BigDecimal amount) {
        if (canDebit(amount)) {
            this.balance = this.balance.subtract(amount);
            return true;
        }
        return false;
    }

    // Administrative operations
    public void block() {
        this.status = Status.BLOCKED;
    }

    public void activate() {
        this.status = Status.ACTIVE;
    }
}
