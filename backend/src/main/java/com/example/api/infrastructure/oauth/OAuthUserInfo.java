package com.example.api.infrastructure.oauth;

import lombok.Builder;

@Builder
public record OAuthUserInfo(String providerId, String email, String name, String photoUrl) {}
