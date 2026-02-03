package com.example.bankcards.dto.user;

import com.example.bankcards.dto.card.CardDto;
import com.example.bankcards.entity.Role;
import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * DTO for complete user information transfer.
 * Used for API responses and internal data exchange between layers.
 * Contains full user profile with validation constraints and associated cards.
 * Includes computed fields like full name and card count for convenience.
 */
@Data
public class UserDto {
    private Long id;

    @NotBlank(message = "First name must not be blank")
    private String firstName;

    @NotBlank(message = "Last name must not be blank")
    private String lastName;

    @NotNull(message = "Birth date is required")
    @PastOrPresent(message = "Birth date cannot be in the future")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
    private LocalDate birthDate;

    @NotBlank(message = "Email must not be blank")
    @Email(message = "Email should be valid")
    private String email;

    private String phoneNumber;

    private Role role;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdAt;

    private Boolean isActive;

    // Full name (computed field)
    private String fullName;

    @Valid // for nested card validation
    private List<CardDto> cards;

    // Number of cards (computed field)
    private Integer cardCount;
}
