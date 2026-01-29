package com.example.bankcards.repository;

import com.example.bankcards.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User,Long> {
    User getUserById(Long aLong);

    List<User> getUsersByEmail(String email);

    //Используется @NamedQuery из User.java
    Optional<User> findByEmailNamed(@Param("email") String email);

    //JPQL запрос
    @Query("SELECT u from User u where u.email=:email")
    Optional<User> findByEmailJPQL(@Param("email") String email);

    //Native Sql
    @Query(value = "SELECT * from public.users u where u.email = :email", nativeQuery=true)
    Optional<User> findByEmailNativeQuery(@Param("email") String email);

    //для решения проблемы ленивой инициализации:
    // Используем стандартный метод findAll() - Spring Data JPA автоматически добавит ORDER BY из Pageable
    Page<User> findAll(Pageable pageable);

    // Поиск пользователей по части email (для админа)
    @Query("SELECT u FROM User u WHERE LOWER(u.email) LIKE LOWER(CONCAT('%', :emailPattern, '%'))")
    List<User> findByEmailContainingIgnoreCase(@Param("emailPattern") String emailPattern);

}
