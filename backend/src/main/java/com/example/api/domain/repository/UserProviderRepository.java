package com.example.api.domain.repository;

import com.example.api.domain.enums.AuthProvider;
import com.example.api.domain.model.UserProvider;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface UserProviderRepository extends JpaRepository<UserProvider, Long> {
    Optional<UserProvider> findByProviderAndProviderId(AuthProvider provider, String providerId);
}
