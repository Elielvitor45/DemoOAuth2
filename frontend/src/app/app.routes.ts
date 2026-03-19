// src/app/app.routes.ts
import { Routes } from '@angular/router';
import { authGuard, unauthGuard } from './core/guards/auth.guard';

export const APP_ROUTES: Routes = [
  { path: '', redirectTo: 'dashboard', pathMatch: 'full' },

  {
    path: 'login',
    canActivate: [unauthGuard],
    loadComponent: () => import('./features/auth/login.component').then(m => m.LoginComponent),
    title: 'Login',
  },
  {
    path: 'register',
    canActivate: [unauthGuard],
    loadComponent: () => import('./features/auth/register.component').then(m => m.RegisterComponent),
    title: 'Criar conta',
  },
  {
    // Recebe ?code=...&state=... do provedor OAuth2
    path: 'auth/callback',
    loadComponent: () => import('./features/auth/callback.component').then(m => m.CallbackComponent),
    title: 'Autenticando...',
  },
  {
    path: 'dashboard',
    canActivate: [authGuard],
    loadComponent: () => import('./features/dashboard/dashboard.component').then(m => m.DashboardComponent),
    title: 'Dashboard',
  },

  { path: '**', redirectTo: 'dashboard' },
];
