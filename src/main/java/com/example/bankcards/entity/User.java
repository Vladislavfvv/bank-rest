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
import lombok.AllArgsConstructor;
import lombok.Builder;
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
@AllArgsConstructor
@Builder
@SuppressWarnings({"JpaDataSourceORMInspection"})
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
    private String email; // For system login and notifications

    @Column(name = "phone_number")
    private String phoneNumber; // Additional contact method

    @Column(nullable = false)
    private String password; // Will be hashed in service

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private Role role = Role.ROLE_USER;

    @Column(name = "created_at", nullable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean isActive = true; // For user blocking or later can be redesigned to something like deletion from DB

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonManagedReference
    @Builder.Default
    private List<Card> cards = new ArrayList<>();

    // User's full name
    public String getFullName() {
        return firstName + " " + lastName;
    }

    // Check account activity
    public boolean isAccountActive() {
        return isActive != null && isActive;
    }

    // Check if account is inactive (complementary method for better readability)
    public boolean isAccountInactive() {
        return !isAccountActive();
    }

    // Get all user cards (for compatibility with CardMapper)
    public List<Card> getAllCards() {
        return cards != null ? cards : new ArrayList<>();
    }
}
