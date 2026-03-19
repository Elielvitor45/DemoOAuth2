// src/app/core/models/auth.model.ts

export interface AuthResponse {
  accessToken: string;
  tokenType: 'Bearer';
  expiresIn: number;
  refreshToken: string;
}

export interface TokenState {
  accessToken: string;
  refreshToken: string;
  expiresAt: number; // timestamp ms
}

export type OAuthProvider = 'google' | 'facebook';
