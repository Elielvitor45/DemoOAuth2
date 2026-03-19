// src/app/core/interceptors/auth.interceptor.ts
import { HttpInterceptorFn, HttpRequest, HttpHandlerFn, HttpErrorResponse } from '@angular/common/http';
import { inject } from '@angular/core';
import { catchError, switchMap, throwError } from 'rxjs';
import { AuthService } from '../services/auth.service';
import { environment } from '../../../environments/environment';

export const authInterceptor: HttpInterceptorFn = (
  req: HttpRequest<unknown>,
  next: HttpHandlerFn,
) => {
  const auth = inject(AuthService);

  // Não intercepta requisições fora da nossa API
  if (!req.url.includes('/api/')) return next(req);

  // Endpoints de auth são públicos — não adiciona token
  if (req.url.includes('/auth/login') || req.url.includes('/auth/register') ||
      req.url.includes('/auth/oauth') || req.url.includes('/auth/refresh')) {
    return next(req);
  }

  const token = auth.getAccessToken();

  // Refresh proativo se o token está próximo de expirar
  if (token && auth.isNearExpiry() && !auth.isExpired()) {
    return auth.refreshToken().pipe(
      switchMap(() => next(addToken(req, auth.getAccessToken()))),
      catchError(err => { auth.logout(); return throwError(() => err); }),
    );
  }

  return next(addToken(req, token)).pipe(
    catchError(err => {
      // 401 após enviar token: tenta refresh uma vez
      if (err instanceof HttpErrorResponse && err.status === 401 && token) {
        return auth.refreshToken().pipe(
          switchMap(() => next(addToken(req, auth.getAccessToken()))),
          catchError(e => { auth.logout(); return throwError(() => e); }),
        );
      }
      return throwError(() => err);
    }),
  );
};

function addToken(req: HttpRequest<unknown>, token: string | null): HttpRequest<unknown> {
  if (!token) return req;
  return req.clone({ setHeaders: { Authorization: `Bearer ${token}` } });
}
