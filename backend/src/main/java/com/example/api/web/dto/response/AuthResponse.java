package com.example.api.web.dto.response;

public record AuthResponse(
    String accessToken,
    String tokenType,
    long expiresIn,
    String refreshToken
) {}
