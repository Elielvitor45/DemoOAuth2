// src/app/features/dashboard/dashboard.component.ts
import {
  ChangeDetectionStrategy, Component, OnInit, inject,
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';
import { AuthService } from '../../core/services/auth.service';
import { User, PROVIDER_LABELS, PROVIDER_COLORS, ProviderName } from '../../core/models/user.model';
import { Observable } from 'rxjs';

@Component({
  selector: 'app-dashboard',
  standalone: true,
  imports: [CommonModule, RouterLink],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <div class="layout">

      <!-- Navbar -->
      <header class="navbar">
        <span class="brand">MyApp</span>
        <button class="btn-logout" (click)="logout()">Sair</button>
      </header>

      <!-- Conteúdo -->
      <main class="main">
        @if (user$ | async; as user) {

          <div class="profile-card">

            <!-- Foto ou avatar inicial -->
            <div class="avatar-wrap">
              @if (user.photoUrl) {
                <img
                  [src]="user.photoUrl"
                  [alt]="user.name"
                  class="avatar"
                  width="96" height="96"
                  referrerpolicy="no-referrer"
                />
              } @else {
                <div class="avatar-fallback">
                  {{ user.name.charAt(0).toUpperCase() }}
                </div>
              }
            </div>

            <!-- Dados -->
            <h2 class="user-name">{{ user.name }}</h2>
            <p class="user-email">{{ user.email ?? 'E-mail não disponível' }}</p>

            <!-- Badges dos providers com os quais esse usuário já logou -->
            <div class="providers-row">
              @for (p of user.providers; track p) {
                <span
                  class="provider-badge"
                  [style.background]="getColor(p)"
                  [title]="'Vinculado via ' + getLabel(p)"
                >
                  @if (p === 'GOOGLE') {
                    <svg width="14" height="14" viewBox="0 0 24 24">
                      <path fill="white" d="M22.56 12.25c0-.78-.07-1.53-.2-2.25H12v4.26h5.92c-.26 1.37-1.04 2.53-2.21 3.31v2.77h3.57c2.08-1.92 3.28-4.74 3.28-8.09z"/>
                      <path fill="white" d="M12 23c2.97 0 5.46-.98 7.28-2.66l-3.57-2.77c-.98.66-2.23 1.06-3.71 1.06-2.86 0-5.29-1.93-6.16-4.53H2.18v2.84C3.99 20.53 7.7 23 12 23z"/>
                      <path fill="white" d="M5.84 14.09c-.22-.66-.35-1.36-.35-2.09s.13-1.43.35-2.09V7.07H2.18C1.43 8.55 1 10.22 1 12s.43 3.45 1.18 4.93l3.66-2.84z"/>
                      <path fill="white" d="M12 5.38c1.62 0 3.06.56 4.21 1.64l3.15-3.15C17.45 2.09 14.97 1 12 1 7.7 1 3.99 3.47 2.18 7.07l3.66 2.84c.87-2.6 3.3-4.53 6.16-4.53z"/>
                    </svg>
                  }
                  @if (p === 'FACEBOOK') {
                    <svg width="14" height="14" viewBox="0 0 24 24" fill="white">
                      <path d="M24 12.073c0-6.627-5.373-12-12-12s-12 5.373-12 12c0 5.99 4.388 10.954 10.125 11.854v-8.385H7.078v-3.47h3.047V9.43c0-3.007 1.792-4.669 4.533-4.669 1.312 0 2.686.235 2.686.235v2.953H15.83c-1.491 0-1.956.925-1.956 1.874v2.25h3.328l-.532 3.47h-2.796v8.385C19.612 23.027 24 18.062 24 12.073z"/>
                    </svg>
                  }
                  @if (p === 'LOCAL') {
                    <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="white" stroke-width="2">
                      <path d="M20 21v-2a4 4 0 0 0-4-4H8a4 4 0 0 0-4 4v2"/>
                      <circle cx="12" cy="7" r="4"/>
                    </svg>
                  }
                  {{ getLabel(p) }}
                </span>
              }
            </div>

          </div>

        } @else {
          <div class="loading">Carregando...</div>
        }
      </main>

    </div>
  `,
  styles: [`
    .layout { min-height:100vh; background:#f3f4f6; }

    .navbar {
      background:#fff;
      border-bottom:1px solid #e5e7eb;
      padding:1rem 2rem;
      display:flex;
      align-items:center;
      justify-content:space-between;
    }
    .brand { font-size:1.1rem; font-weight:700; color:#111; }
    .btn-logout {
      background:#ef4444; color:#fff; border:none;
      border-radius:6px; padding:.45rem .9rem;
      font-size:.875rem; cursor:pointer;
      transition:opacity .15s;
    }
    .btn-logout:hover { opacity:.88; }

    .main {
      max-width:480px;
      margin:3rem auto;
      padding:0 1rem;
    }

    .profile-card {
      background:#fff;
      border-radius:16px;
      padding:2.5rem 2rem;
      text-align:center;
      box-shadow:0 4px 24px rgba(0,0,0,.07);
    }

    .avatar-wrap { margin-bottom:1rem; }

    .avatar {
      width:96px; height:96px;
      border-radius:50%;
      object-fit:cover;
      border:3px solid #e5e7eb;
    }

    .avatar-fallback {
      width:96px; height:96px;
      border-radius:50%;
      background:#4285f4;
      color:#fff;
      font-size:2.5rem;
      font-weight:700;
      display:flex;
      align-items:center;
      justify-content:center;
      margin:0 auto;
    }

    .user-name {
      font-size:1.4rem;
      font-weight:700;
      color:#111;
      margin:0 0 .35rem;
    }

    .user-email {
      color:#6b7280;
      font-size:.9rem;
      margin:0 0 1.5rem;
    }

    /* Linha com badges de provedores */
    .providers-row {
      display:flex;
      gap:.5rem;
      justify-content:center;
      flex-wrap:wrap;
    }

    .provider-badge {
      display:inline-flex;
      align-items:center;
      gap:.35rem;
      padding:.3rem .75rem;
      border-radius:100px;
      color:#fff;
      font-size:.8rem;
      font-weight:500;
    }

    .loading {
      text-align:center;
      color:#6b7280;
      padding:3rem;
    }
  `],
})
export class DashboardComponent implements OnInit {

  private readonly auth = inject(AuthService);

  protected user$!: Observable<User | null>;

  ngOnInit(): void {
    this.user$ = this.auth.currentUser;
    if (!this.auth.getUserSnapshot()) {
      this.auth.loadMe().subscribe();
    }
  }

  protected getLabel(p: ProviderName): string {
    return PROVIDER_LABELS[p] ?? p;
  }

  protected getColor(p: ProviderName): string {
    return PROVIDER_COLORS[p] ?? '#6b7280';
  }

  protected logout(): void {
    this.auth.logout();
  }
}
