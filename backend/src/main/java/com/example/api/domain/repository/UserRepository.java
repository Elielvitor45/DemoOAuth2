package com.example.api.domain.repository;

import com.example.api.domain.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);
    boolean existsByEmail(String email);

    @Query("SELECT u FROM User u LEFT JOIN FETCH u.providers WHERE u.email = :email")
    Optional<User> findByEmailWithProviders(@Param("email") String email);

    @Query("SELECT u FROM User u LEFT JOIN FETCH u.providers WHERE u.id = :id")
    Optional<User> findByIdWithProviders(@Param("id") Long id);
}
