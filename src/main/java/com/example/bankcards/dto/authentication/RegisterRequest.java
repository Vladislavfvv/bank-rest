package com.example.bankcards.dto.authentication;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDate;

/**
 * Data Transfer Object for user registration requests.
 * Contains all necessary information for creating a new user account in the banking system.
 * Includes comprehensive validation rules to ensure data integrity and security.
 * Used by the authentication controller to process new user registrations.
 */
@Data
public class RegisterRequest {
    
    /**
     * User's first name.
     * Must be between 2 and 50 characters long and cannot be blank.
     * Used for creating the card holder name and user identification.
     */
    @NotBlank(message = "First name is required")
    @Size(min = 2, max = 50, message = "First name must be between 2 and 50 characters")
    private String firstName;

    /**
     * User's last name.
     * Must be between 2 and 50 characters long and cannot be blank.
     * Combined with first name to create the full card holder name.
     */
    @NotBlank(message = "Last name is required")
    @Size(min = 2, max = 50, message = "Last name must be between 2 and 50 characters")
    private String lastName;

    /**
     * User's birth date.
     * Must be a date in the past to ensure valid age verification.
     * Used for age verification and compliance with banking regulations.
     * Expected format: yyyy-MM-dd (e.g., "1990-05-15").
     */
    @NotNull(message = "Birth date is required")
    @Past(message = "Birth date must be in the past")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
    private LocalDate birthDate;

    /**
     * User's email address.
     * Must be a valid email format and serves as the unique identifier for login.
     * Used for authentication and communication with the user.
     * Must be unique across the system.
     */
    @NotBlank(message = "Email is required")
    @Email(message = "Email should be valid")
    private String email;

    /**
     * User's phone number (optional).
     * Must follow international phone number format if provided.
     * Accepts numbers with optional country code prefix (+).
     * Used for additional contact information and potential 2FA.
     */
    @Pattern(regexp = "^\\+?[1-9]\\d{1,14}$", message = "Phone number format is invalid")
    private String phoneNumber;

    /**
     * User's password for account security.
     * Must be at least 8 characters long and contain:
     * - At least one lowercase letter (a-z)
     * - At least one uppercase letter (A-Z)
     * - At least one digit (0-9)
     * Password will be encrypted using BCrypt before storage.
     */
    @NotBlank(message = "Password is required")
    @Size(min = 8, message = "Password must be at least 8 characters")
    @Pattern(regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d).*$", 
             message = "Password must contain at least one lowercase letter, one uppercase letter, and one digit")
    private String password;
}