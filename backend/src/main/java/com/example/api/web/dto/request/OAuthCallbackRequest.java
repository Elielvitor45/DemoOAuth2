package com.example.api.web.dto.request;

import jakarta.validation.constraints.NotBlank;

public record OAuthCallbackRequest(
    @NotBlank String provider,
    @NotBlank String code
) {}
