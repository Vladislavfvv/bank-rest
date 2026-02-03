package com.example.bankcards.scheduler;

import com.example.bankcards.service.CardService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Scheduled task for automatically updating expired cards status to EXPIRED
 * Runs daily at midnight (00:00:00) by default.
 * Can be configured via application.yml property: card.expiration.check.cron
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CardExpirationScheduler {

    private final CardService cardService;

    /**
     * Scheduled task to check and update expired cards.
     * Runs every day at 00:00:00 (midnight) by default.
     * Cron expression: "0 0 0 * * ?" means:
     * - 0 seconds
     * - 0 minutes
     * - 0 hours (midnight)
     * - Every day of month
     * - Every month
     * - Every day of week
     * To run more frequently (e.g., every hour), use: "0 0 * * * ?"
     * To run at specific time (e.g., 2 AM), use: "0 0 2 * * ?"
     */
    @Scheduled(cron = "${card.expiration.check.cron:0 0 0 * * ?}")
    public void checkAndUpdateExpiredCards() {
        log.info("Starting scheduled task: Checking for expired cards...");
        
        try {
            int updatedCount = cardService.updateExpiredCardsStatus();
            
            if (updatedCount > 0) {
                log.info("Scheduled task completed: {} cards updated to EXPIRED status", updatedCount);
            } else {
                log.debug("Scheduled task completed: No expired cards found");
            }
        } catch (Exception e) {
            handleScheduledTaskError(e);
        }
    }

    /**
     * Handles errors during scheduled task execution.
     * Logs warning without full stack trace to reduce log noise.
     */
    private void handleScheduledTaskError(Exception e) {
        log.warn("Error during scheduled task for expired cards check: {}", e.getMessage());
    }
}
