// src/app/features/auth/register.component.ts
import {
  ChangeDetectionStrategy, Component, inject, signal,
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { AuthService } from '../../core/services/auth.service';

@Component({
  selector: 'app-register',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <div class="page">
      <div class="card">
        <h1>Criar conta</h1>

        @if (error()) {
          <div class="alert-error">{{ error() }}</div>
        }

        <form (ngSubmit)="submit()" #f="ngForm">
          <div class="field">
            <label>Nome</label>
            <input type="text" [(ngModel)]="name" name="name" required
                   placeholder="Seu nome completo" [disabled]="loading()" />
          </div>
          <div class="field">
            <label>E-mail</label>
            <input type="email" [(ngModel)]="email" name="email" required
                   placeholder="seu@email.com" [disabled]="loading()" />
          </div>
          <div class="field">
            <label>Senha <span class="hint">(mínimo 8 caracteres)</span></label>
            <input type="password" [(ngModel)]="password" name="password"
                   required minlength="8" placeholder="••••••••" [disabled]="loading()" />
          </div>
          <button type="submit" class="btn" [disabled]="loading() || !f.valid">
            @if (loading()) { Criando conta... } @else { Criar conta }
          </button>
        </form>

        <p class="footer-link">
          Já tem conta? <a routerLink="/login">Entrar</a>
        </p>
      </div>
    </div>
  `,
  styles: [`
    .page { min-height:100vh; display:flex; align-items:center; justify-content:center; background:#f3f4f6; padding:1rem; }
    .card { background:#fff; border-radius:12px; padding:2.5rem 2rem; width:100%; max-width:400px; box-shadow:0 4px 24px rgba(0,0,0,.08); }
    h1 { font-size:1.5rem; font-weight:700; color:#111; margin:0 0 1.5rem; text-align:center; }
    .alert-error { background:#fef2f2; border:1px solid #fca5a5; color:#dc2626; padding:.75rem 1rem; border-radius:8px; margin-bottom:1rem; font-size:.875rem; }
    .field { margin-bottom:1rem; }
    .field label { display:block; font-size:.875rem; font-weight:500; color:#374151; margin-bottom:.4rem; }
    .hint { font-weight:400; color:#9ca3af; font-size:.8rem; }
    .field input { width:100%; padding:.7rem .875rem; border:1px solid #d1d5db; border-radius:8px; font-size:.95rem; outline:none; box-sizing:border-box; transition:border .15s; }
    .field input:focus { border-color:#4285f4; }
    .btn { display:flex; align-items:center; justify-content:center; width:100%; padding:.8rem; background:#111827; color:#fff; border:none; border-radius:8px; font-size:.95rem; font-weight:500; cursor:pointer; transition:opacity .15s; }
    .btn:disabled { opacity:.6; cursor:not-allowed; }
    .btn:hover:not(:disabled) { opacity:.88; }
    .footer-link { text-align:center; margin-top:1.25rem; font-size:.875rem; color:#6b7280; }
    .footer-link a { color:#4285f4; text-decoration:none; font-weight:500; }
  `],
})
export class RegisterComponent {

  private readonly auth   = inject(AuthService);
  private readonly router = inject(Router);

  protected name     = '';
  protected email    = '';
  protected password = '';
  protected loading  = signal(false);
  protected error    = signal<string | null>(null);

  submit(): void {
    this.loading.set(true);
    this.error.set(null);

    this.auth.register(this.name, this.email, this.password).subscribe({
      next: () => this.router.navigate(['/dashboard']),
      error: (e) => {
        this.error.set(e?.error?.message ?? 'Erro ao criar conta');
        this.loading.set(false);
      },
    });
  }
}
