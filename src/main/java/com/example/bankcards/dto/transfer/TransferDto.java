package com.example.bankcards.dto.transfer;

import com.example.bankcards.entity.TransferStatus;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * DTO for transfer information display.
 * Used to transfer complete transfer data between layers and for API responses.
 * Contains transfer details with masked card numbers for security.
 */
@Data
public class TransferDto {
    private Long id;
    
    private Long fromCardId;
    private String fromCardMaskedNumber;
    
    private Long toCardId;
    private String toCardMaskedNumber;
    
    private BigDecimal amount;
    private String description;
    
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime transferDate;
    
    private TransferStatus status;
}