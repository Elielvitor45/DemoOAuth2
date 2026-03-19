// src/app/core/models/user.model.ts

export interface User {
  id: number;
  name: string;
  email: string | null;
  photoUrl: string | null;
  providers: ProviderName[];
}

export type ProviderName = 'GOOGLE' | 'FACEBOOK' | 'LOCAL';

export const PROVIDER_LABELS: Record<ProviderName, string> = {
  GOOGLE:   'Google',
  FACEBOOK: 'Facebook',
  LOCAL:    'Email',
};

export const PROVIDER_COLORS: Record<ProviderName, string> = {
  GOOGLE:   '#4285f4',
  FACEBOOK: '#1877f2',
  LOCAL:    '#6b7280',
};
