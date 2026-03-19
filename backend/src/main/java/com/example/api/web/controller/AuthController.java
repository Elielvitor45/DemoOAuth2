package com.example.api.web.controller;

import com.example.api.application.auth.AuthService;
import com.example.api.application.user.UserService;
import com.example.api.web.dto.request.*;
import com.example.api.web.dto.response.AuthResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final UserService userService;

    /** POST /api/v1/auth/register — cadastro com email + senha */
    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(authService.registerLocal(req));
    }

    /** POST /api/v1/auth/login — login com email + senha */
    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest req) {
        return ResponseEntity.ok(authService.loginLocal(req));
    }

    /**
     * POST /api/v1/auth/oauth/callback
     * Recebe o code do Angular (após redirect do provedor) e troca por JWT interno.
     */
    @PostMapping("/oauth/callback")
    public ResponseEntity<AuthResponse> oauthCallback(@Valid @RequestBody OAuthCallbackRequest req) {
        return ResponseEntity.ok(authService.processOAuthCallback(req));
    }

    /** POST /api/v1/auth/refresh */
    @PostMapping("/refresh")
    public ResponseEntity<AuthResponse> refresh(@Valid @RequestBody RefreshTokenRequest req) {
        return ResponseEntity.ok(authService.refresh(req));
    }

    /** POST /api/v1/auth/logout — requer Bearer token */
    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@AuthenticationPrincipal UserDetails userDetails) {
        userService.findByEmail(userDetails.getUsername())
            .ifPresent(authService::logout);
        return ResponseEntity.noContent().build();
    }
}
