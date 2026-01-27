package com.example.bankcards.entity;

import com.fasterxml.jackson.annotation.JsonManagedReference;
import jakarta.persistence.Id;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.NamedQuery;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Column;
import jakarta.persistence.Enumerated;
import jakarta.persistence.EnumType;
import jakarta.persistence.OneToMany;
import jakarta.persistence.CascadeType;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Setter
@Getter
@Entity
@Table(name = "users", schema = "public")
@NoArgsConstructor
@NamedQuery(
        name = "User.findByEmailNamed",
        query = "Select u FROM User u where u.email=:email"
)
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "first_name", nullable = false)
    private String firstName;

    @Column(name = "last_name", nullable = false)
    private String lastName;

    @Column(name = "birth_date")
    private LocalDate birthDate;

    @Column(unique = true, nullable = false)
    private String email; // Для входа в систему и уведомлений

    @Column(name = "phone_number")
    private String phoneNumber; // Дополнительный способ связи

    @Column(nullable = false)
    private String password; // Будет хешироваться в сервисе

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role = Role.ROLE_USER;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true; // Для блокировки пользователя

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonManagedReference
    private List<Card> cards = new ArrayList<>();

    // Полное имя пользователя
    public String getFullName() {
        return firstName + " " + lastName;
    }

    // Проверка активности аккаунта
    public boolean isAccountActive() {
        return isActive != null && isActive;
    }

    // Получить все карты пользователя (для совместимости с CardMapper)
    public List<Card> getAllCards() {
        return cards != null ? cards : new ArrayList<>();
    }
}
