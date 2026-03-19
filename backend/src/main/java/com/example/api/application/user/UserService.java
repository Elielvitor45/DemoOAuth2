package com.example.api.application.user;

import com.example.api.domain.enums.AuthProvider;
import com.example.api.domain.model.*;
import com.example.api.domain.repository.*;
import com.example.api.infrastructure.oauth.OAuthUserInfo;
import com.example.api.web.dto.response.UserResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserService {

    private final UserRepository userRepository;
    private final UserProviderRepository userProviderRepository;
    private final PasswordEncoder passwordEncoder;

    /**
     * Busca ou cria usuário a partir de dados OAuth2.
     * Estratégia:
     * 1. Existe UserProvider com (provider, providerId)? → atualiza e retorna
     * 2. Existe User com mesmo email? → vincula provider e retorna
     * 3. Cria novo User + UserProvider
     */
    @Transactional
    public User findOrCreateOAuthUser(OAuthUserInfo info, AuthProvider provider,
                                      String accessToken, LocalDateTime tokenExpiresAt) {

        // Passo 1: provider já vinculado
        Optional<UserProvider> existingProvider =
            userProviderRepository.findByProviderAndProviderId(provider, info.providerId());

        if (existingProvider.isPresent()) {
            UserProvider up = existingProvider.get();
            User user = up.getUser();
            updateUserData(user, info);
            up.setAccessToken(accessToken);
            up.setTokenExpiresAt(tokenExpiresAt);
            userProviderRepository.save(up);
            return userRepository.save(user);
        }

        // Passo 2: mesmo email
        if (info.email() != null && !info.email().isBlank()) {
            Optional<User> existing = userRepository.findByEmail(info.email());
            if (existing.isPresent()) {
                User user = existing.get();
                linkProvider(user, provider, info.providerId(), accessToken, tokenExpiresAt);
                updateUserData(user, info);
                return userRepository.save(user);
            }
        }

        // Passo 3: novo usuário
        User newUser = User.builder()
            .email(info.email())
            .name(info.name() != null ? info.name() : "Usuário")
            .photoUrl(info.photoUrl())
            .build();
        User saved = userRepository.save(newUser);
        linkProvider(saved, provider, info.providerId(), accessToken, tokenExpiresAt);
        log.info("Novo usuário criado via OAuth. provider={}, userId={}", provider, saved.getId());
        return saved;
    }

    @Transactional
    public User registerLocal(String email, String password, String name) {
        if (userRepository.existsByEmail(email)) {
            throw new IllegalArgumentException("Email já cadastrado");
        }
        User user = User.builder()
            .email(email)
            .password(passwordEncoder.encode(password))
            .name(name)
            .build();
        User saved = userRepository.save(user);
        linkProvider(saved, AuthProvider.LOCAL, null, null, null);
        log.info("Usuário local registrado. userId={}", saved.getId());
        return saved;
    }

    @Transactional(readOnly = true)
    public Optional<User> findByEmail(String email) {
        return userRepository.findByEmailWithProviders(email);
    }

    @Transactional(readOnly = true)
    public Optional<User> findById(Long id) {
        return userRepository.findByIdWithProviders(id);
    }

    public UserResponse toResponse(User user) {
        List<String> providerNames = user.getProviders().stream()
            .map(p -> p.getProvider().name())
            .toList();
        return new UserResponse(
            user.getId(),
            user.getName(),
            user.getEmail(),
            user.getPhotoUrl(),
            providerNames
        );
    }

    // ── privados ──────────────────────────────────────────────

    private void linkProvider(User user, AuthProvider provider, String providerId,
                               String accessToken, LocalDateTime tokenExpiresAt) {
        UserProvider up = UserProvider.builder()
            .user(user)
            .provider(provider)
            .providerId(providerId)
            .accessToken(accessToken)
            .tokenExpiresAt(tokenExpiresAt)
            .build();
        userProviderRepository.save(up);
    }

    private void updateUserData(User user, OAuthUserInfo info) {
        if (info.name() != null && !info.name().equals(user.getName())) {
            user.setName(info.name());
        }
        if (info.photoUrl() != null && !info.photoUrl().equals(user.getPhotoUrl())) {
            user.setPhotoUrl(info.photoUrl());
        }
    }
}
