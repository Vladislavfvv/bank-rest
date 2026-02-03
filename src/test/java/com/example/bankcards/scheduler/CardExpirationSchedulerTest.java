package com.example.bankcards.scheduler;

import com.example.bankcards.service.CardService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for CardExpirationScheduler.
 * Tests the scheduled task that automatically updates expired cards.
 */
@ExtendWith(MockitoExtension.class)
class CardExpirationSchedulerTest {

    @InjectMocks
    private CardExpirationScheduler scheduler;

    @Mock
    private CardService cardService;

    @Test
    @DisplayName("checkAndUpdateExpiredCards - Successfully updates expired cards")
    void checkAndUpdateExpiredCards_WithExpiredCards_UpdatesSuccessfully() {
        // given
        int updatedCount = 3;
        when(cardService.updateExpiredCardsStatus()).thenReturn(updatedCount);

        // when
        scheduler.checkAndUpdateExpiredCards();

        // then
        verify(cardService).updateExpiredCardsStatus();
    }

    @Test
    @DisplayName("checkAndUpdateExpiredCards - No expired cards found")
    void checkAndUpdateExpiredCards_NoExpiredCards_CompletesSuccessfully() {
        // given
        int updatedCount = 0;
        when(cardService.updateExpiredCardsStatus()).thenReturn(updatedCount);

        // when
        scheduler.checkAndUpdateExpiredCards();

        // then
        verify(cardService).updateExpiredCardsStatus();
    }

    @Test
    @DisplayName("checkAndUpdateExpiredCards - Handles exceptions gracefully")
    void checkAndUpdateExpiredCards_ServiceThrowsException_HandlesGracefully() {
        // given
        when(cardService.updateExpiredCardsStatus())
                .thenThrow(new RuntimeException("Database error"));

        // when & then - should not throw exception, should handle gracefully
        scheduler.checkAndUpdateExpiredCards();

        // verify that service was called
        verify(cardService).updateExpiredCardsStatus();
    }
}
