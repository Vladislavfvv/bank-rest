package com.example.bankcards.repository;

import com.example.bankcards.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.lang.NonNull;

import java.util.List;
import java.util.Optional;

@SuppressWarnings("SqlNoDataSourceInspection")
public interface UserRepository extends JpaRepository<User,Long> {
    User getUserById(@NonNull Long aLong);

    List<User> getUsersByEmail(@NonNull String email);

    // Uses @NamedQuery from User.java
    Optional<User> findByEmailNamed(@Param("email") @NonNull String email);

    // JPQL query
    @Query("SELECT u from User u where u.email=:email")
    Optional<User> findByEmailJPQL(@Param("email") @NonNull String email);

    // Native SQL query
    @Query(value = "SELECT * from public.users u where u.email = :email", nativeQuery=true)
    Optional<User> findByEmailNativeQuery(@Param("email") @NonNull String email);

    // Search users by email pattern (for admin)
    @Query("SELECT u FROM User u WHERE LOWER(u.email) LIKE LOWER(CONCAT('%', :emailPattern, '%'))")
    List<User> findByEmailContainingIgnoreCase(@Param("emailPattern") @NonNull String emailPattern);

}
