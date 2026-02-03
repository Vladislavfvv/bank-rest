package com.example.bankcards.repository;

import com.example.bankcards.entity.Card;
import com.example.bankcards.entity.Status;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@SuppressWarnings("SqlNoDataSourceInspection")
public interface CardRepository extends JpaRepository<Card,Long> {
    // JPQL query
    @Query("SELECT COUNT(c) FROM Card c WHERE c.user.id = :userId")
    long countCardsByUserId(@NonNull Long userId);

    /**
     * Finds card by number and user ID.
     * Used to check if such card already exists for the given user.
     */
    @Query("SELECT c FROM Card c WHERE c.number = :number AND c.user.id = :userId")
    Optional<Card> findByNumberAndUserId(@Param("number") @NonNull String number, @Param("userId") @NonNull Long userId);

    /**
     * Finds card by number (regardless of user).
     * Used to check if card belongs to another user.
     */
    @Query("SELECT c FROM Card c WHERE c.number = :number")
    Optional<Card> findByNumber(@Param("number") @NonNull String number);

    /**
     * Finds all user cards by email with pagination.
     * Used to get current user's card list.
     */
    @Query("SELECT c FROM Card c WHERE LOWER(c.user.email) = LOWER(:email)")
    Page<Card> findAllByUser_EmailIgnoreCase(@Param("email") @NonNull String email, @NonNull Pageable pageable);

    /**
     * Finds all user cards by email with filters (status, expiration date) and pagination.
     * Used to get filtered current user's card list.
     */
    @Query("SELECT c FROM Card c WHERE LOWER(c.user.email) = LOWER(:email) " +
           "AND (:status IS NULL OR c.status = :status) " +
           "AND (:expirationDateFrom IS NULL OR c.expirationDate >= :expirationDateFrom) " +
           "AND (:expirationDateTo IS NULL OR c.expirationDate <= :expirationDateTo)")
    Page<Card> findAllByUser_EmailIgnoreCaseWithFilters(
            @Param("email") @NonNull String email,
            @Param("status") @Nullable Status status,
            @Param("expirationDateFrom") @Nullable LocalDate expirationDateFrom,
            @Param("expirationDateTo") @Nullable LocalDate expirationDateTo,
            @NonNull Pageable pageable);

    /**
     * Finds all cards with filters (status, expiration date) and pagination.
     * Used by admin to get filtered all cards list.
     */
    @Query("SELECT c FROM Card c WHERE " +
           "(:status IS NULL OR c.status = :status) " +
           "AND (:expirationDateFrom IS NULL OR c.expirationDate >= :expirationDateFrom) " +
           "AND (:expirationDateTo IS NULL OR c.expirationDate <= :expirationDateTo)")
    Page<Card> findAllWithFilters(
            @Param("status") @Nullable Status status,
            @Param("expirationDateFrom") @Nullable LocalDate expirationDateFrom,
            @Param("expirationDateTo") @Nullable LocalDate expirationDateTo,
            @NonNull Pageable pageable);

    /**
     * Finds all user cards by user ID with pagination.
     * Used by admin to get cards of specific user.
     */
    @Query("SELECT c FROM Card c WHERE c.user.id = :userId")
    Page<Card> findAllByUserId(@Param("userId") @NonNull Long userId, @NonNull Pageable pageable);

    /**
     * Deletes all user cards by user ID.
     * Used for guaranteed deletion of all user cards.
     */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("DELETE FROM Card c WHERE c.user.id = :userId")
    int deleteAllByUserId(@Param("userId") @NonNull Long userId);

    /**
     * Deletes cards by list of IDs.
     * Used for guaranteed deletion of specific cards.
     */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("DELETE FROM Card c WHERE c.id IN :cardIds")
    int deleteByIds(@Param("cardIds") @NonNull List<Long> cardIds);

    /**
     * Finds all cards with expired expiration date that are not already marked as EXPIRED.
     * Used by scheduled task to automatically update card status.
     */
    @Query("SELECT c FROM Card c WHERE c.expirationDate < :currentDate AND c.status != :expiredStatus")
    List<Card> findExpiredCards(@Param("currentDate") @NonNull LocalDate currentDate, @Param("expiredStatus") @NonNull Status expiredStatus);

    /**
     * Updates status of expired cards to EXPIRED
     * Used by scheduled task for batch update.
     */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE Card c SET c.status = :expiredStatus WHERE c.expirationDate < :currentDate AND c.status != :expiredStatus")
    int updateExpiredCardsStatus(@Param("currentDate") @NonNull LocalDate currentDate, @Param("expiredStatus") @NonNull Status expiredStatus);

    /**
     * Finds card by ID with user loaded (JOIN FETCH).
     * Used to avoid LazyInitializationException when accessing card.user.
     */
    @Query("SELECT c FROM Card c JOIN FETCH c.user WHERE c.id = :id")
    Optional<Card> findByIdWithUser(@Param("id") @NonNull Long id);
}
