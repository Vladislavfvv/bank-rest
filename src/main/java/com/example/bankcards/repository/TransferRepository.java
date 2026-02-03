package com.example.bankcards.repository;

import com.example.bankcards.entity.Transfer;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;

@Repository
@SuppressWarnings("SqlNoDataSourceInspection")
public interface TransferRepository extends JpaRepository<Transfer, Long> {

    /**
     * Find all user transfers (where user is sender or receiver)
     */
    @Query("SELECT t FROM Transfer t " +
           "WHERE t.fromCard.user.email = :userEmail " +
           "OR t.toCard.user.email = :userEmail")
    Page<Transfer> findByUserEmail(@Param("userEmail") @NonNull String userEmail, @NonNull Pageable pageable);

    /**
     * Find transfers for specific card
     */
    @Query("SELECT t FROM Transfer t " +
           "WHERE t.fromCard.id = :cardId OR t.toCard.id = :cardId")
    Page<Transfer> findByCardId(@Param("cardId") @NonNull Long cardId, @NonNull Pageable pageable);

    /**
     * Get total income amount for card (when card is receiver)
     */
    @Query("SELECT COALESCE(SUM(t.amount), 0) FROM Transfer t " +
           "WHERE t.toCard.id = :cardId AND t.status = 'COMPLETED'")
    BigDecimal getTotalIncomeByCardId(@Param("cardId") @NonNull Long cardId);

    /**
     * Get total expense amount for card (when card is sender)
     */
    @Query("SELECT COALESCE(SUM(t.amount), 0) FROM Transfer t " +
           "WHERE t.fromCard.id = :cardId AND t.status = 'COMPLETED'")
    BigDecimal getTotalExpenseByCardId(@Param("cardId") @NonNull Long cardId);

    /**
     * Get count of incoming transfers for card
     */
    @Query("SELECT COUNT(t) FROM Transfer t " +
           "WHERE t.toCard.id = :cardId AND t.status = 'COMPLETED'")
    Long getIncomeTransfersCountByCardId(@Param("cardId") @NonNull Long cardId);

    /**
     * Get count of outgoing transfers for card
     */
    @Query("SELECT COUNT(t) FROM Transfer t " +
           "WHERE t.fromCard.id = :cardId AND t.status = 'COMPLETED'")
    Long getExpenseTransfersCountByCardId(@Param("cardId") @NonNull Long cardId);

    /**
     * Get total income amount for all user cards
     */
    @Query("SELECT COALESCE(SUM(t.amount), 0) FROM Transfer t " +
           "WHERE t.toCard.user.id = :userId AND t.status = 'COMPLETED'")
    BigDecimal getTotalIncomeByUserId(@Param("userId") @NonNull Long userId);

    /**
     * Get total expense amount for all user cards
     */
    @Query("SELECT COALESCE(SUM(t.amount), 0) FROM Transfer t " +
           "WHERE t.fromCard.user.id = :userId AND t.status = 'COMPLETED'")
    BigDecimal getTotalExpenseByUserId(@Param("userId") @NonNull Long userId);
}