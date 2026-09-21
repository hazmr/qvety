import { HttpErrorResponse, HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { catchError, throwError } from 'rxjs';
import { AuthService } from './auth.service';
import { LocaleService } from './locale.service';

/**
 * Adds the bearer token and Accept-Language (the user's locale, so backend messages match the UI).
 * A 401 on any call means the token is dead (expired, revoked), so drop the session client-side; calling
 * the logout endpoint with a dead token would only 401 again.
 */
export const authInterceptor: HttpInterceptorFn = (req, next) => {
  const auth = inject(AuthService);
  const locale = inject(LocaleService);
  const token = auth.token();
  const headers: Record<string, string> = { 'Accept-Language': locale.locale() };
  if (token && !req.url.endsWith('/api/v1/auth/login')) {
    headers['Authorization'] = `Bearer ${token}`;
  }
  return next(req.clone({ setHeaders: headers })).pipe(
    catchError((err: unknown) => {
      if (err instanceof HttpErrorResponse && err.status === 401 && token) {
        auth.dropSession();
      }
      return throwError(() => err);
    }),
  );
};
