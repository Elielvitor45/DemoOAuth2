export const environment = {
  production: false,
  apiUrl: '/api',
  oauth2: {
    google: {
      clientId: '795460274488-l9c6ip8veb03bhljn2t6aglgiuu5i2oo.apps.googleusercontent.com',
      authorizationUri: 'https://accounts.google.com/o/oauth2/v2/auth',
      redirectUri: 'http://localhost:4200/auth/callback',
      scopes: 'openid profile email',
    },
    facebook: {
      clientId: '2747407398928533',
      authorizationUri: 'https://www.facebook.com/v18.0/dialog/oauth',
      redirectUri: 'http://localhost:4200/auth/callback',
      scopes: 'public_profile,email',
    },
  },
  tokenRefreshThresholdMs: 60_000,
} as const;