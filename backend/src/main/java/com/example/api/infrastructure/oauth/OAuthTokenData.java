package com.example.api.infrastructure.oauth;

import java.time.LocalDateTime;

public record OAuthTokenData(String accessToken, LocalDateTime expiresAt) {}
