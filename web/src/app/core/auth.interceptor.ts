import { HttpErrorResponse, HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { HttpResponse } from '@angular/common/http';
import { catchError, tap, throwError } from 'rxjs';
import { AuthService } from './auth.service';
import { LocaleService } from './locale.service';
import { PlatformAuthService } from './platform-auth.service';

/**
 * Adds the bearer token and Accept-Language (the user's locale, so backend messages match the UI).
 * Two sessions can be open at once, a clinic one and a Qvety staff one, so the token is chosen by the
 * path: /api/platform is the staff side, everything else is the clinic.
 * A 401 on any call means the token is dead (expired, revoked), so drop the session client-side; calling
 * the logout endpoint with a dead token would only 401 again.
 */
const PAST_DUE_HEADER = 'X-Practice-Past-Due';

export const authInterceptor: HttpInterceptorFn = (req, next) => {
  const auth = inject(AuthService);
  const platform = inject(PlatformAuthService);
  const locale = inject(LocaleService);
  const isPlatform = req.url.includes('/api/platform/');
  const token = isPlatform ? platform.token() : auth.token();
  const isLogin = req.url.endsWith('/api/v1/auth/login') || req.url.endsWith('/api/platform/auth/login');
  const headers: Record<string, string> = { 'Accept-Language': locale.locale() };
  if (token && !isLogin) {
    headers['Authorization'] = `Bearer ${token}`;
  }
  return next(req.clone({ setHeaders: headers })).pipe(
    tap((event) => {
      // The server marks a practice behind on payment; the shell turns this into one banner.
      if (event instanceof HttpResponse && !isPlatform && event.headers.has(PAST_DUE_HEADER)) {
        auth.pastDue.set(event.headers.get(PAST_DUE_HEADER) === 'true');
      }
    }),
    catchError((err: unknown) => {
      if (err instanceof HttpErrorResponse && err.status === 401 && token) {
        if (isPlatform) { platform.dropSession(); } else { auth.dropSession(); }
      }
      return throwError(() => err);
    }),
  );
};
