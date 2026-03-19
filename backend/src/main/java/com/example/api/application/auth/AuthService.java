package com.example.api.application.auth;

import com.example.api.application.user.UserService;
import com.example.api.domain.enums.AuthProvider;
import com.example.api.domain.model.*;
import com.example.api.domain.repository.RefreshTokenRepository;
import com.example.api.infrastructure.oauth.*;
import com.example.api.infrastructure.security.JwtService;
import com.example.api.web.dto.request.*;
import com.example.api.web.dto.response.AuthResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.authentication.*;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

    private final UserService userService;
    private final JwtService jwtService;
    private final RefreshTokenRepository refreshTokenRepository;
    private final GoogleOAuthClient googleClient;
    private final FacebookOAuthClient facebookClient;
    private final AuthenticationManager authenticationManager;

    // ── Login local (email + senha) ───────────────────────────

    @Transactional
    public AuthResponse loginLocal(LoginRequest request) {
        // Valida credenciais via Spring Security
        authenticationManager.authenticate(
            new UsernamePasswordAuthenticationToken(request.email(), request.password())
        );
        User user = userService.findByEmail(request.email())
            .orElseThrow(() -> new UsernameNotFoundException("Usuário não encontrado"));
        log.info("Login local. userId={}", user.getId());
        return buildAuthResponse(user);
    }

    // ── Cadastro local ────────────────────────────────────────

    @Transactional
    public AuthResponse registerLocal(RegisterRequest request) {
        User user = userService.registerLocal(request.email(), request.password(), request.name());
        return buildAuthResponse(user);
    }

    // ── OAuth2 callback ───────────────────────────────────────

    /**
     * Recebe o authorization_code do Angular e:
     * 1. Troca code por access_token no provedor
     * 2. Busca dados do usuário no provedor
     * 3. Busca ou cria usuário interno
     * 4. Emite JWT interno + refresh token
     */
    @Transactional
    public AuthResponse processOAuthCallback(OAuthCallbackRequest request) {
        AuthProvider provider = parseProvider(request.provider());

        OAuthTokenData tokenData;
        OAuthUserInfo  userInfo;

        switch (provider) {
            case GOOGLE -> {
                tokenData = googleClient.exchangeCode(request.code());
                userInfo  = googleClient.fetchUserInfo(tokenData.accessToken());
            }
            case FACEBOOK -> {
                tokenData = facebookClient.exchangeCode(request.code());
                userInfo  = facebookClient.fetchUserInfo(tokenData.accessToken());
            }
            default -> throw new IllegalArgumentException("Provider não suportado: " + provider);
        }

        User user = userService.findOrCreateOAuthUser(
            userInfo, provider, tokenData.accessToken(), tokenData.expiresAt());

        log.info("OAuth login. provider={}, userId={}", provider, user.getId());
        return buildAuthResponse(user);
    }

    // ── Refresh token ─────────────────────────────────────────

    @Transactional
    public AuthResponse refresh(RefreshTokenRequest request) {
        String hash = jwtService.hashRefreshToken(request.refreshToken());

        RefreshToken stored = refreshTokenRepository.findByTokenHash(hash)
            .orElseThrow(() -> new IllegalArgumentException("Refresh token inválido"));

        if (!stored.isValid()) {
            throw new IllegalArgumentException("Refresh token expirado ou revogado");
        }

        // Rotação: revoga o antigo e emite novo par
        stored.revoke();
        refreshTokenRepository.save(stored);

        log.info("Token renovado. userId={}", stored.getUser().getId());
        return buildAuthResponse(stored.getUser());
    }

    // ── Logout ────────────────────────────────────────────────

    @Transactional
    public void logout(User user) {
        refreshTokenRepository.revokeAllByUser(user, LocalDateTime.now());
        log.info("Logout. userId={}", user.getId());
    }

    // ── Limpeza periódica ─────────────────────────────────────

    @Scheduled(fixedRate = 3_600_000)  // a cada 1 hora
    @Transactional
    public void cleanExpiredTokens() {
        refreshTokenRepository.deleteAllExpiredBefore(LocalDateTime.now());
        log.debug("Refresh tokens expirados removidos");
    }

    // ── internos ──────────────────────────────────────────────

    private AuthResponse buildAuthResponse(User user) {
        String accessToken  = jwtService.generateAccessToken(user);
        String rawRefresh   = jwtService.generateRefreshToken();
        String refreshHash  = jwtService.hashRefreshToken(rawRefresh);

        RefreshToken rt = RefreshToken.builder()
            .user(user)
            .tokenHash(refreshHash)
            .expiresAt(LocalDateTime.now()
                .plusSeconds(jwtService.getRefreshExpirationMs() / 1000))
            .build();
        refreshTokenRepository.save(rt);

        return new AuthResponse(
            accessToken, "Bearer",
            jwtService.getExpirationMs() / 1000,
            rawRefresh
        );
    }

    private AuthProvider parseProvider(String s) {
        try {
            return AuthProvider.valueOf(s.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Provider desconhecido: " + s);
        }
    }
}
