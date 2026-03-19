// src/app/features/auth/login.component.ts
import {
  ChangeDetectionStrategy, Component, OnInit,
  inject, signal,
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router, ActivatedRoute, RouterLink } from '@angular/router';
import { AuthService } from '../../core/services/auth.service';

@Component({
  selector: 'app-login',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <div class="page">
      <div class="card">
        <h1>Entrar</h1>

        @if (error()) {
          <div class="alert alert-error">{{ error() }}</div>
        }

        <!-- Botões OAuth2 -->
        <div class="oauth-buttons">
          <button class="btn btn-google" (click)="loginWith('google')" [disabled]="loading()">
            <svg width="18" height="18" viewBox="0 0 24 24">
              <path fill="#fff" d="M22.56 12.25c0-.78-.07-1.53-.2-2.25H12v4.26h5.92c-.26 1.37-1.04 2.53-2.21 3.31v2.77h3.57c2.08-1.92 3.28-4.74 3.28-8.09z"/>
              <path fill="#fff" d="M12 23c2.97 0 5.46-.98 7.28-2.66l-3.57-2.77c-.98.66-2.23 1.06-3.71 1.06-2.86 0-5.29-1.93-6.16-4.53H2.18v2.84C3.99 20.53 7.7 23 12 23z"/>
              <path fill="#fff" d="M5.84 14.09c-.22-.66-.35-1.36-.35-2.09s.13-1.43.35-2.09V7.07H2.18C1.43 8.55 1 10.22 1 12s.43 3.45 1.18 4.93l3.66-2.84z"/>
              <path fill="#fff" d="M12 5.38c1.62 0 3.06.56 4.21 1.64l3.15-3.15C17.45 2.09 14.97 1 12 1 7.7 1 3.99 3.47 2.18 7.07l3.66 2.84c.87-2.6 3.3-4.53 6.16-4.53z"/>
            </svg>
            Continuar com Google
          </button>

          <button class="btn btn-facebook" (click)="loginWith('facebook')" [disabled]="loading()">
            <svg width="18" height="18" viewBox="0 0 24 24" fill="white">
              <path d="M24 12.073c0-6.627-5.373-12-12-12s-12 5.373-12 12c0 5.99 4.388 10.954 10.125 11.854v-8.385H7.078v-3.47h3.047V9.43c0-3.007 1.792-4.669 4.533-4.669 1.312 0 2.686.235 2.686.235v2.953H15.83c-1.491 0-1.956.925-1.956 1.874v2.25h3.328l-.532 3.47h-2.796v8.385C19.612 23.027 24 18.062 24 12.073z"/>
            </svg>
            Continuar com Facebook
          </button>
        </div>

        <div class="divider"><span>ou</span></div>

        <!-- Formulário email + senha -->
        <form (ngSubmit)="loginEmail()" #f="ngForm">
          <div class="field">
            <label>E-mail</label>
            <input type="email" [(ngModel)]="email" name="email" required
                   placeholder="seu@email.com" [disabled]="loading()" />
          </div>
          <div class="field">
            <label>Senha</label>
            <input type="password" [(ngModel)]="password" name="password" required
                   placeholder="••••••••" [disabled]="loading()" />
          </div>
          <button type="submit" class="btn btn-primary" [disabled]="loading() || !f.valid">
            @if (loading()) { Entrando... } @else { Entrar }
          </button>
        </form>

        <p class="footer-link">
          Não tem conta? <a routerLink="/register">Criar conta</a>
        </p>
      </div>
    </div>
  `,
  styles: [`
    .page { min-height:100vh; display:flex; align-items:center; justify-content:center; background:#f3f4f6; padding:1rem; }
    .card { background:#fff; border-radius:12px; padding:2.5rem 2rem; width:100%; max-width:400px; box-shadow:0 4px 24px rgba(0,0,0,.08); }
    h1 { font-size:1.5rem; font-weight:700; color:#111; margin:0 0 1.5rem; text-align:center; }
    .alert-error { background:#fef2f2; border:1px solid #fca5a5; color:#dc2626; padding:.75rem 1rem; border-radius:8px; margin-bottom:1rem; font-size:.875rem; }
    .oauth-buttons { display:flex; flex-direction:column; gap:.75rem; }
    .btn { display:flex; align-items:center; justify-content:center; gap:.6rem; padding:.8rem 1rem; border:none; border-radius:8px; font-size:.95rem; font-weight:500; cursor:pointer; transition:opacity .15s; width:100%; }
    .btn:disabled { opacity:.6; cursor:not-allowed; }
    .btn:hover:not(:disabled) { opacity:.88; }
    .btn-google   { background:#4285f4; color:#fff; }
    .btn-facebook { background:#1877f2; color:#fff; }
    .btn-primary  { background:#111827; color:#fff; margin-top:.25rem; }
    .divider { text-align:center; margin:1.25rem 0; position:relative; }
    .divider::before { content:''; position:absolute; top:50%; left:0; right:0; height:1px; background:#e5e7eb; }
    .divider span { background:#fff; padding:0 .75rem; color:#9ca3af; font-size:.875rem; position:relative; }
    .field { margin-bottom:1rem; }
    .field label { display:block; font-size:.875rem; font-weight:500; color:#374151; margin-bottom:.4rem; }
    .field input { width:100%; padding:.7rem .875rem; border:1px solid #d1d5db; border-radius:8px; font-size:.95rem; outline:none; box-sizing:border-box; transition:border .15s; }
    .field input:focus { border-color:#4285f4; }
    .footer-link { text-align:center; margin-top:1.25rem; font-size:.875rem; color:#6b7280; }
    .footer-link a { color:#4285f4; text-decoration:none; font-weight:500; }
  `],
})
export class LoginComponent implements OnInit {

  private readonly auth   = inject(AuthService);
  private readonly router = inject(Router);
  private readonly route  = inject(ActivatedRoute);

  protected email    = '';
  protected password = '';
  protected loading  = signal(false);
  protected error    = signal<string | null>(null);

  private returnUrl = '/dashboard';

  ngOnInit(): void {
    this.returnUrl = this.route.snapshot.queryParams['returnUrl'] ?? '/dashboard';
    const err = this.route.snapshot.queryParams['error'];
    if (err) this.error.set(this.humanize(err));
  }

  loginWith(provider: 'google' | 'facebook'): void {
    this.error.set(null);
    this.auth.redirectToProvider(provider);
  }

  loginEmail(): void {
    if (!this.email || !this.password) return;
    this.loading.set(true);
    this.error.set(null);

    this.auth.login(this.email, this.password).subscribe({
      next: () => this.router.navigateByUrl(this.returnUrl),
      error: (e) => {
        this.error.set(e?.error?.message ?? 'Email ou senha incorretos');
        this.loading.set(false);
      },
    });
  }

  private humanize(code: string): string {
    const map: Record<string, string> = {
      access_denied: 'Acesso negado. Você cancelou o login.',
      oauth_error:   'Erro no login social. Tente novamente.',
    };
    return map[code] ?? `Erro: ${code}`;
  }
}
