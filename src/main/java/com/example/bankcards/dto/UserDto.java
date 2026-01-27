package com.example.bankcards.dto;

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

    // Полное имя (вычисляемое поле)
    private String fullName;

    @Valid // для вложенной валидации карт
    private List<CardDto> cards;

    // Количество карт (вычисляемое поле)
    private Integer cardCount;
}
