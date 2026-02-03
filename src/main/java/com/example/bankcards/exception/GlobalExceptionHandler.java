package com.example.bankcards.exception;

import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.security.SignatureException;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.InsufficientAuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    // ================= Authentication & Security Exceptions =================
    
    /**
     * Handles authentication exceptions from our service.
     */
    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ErrorResponse> handleAuthenticationException(
            AuthenticationException ex, HttpServletRequest request) {
        return buildErrorResponse(
            HttpStatus.UNAUTHORIZED, 
            "AUTHENTICATION_ERROR", 
            ex.getMessage(), 
            request.getRequestURI()
        );
    }

    /**
     * Handles invalid credentials exceptions.
     */
    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ErrorResponse> handleBadCredentialsException(
            @SuppressWarnings("unused") BadCredentialsException ex, HttpServletRequest request) {
        return buildErrorResponse(
            HttpStatus.UNAUTHORIZED, 
            "BAD_CREDENTIALS", 
            "Invalid login or password", 
            request.getRequestURI()
        );
    }

    /**
     * Handles access denied exceptions.
     */
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleAccessDeniedException(
            @SuppressWarnings("unused") AccessDeniedException ex, HttpServletRequest request) {
        return buildErrorResponse(
            HttpStatus.FORBIDDEN, 
            "ACCESS_DENIED", 
            "Access denied. Insufficient permissions.", 
            request.getRequestURI()
        );
    }

    /**
     * Handles insufficient authentication exceptions.
     */
    @ExceptionHandler(InsufficientAuthenticationException.class)
    public ResponseEntity<ErrorResponse> handleInsufficientAuthenticationException(
            @SuppressWarnings("unused") InsufficientAuthenticationException ex, HttpServletRequest request) {
        return buildErrorResponse(
            HttpStatus.UNAUTHORIZED, 
            "INSUFFICIENT_AUTHENTICATION", 
            "Authentication required", 
            request.getRequestURI()
        );
    }

    // ================= JWT Exceptions =================
    
    /**
     * Handles expired JWT token exceptions.
     */
    @ExceptionHandler(ExpiredJwtException.class)
    public ResponseEntity<ErrorResponse> handleExpiredJwtException(
            @SuppressWarnings("unused") ExpiredJwtException ex, HttpServletRequest request) {
        return buildErrorResponse(
            HttpStatus.UNAUTHORIZED, 
            "TOKEN_EXPIRED", 
            "JWT token has expired", 
            request.getRequestURI()
        );
    }

    /**
     * Handles malformed JWT token exceptions.
     */
    @ExceptionHandler(MalformedJwtException.class)
    public ResponseEntity<ErrorResponse> handleMalformedJwtException(
            @SuppressWarnings("unused") MalformedJwtException ex, HttpServletRequest request) {
        return buildErrorResponse(
            HttpStatus.UNAUTHORIZED, 
            "INVALID_TOKEN", 
            "Malformed JWT token", 
            request.getRequestURI()
        );
    }

    /**
     * Handles JWT token signature exceptions.
     */
    @ExceptionHandler(SignatureException.class)
    public ResponseEntity<ErrorResponse> handleSignatureException(
            @SuppressWarnings("unused") SignatureException ex, HttpServletRequest request) {
        return buildErrorResponse(
            HttpStatus.UNAUTHORIZED, 
            "INVALID_TOKEN_SIGNATURE", 
            "Invalid JWT token signature", 
            request.getRequestURI()
        );
    }

    /**
     * Handles general JWT exceptions.
     */
    @ExceptionHandler(JwtException.class)
    public ResponseEntity<ErrorResponse> handleJwtException(
            JwtException ex, HttpServletRequest request) {
        return buildErrorResponse(
            HttpStatus.UNAUTHORIZED, 
            "JWT_ERROR", 
            "JWT processing error: " + ex.getMessage(), 
            request.getRequestURI()
        );
    }

    // ================= User Exceptions =================
    
    @ExceptionHandler(UserNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleUserNotFound(
            UserNotFoundException ex, HttpServletRequest request) {
        return buildErrorResponse(
            HttpStatus.NOT_FOUND, 
            "USER_NOT_FOUND", 
            ex.getMessage(), 
            request.getRequestURI()
        );
    }

    @ExceptionHandler(UserAlreadyExistsException.class)
    public ResponseEntity<ErrorResponse> handleUserAlreadyExists(
            UserAlreadyExistsException ex, HttpServletRequest request) {
        return buildErrorResponse(
            HttpStatus.CONFLICT, 
            "USER_ALREADY_EXISTS", 
            ex.getMessage(), 
            request.getRequestURI()
        );
    }

    // ================= Card Exceptions =================
    
    @ExceptionHandler(CardNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleCardNotFound(
            CardNotFoundException ex, HttpServletRequest request) {
        return buildErrorResponse(
            HttpStatus.NOT_FOUND, 
            "CARD_NOT_FOUND", 
            ex.getMessage(), 
            request.getRequestURI()
        );
    }

    @ExceptionHandler(CardAlreadyExistsException.class)
    public ResponseEntity<ErrorResponse> handleCardAlreadyExists(
            CardAlreadyExistsException ex, HttpServletRequest request) {
        return buildErrorResponse(
            HttpStatus.CONFLICT, 
            "CARD_ALREADY_EXISTS", 
            ex.getMessage(), 
            request.getRequestURI()
        );
    }

    // ================= Transfer Exceptions =================
    
    @ExceptionHandler(InsufficientFundsException.class)
    public ResponseEntity<ErrorResponse> handleInsufficientFunds(
            InsufficientFundsException ex, HttpServletRequest request) {
        return buildErrorResponse(
            HttpStatus.BAD_REQUEST, 
            "INSUFFICIENT_FUNDS", 
            ex.getMessage(), 
            request.getRequestURI()
        );
    }

    @ExceptionHandler(InvalidTransferException.class)
    public ResponseEntity<ErrorResponse> handleInvalidTransfer(
            InvalidTransferException ex, HttpServletRequest request) {
        return buildErrorResponse(
            HttpStatus.BAD_REQUEST, 
            "INVALID_TRANSFER", 
            ex.getMessage(), 
            request.getRequestURI()
        );
    }

    /**
     * Handles custom access denied exceptions (different from Spring Security's AccessDeniedException).
     */
    @ExceptionHandler(com.example.bankcards.exception.AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleCustomAccessDenied(
            com.example.bankcards.exception.AccessDeniedException ex, HttpServletRequest request) {
        return buildErrorResponse(
            HttpStatus.FORBIDDEN, 
            "ACCESS_DENIED", 
            ex.getMessage(), 
            request.getRequestURI()
        );
    }

    // ================= Validation Exceptions =================
    
    /**
     * Handles input data validation exceptions.
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationExceptions(
            MethodArgumentNotValidException ex, HttpServletRequest request) {
        
        List<String> details = new ArrayList<>();
        for (FieldError error : ex.getBindingResult().getFieldErrors()) {
            details.add(error.getField() + ": " + error.getDefaultMessage());
        }

        ErrorResponse errorResponse = new ErrorResponse(
            HttpStatus.BAD_REQUEST.value(),
            "VALIDATION_ERROR",
            "Validation failed",
            request.getRequestURI(),
            details
        );
        
        return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
    }

    /**
     * Handles unsupported content type exceptions.
     */
    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public ResponseEntity<ErrorResponse> handleHttpMediaTypeNotSupported(
            @SuppressWarnings("unused") HttpMediaTypeNotSupportedException ex, HttpServletRequest request) {
        return buildErrorResponse(
            HttpStatus.UNSUPPORTED_MEDIA_TYPE,
            "UNSUPPORTED_MEDIA_TYPE",
            "Content-Type must be 'application/json'. Please set Content-Type header in your request.",
            request.getRequestURI()
        );
    }

    /**
     * Handles malformed JSON exceptions.
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleHttpMessageNotReadable(
            HttpMessageNotReadableException ex, HttpServletRequest request) {
        String message = "Invalid request format. Please check your JSON data and field names.";
        if (ex.getMessage() != null && ex.getMessage().contains("JSON parse error")) {
            message = "Invalid JSON format. Please check your request body and ensure all fields are correct.";
        }
        return buildErrorResponse(
            HttpStatus.BAD_REQUEST, 
            "BAD_REQUEST", 
            message, 
            request.getRequestURI()
        );
    }

    /**
     * Handles IllegalStateException exceptions.
     */
    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ErrorResponse> handleIllegalState(
            IllegalStateException ex, HttpServletRequest request) {
        String message = ex.getMessage();
        if (message != null && (message.contains("Email") || message.contains("token") || message.contains("Authentication"))) {
            return buildErrorResponse(
                HttpStatus.FORBIDDEN, 
                "ACCESS_DENIED", 
                "Access denied: You can only update your own information.", 
                request.getRequestURI()
            );
        }
        return buildErrorResponse(
            HttpStatus.BAD_REQUEST, 
            "BAD_REQUEST", 
            message != null ? message : "Illegal state", 
            request.getRequestURI()
        );
    }

    /**
     * Handles general runtime exceptions.
     */
    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<ErrorResponse> handleRuntimeException(
            RuntimeException ex, HttpServletRequest request) {
        log.error("Runtime exception occurred", ex);
        return buildErrorResponse(
            HttpStatus.BAD_REQUEST, 
            "RUNTIME_ERROR", 
            ex.getMessage(), 
            request.getRequestURI()
        );
    }

    /**
     * Handles all other unhandled exceptions.
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleAllExceptions(
            Exception ex, HttpServletRequest request) {
        log.error("Unexpected error occurred", ex);

        String message = "An unexpected error occurred. Please check your input data and try again.";
        if (ex.getMessage() != null && !ex.getMessage().isEmpty() && 
            (ex.getMessage().contains("NullPointerException") ||
             ex.getMessage().contains("IllegalArgumentException"))) {
            message = "Invalid request data. Please check your input and try again.";
        }
        
        return buildErrorResponse(
            HttpStatus.INTERNAL_SERVER_ERROR, 
            "INTERNAL_ERROR", 
            message, 
            request.getRequestURI()
        );
    }

    // ================= Helper Method =================
    
    /**
     * Helper method for creating standardized error response.
     */
    private ResponseEntity<ErrorResponse> buildErrorResponse(
            HttpStatus status, String error, String message, String path) {
        ErrorResponse errorResponse = new ErrorResponse(
            status.value(), 
            error, 
            message, 
            path
        );
        return ResponseEntity.status(status).body(errorResponse);
    }
}
