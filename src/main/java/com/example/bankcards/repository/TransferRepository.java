package com.example.bankcards.repository;

import com.example.bankcards.entity.Transfer;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;

@Repository
public interface TransferRepository extends JpaRepository<Transfer, Long> {

    /**
     * Найти все переводы пользователя (где он отправитель или получатель)
     */
    @Query("SELECT t FROM Transfer t " +
           "WHERE t.fromCard.user.email = :userEmail " +
           "OR t.toCard.user.email = :userEmail")
    Page<Transfer> findByUserEmail(@Param("userEmail") String userEmail, Pageable pageable);

    /**
     * Найти переводы конкретной карты
     */
    @Query("SELECT t FROM Transfer t " +
           "WHERE t.fromCard.id = :cardId OR t.toCard.id = :cardId")
    Page<Transfer> findByCardId(@Param("cardId") Long cardId, Pageable pageable);

    /**
     * Получить общую сумму приходов по карте (когда карта получатель)
     */
    @Query("SELECT COALESCE(SUM(t.amount), 0) FROM Transfer t " +
           "WHERE t.toCard.id = :cardId AND t.status = 'COMPLETED'")
    BigDecimal getTotalIncomeByCardId(@Param("cardId") Long cardId);

    /**
     * Получить общую сумму расходов по карте (когда карта отправитель)
     */
    @Query("SELECT COALESCE(SUM(t.amount), 0) FROM Transfer t " +
           "WHERE t.fromCard.id = :cardId AND t.status = 'COMPLETED'")
    BigDecimal getTotalExpenseByCardId(@Param("cardId") Long cardId);

    /**
     * Получить количество входящих переводов по карте
     */
    @Query("SELECT COUNT(t) FROM Transfer t " +
           "WHERE t.toCard.id = :cardId AND t.status = 'COMPLETED'")
    Long getIncomeTransfersCountByCardId(@Param("cardId") Long cardId);

    /**
     * Получить количество исходящих переводов по карте
     */
    @Query("SELECT COUNT(t) FROM Transfer t " +
           "WHERE t.fromCard.id = :cardId AND t.status = 'COMPLETED'")
    Long getExpenseTransfersCountByCardId(@Param("cardId") Long cardId);

    /**
     * Получить общую сумму приходов по всем картам пользователя
     */
    @Query("SELECT COALESCE(SUM(t.amount), 0) FROM Transfer t " +
           "WHERE t.toCard.user.id = :userId AND t.status = 'COMPLETED'")
    BigDecimal getTotalIncomeByUserId(@Param("userId") Long userId);

    /**
     * Получить общую сумму расходов по всем картам пользователя
     */
    @Query("SELECT COALESCE(SUM(t.amount), 0) FROM Transfer t " +
           "WHERE t.fromCard.user.id = :userId AND t.status = 'COMPLETED'")
    BigDecimal getTotalExpenseByUserId(@Param("userId") Long userId);
}