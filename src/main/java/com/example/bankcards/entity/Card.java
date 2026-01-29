package com.example.bankcards.entity;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "cards", schema = "public")
@Getter
@Setter
@NoArgsConstructor
public class Card {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    @JsonBackReference
    private User user;

    @Column(nullable = false, length = 256) // Для зашифрованного номера
    private String number;

    @Column(nullable = false)
    private String holder;

    @Column(name = "expiration_date", nullable = false)
    private LocalDate expirationDate;

    @Column(nullable = false, length = 4) // CVV зашифрован, но короткий
    @JsonIgnore // НИКОГДА не отправляем CVV в JSON ответах!
    private String cvv;

    @Column(precision = 15, scale = 2, nullable = false)
    private BigDecimal balance = BigDecimal.ZERO;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Status status = Status.ACTIVE;

    // Метод для отображения маскированного номера карты
    public String getMaskedNumber() {
        if (number == null || number.length() < 4) {
            return "****";
        }
        return "**** **** **** " + number.substring(number.length() - 4);
    }

    // Метод для проверки CVV (для внутреннего использования)
    public boolean verifyCvv(String inputCvv) {
        // В реальном приложении здесь будет расшифровка и сравнение
        return this.cvv != null && this.cvv.equals(inputCvv);
    }

    // Маскированная дата истечения (MM/YY)
    public String getMaskedExpirationDate() {
        if (expirationDate == null) {
            return "**/**";
        }
        return String.format("%02d/%02d", 
            expirationDate.getMonthValue(), 
            expirationDate.getYear() % 100);
    }

    // Проверка активности карты
    public boolean isActive() {
        return status == Status.ACTIVE && expirationDate.isAfter(LocalDate.now());
    }

    // Операции с балансом
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

    // Административные операции
    public void block() {
        this.status = Status.BLOCKED;
    }

    public void activate() {
        this.status = Status.ACTIVE;
    }
}
