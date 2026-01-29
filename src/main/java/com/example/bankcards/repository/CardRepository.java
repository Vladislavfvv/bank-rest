package com.example.bankcards.repository;

import com.example.bankcards.entity.Card;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface CardRepository extends JpaRepository<Card,Long> {
    // JPQL-запрос
    @Query("SELECT COUNT(c) FROM Card c WHERE c.user.id = :userId")
    long countCardsByUserId(Long userId);

    /**
     * Находит карту по номеру и ID пользователя.
     * Используется для проверки, есть ли уже такая карта у данного пользователя.
     */
    @Query("SELECT c FROM Card c WHERE c.number = :number AND c.user.id = :userId")
    Optional<Card> findByNumberAndUserId(@Param("number") String number, @Param("userId") Long userId);

    /**
     * Находит карту по номеру (независимо от пользователя).
     * Используется для проверки, не принадлежит ли карта другому пользователю.
     */
    @Query("SELECT c FROM Card c WHERE c.number = :number")
    Optional<Card> findByNumber(@Param("number") String number);

    /**
     * Находит все карты пользователя по email (без учета регистра) с пагинацией.
     * Используется для получения списка карт текущего пользователя.
     */
    @Query("SELECT c FROM Card c WHERE LOWER(c.user.email) = LOWER(:email)")
    Page<Card> findAllByUser_EmailIgnoreCase(@Param("email") String email, Pageable pageable);

    /**
     * Находит все карты пользователя по ID пользователя с пагинацией.
     * Используется админом для получения карт конкретного пользователя.
     */
    @Query("SELECT c FROM Card c WHERE c.user.id = :userId")
    Page<Card> findAllByUserId(@Param("userId") Long userId, Pageable pageable);

    /**
     * Удаляет все карты пользователя по ID пользователя.
     * Используется для гарантированного удаления всех карт пользователя.
     */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("DELETE FROM Card c WHERE c.user.id = :userId")
    int deleteAllByUserId(@Param("userId") Long userId);

    /**
     * Удаляет карты по списку ID.
     * Используется для гарантированного удаления конкретных карт.
     */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("DELETE FROM Card c WHERE c.id IN :cardIds")
    int deleteByIds(@Param("cardIds") List<Long> cardIds);
}
