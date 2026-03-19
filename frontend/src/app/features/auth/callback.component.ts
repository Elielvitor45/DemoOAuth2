// src/app/features/auth/callback.component.ts
import {
  ChangeDetectionStrategy, Component, OnInit, inject, signal,
} from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { AuthService } from '../../core/services/auth.service';

/**
 * Fluxo OAuth2 neste projeto:
 *
 * 1. Angular redireciona o browser para o provedor (Google/Facebook)
 *    com redirect_uri apontando para o BACKEND:
 *    http://localhost:8080/login/oauth2/code/google
 *
 * 2. O provedor redireciona para o backend com ?code=...
 *
 * 3. O backend Spring Boot recebe o code, troca pelo access_token do provedor,
 *    busca o userinfo, cria/atualiza o usuário no banco,
 *    gera um JWT interno e redireciona o browser para:
 *    http://localhost:4200/auth/callback?provider=google&code=<JWT_INTERNO>
 *    (ou poderia usar um parâmetro diferente — depende da implementação do backend)
 *
 * ATENÇÃO: como o backend neste projeto NÃO redireciona de volta para o Angular
 * (ele apenas expõe POST /api/v1/auth/oauth/callback), o fluxo correto é:
 *
 * Opção A (implementada aqui): o backend completa o fluxo e redireciona para
 * o Angular com o JWT na URL: /auth/oauth-success?token=JWT&refreshToken=RT
 *
 * Opção B: Angular chama diretamente o provedor com redirect_uri apontando
 * para o Angular, recebe o code aqui, e envia para o backend via POST.
 *
 * Esta implementação usa a Opção B: o Angular recebe o code do provedor
 * e envia ao backend via POST /api/v1/auth/oauth/callback.
 *
 * Para isso, o redirect_uri nos consoles deve ser:
 * http://localhost:4200/auth/callback
 * (não o endpoint do backend)
 */
@Component({
  selector: 'app-callback',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <div class="page">
      <div class="card">
        @if (status() === 'processing') {
          <div class="spinner"></div>
          <p>Autenticando, aguarde...</p>
        }
        @if (status() === 'error') {
          <div class="error-icon">✕</div>
          <h2>Falha na autenticação</h2>
          <p class="error-msg">{{ message() }}</p>
          <button (click)="goLogin()">Tentar novamente</button>
        }
      </div>
    </div>
  `,
  styles: [`
    .page { min-height:100vh; display:flex; align-items:center; justify-content:center; background:#f3f4f6; }
    .card { background:#fff; border-radius:12px; padding:2.5rem 2rem; text-align:center; min-width:280px; box-shadow:0 4px 24px rgba(0,0,0,.08); }
    .spinner { width:44px; height:44px; border:4px solid #e5e7eb; border-top-color:#4285f4; border-radius:50%; animation:spin .8s linear infinite; margin:0 auto 1.25rem; }
    @keyframes spin { to { transform:rotate(360deg); } }
    p { color:#6b7280; margin:0; }
    .error-icon { width:48px; height:48px; background:#fee2e2; color:#dc2626; border-radius:50%; display:flex; align-items:center; justify-content:center; font-size:1.4rem; font-weight:700; margin:0 auto .75rem; }
    h2 { color:#111; margin:0 0 .5rem; }
    .error-msg { color:#6b7280; font-size:.9rem; margin:0 0 1.25rem; }
    button { background:#4285f4; color:#fff; border:none; border-radius:8px; padding:.7rem 1.5rem; cursor:pointer; font-size:.95rem; }
  `],
})
export class CallbackComponent implements OnInit {

  private readonly route  = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly auth   = inject(AuthService);

  protected status  = signal<'processing' | 'error'>('processing');
  protected message = signal('');

  ngOnInit(): void {
    const params   = this.route.snapshot.queryParams;
    const code     = params['code']  as string;
    const error    = params['error'] as string;
    const provider = sessionStorage.getItem('oauth_provider') ?? 'google';

    if (error) {
      this.fail('Acesso negado pelo provedor.');
      return;
    }
    if (!code) {
      this.fail('Parâmetro "code" ausente.');
      return;
    }

    // Limpa estado temporário do sessionStorage
    sessionStorage.removeItem('oauth_state');
    sessionStorage.removeItem('oauth_provider');

    // Envia o code para o backend completar o fluxo OAuth2
    this.auth.handleOAuthCode(provider, code).subscribe({
      next: () => this.router.navigate(['/dashboard'], { replaceUrl: true }),
      error: (e) => this.fail(e?.error?.message ?? 'Falha ao autenticar com o servidor.'),
    });
  }

  protected goLogin(): void { this.router.navigate(['/login']); }

  private fail(msg: string): void {
    this.message.set(msg);
    this.status.set('error');
  }
}
