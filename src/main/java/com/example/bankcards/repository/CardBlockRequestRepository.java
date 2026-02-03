package com.example.bankcards.repository;

import com.example.bankcards.entity.CardBlockRequest;
import com.example.bankcards.entity.RequestStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.lang.NonNull;

import java.util.List;

@SuppressWarnings("SqlNoDataSourceInspection")
public interface CardBlockRequestRepository extends JpaRepository<CardBlockRequest, Long> {
    
    /**
     * Finds all block requests by status with pagination.
     * Used by admin to filter requests by status.
     */
    Page<CardBlockRequest> findByStatus(@NonNull RequestStatus status, @NonNull Pageable pageable);
    
    /**
     * Finds all pending block requests with pagination.
     * Used by admin to view pending requests.
     */
    Page<CardBlockRequest> findByStatusOrderByCreatedAtDesc(@NonNull RequestStatus status, @NonNull Pageable pageable);
    
    /**
     * Finds all block requests for a specific card.
     * Used to check if there are existing requests for a card.
     */
    List<CardBlockRequest> findByCardId(@NonNull Long cardId);
    
    /**
     * Finds all pending block requests for a specific card.
     * Used to check if there are pending requests for a card.
     */
    List<CardBlockRequest> findByCardIdAndStatus(@NonNull Long cardId, @NonNull RequestStatus status);
    
    /**
     * Counts pending requests.
     * Used for notifications to admin.
     */
    @Query("SELECT COUNT(cbr) FROM CardBlockRequest cbr WHERE cbr.status = :status")
    long countByStatus(@Param("status") @NonNull RequestStatus status);
}
