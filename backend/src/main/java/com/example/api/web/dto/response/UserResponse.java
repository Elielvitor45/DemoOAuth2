package com.example.api.web.dto.response;

import java.util.List;

public record UserResponse(
    Long id,
    String name,
    String email,
    String photoUrl,
    List<String> providers   // ex: ["GOOGLE", "LOCAL"]
) {}
