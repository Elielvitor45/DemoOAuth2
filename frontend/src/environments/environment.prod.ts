export const environment = {
  production: true,
  apiUrl: 'https://api.seudominio.com/api',
  oauth2: {
    google: {
      clientId: 'SEU_GOOGLE_CLIENT_ID_PROD',
      authorizationUri: 'https://accounts.google.com/o/oauth2/v2/auth',
      redirectUri: 'https://seudominio.com/auth/callback',
      scopes: 'openid profile email',
    },
    facebook: {
      clientId: 'SEU_FACEBOOK_APP_ID_PROD',
      authorizationUri: 'https://www.facebook.com/v18.0/dialog/oauth',
      redirectUri: 'https://seudominio.com/auth/callback',
      scopes: 'public_profile,email',
    },
  },
  tokenRefreshThresholdMs: 60_000,
} as const;
