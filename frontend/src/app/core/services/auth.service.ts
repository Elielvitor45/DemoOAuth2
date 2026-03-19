// src/app/core/services/auth.service.ts
import { Injectable, OnDestroy, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Router } from '@angular/router';
import {
  BehaviorSubject, Observable, Subscription,
  catchError, filter, of, switchMap, tap, timer,
} from 'rxjs';
import { environment } from '../../../environments/environment';
import { AuthResponse, OAuthProvider, TokenState } from '../models/auth.model';
import { User } from '../models/user.model';

@Injectable({ providedIn: 'root' })
export class AuthService implements OnDestroy {

  private readonly http   = inject(HttpClient);
  private readonly router = inject(Router);
  private readonly api    = environment.apiUrl;

  private tokenState: TokenState | null = null;
  private refreshTimer: Subscription | null = null;

  readonly isAuthenticated$ = new BehaviorSubject<boolean>(false);
  readonly currentUser$     = new BehaviorSubject<User | null>(null);

  readonly isAuthenticated = this.isAuthenticated$.asObservable();
  readonly currentUser     = this.currentUser$.asObservable();

  // ── OAuth2 ───────────────────────────────────────────────────

  redirectToProvider(provider: OAuthProvider): void {
    const cfg = environment.oauth2[provider];
    const state = this.generateState();
    sessionStorage.setItem('oauth_state', state);
    sessionStorage.setItem('oauth_provider', provider);

    const params = new URLSearchParams({
      client_id:     (cfg as any).clientId,
      redirect_uri:  cfg.redirectUri,
      response_type: 'code',
      scope:         cfg.scopes,
      state,
    });

    window.location.href = `${cfg.authorizationUri}?${params.toString()}`;
  }

  handleOAuthCode(provider: string, code: string): Observable<User> {
    return this.http.post<AuthResponse>(`${this.api}/v1/auth/oauth/callback`, { provider, code })
      .pipe(
        tap((r: AuthResponse) => this.storeTokens(r)),
        switchMap(() => this.loadMe()),
      );
  }

  // ── Local auth ───────────────────────────────────────────────

  login(email: string, password: string): Observable<User> {
    return this.http.post<AuthResponse>(`${this.api}/v1/auth/login`, { email, password })
      .pipe(
        tap((r: AuthResponse) => this.storeTokens(r)),
        switchMap(() => this.loadMe()),
      );
  }

  register(name: string, email: string, password: string): Observable<User> {
    return this.http.post<AuthResponse>(`${this.api}/v1/auth/register`, { name, email, password })
      .pipe(
        tap((r: AuthResponse) => this.storeTokens(r)),
        switchMap(() => this.loadMe()),
      );
  }

  // ── Token management ─────────────────────────────────────────

  refreshToken(): Observable<AuthResponse> {
    if (!this.tokenState?.refreshToken) {
      this.logout();
      return new Observable<AuthResponse>(subscriber =>
        subscriber.error(new Error('Sem refresh token'))
      );
    }
    return this.http.post<AuthResponse>(`${this.api}/v1/auth/refresh`, {
      refreshToken: this.tokenState.refreshToken,
    }).pipe(
      tap((r: AuthResponse) => this.storeTokens(r)),
      // Tipo explícito no catchError resolve: Observable<AuthResponse> is not assignable to Observable<unknown>
      catchError((err): Observable<AuthResponse> => {
        this.logout();
        return new Observable<AuthResponse>(subscriber => subscriber.error(err));
      }),
    );
  }

  logout(): void {
    if (this.tokenState?.accessToken) {
      this.http.post(`${this.api}/v1/auth/logout`, {})
        .pipe(catchError(() => of(null))).subscribe();
    }
    this.clearState();
    this.router.navigate(['/login']);
  }

  getAccessToken(): string | null { return this.tokenState?.accessToken ?? null; }
  isLoggedIn(): boolean           { return this.isAuthenticated$.getValue() && !this.isExpired(); }
  isExpired(): boolean            { return !this.tokenState || Date.now() >= this.tokenState.expiresAt; }

  isNearExpiry(): boolean {
    if (!this.tokenState) return true;
    return Date.now() >= this.tokenState.expiresAt - environment.tokenRefreshThresholdMs;
  }

  loadMe(): Observable<User> {
    return this.http.get<User>(`${this.api}/v1/users/me`).pipe(
      tap((u: User) => this.currentUser$.next(u)),
    );
  }

  getUserSnapshot(): User | null { return this.currentUser$.getValue(); }

  // ── Internos ─────────────────────────────────────────────────

  private storeTokens(r: AuthResponse): void {
    this.tokenState = {
      accessToken:  r.accessToken,
      refreshToken: r.refreshToken,
      expiresAt:    Date.now() + r.expiresIn * 1000,
    };
    this.isAuthenticated$.next(true);
    this.scheduleRefresh(r.expiresIn);
  }

  private scheduleRefresh(expiresInSeconds: number): void {
    this.refreshTimer?.unsubscribe();
    const delay = Math.max(0, expiresInSeconds * 1000 - environment.tokenRefreshThresholdMs);
    this.refreshTimer = timer(delay).pipe(
      filter(() => this.isAuthenticated$.getValue()),
      switchMap(() => this.refreshToken()),
    ).subscribe({ error: (e: unknown) => console.error('[Auth] Auto-refresh falhou:', e) });
  }

  private clearState(): void {
    this.tokenState = null;
    this.isAuthenticated$.next(false);
    this.currentUser$.next(null);
    this.refreshTimer?.unsubscribe();
    this.refreshTimer = null;
  }

  private generateState(): string {
    const arr = new Uint8Array(32);
    crypto.getRandomValues(arr);
    return btoa(String.fromCharCode(...arr)).replace(/[+/=]/g, c =>
      c === '+' ? '-' : c === '/' ? '_' : '');
  }

  ngOnDestroy(): void { this.refreshTimer?.unsubscribe(); }
}