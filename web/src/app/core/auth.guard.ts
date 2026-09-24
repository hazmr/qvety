import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { catchError, map, of } from 'rxjs';
import { AuthService } from './auth.service';
import { PlatformAuthService } from './platform-auth.service';

/** Logged in, user loaded, and password change forced when the server says so. */
export const authGuard: CanActivateFn = (route, state) => {
  const auth = inject(AuthService);
  const router = inject(Router);
  if (!auth.isLoggedIn()) {
    return router.createUrlTree(['/login'], { queryParams: { next: state.url } });
  }
  const decide = () => {
    const onChange = state.url.startsWith('/change-password');
    if (auth.mustChangePassword() && !onChange) return router.createUrlTree(['/change-password']);
    return true;
  };
  if (auth.user()) return decide();
  return auth.loadMe().pipe(
    map(() => decide()),
    catchError(() => of(router.createUrlTree(['/login']))),
  );
};

/** The Qvety staff area. A clinic login is no help here, and the server refuses either way. */
export const platformGuard: CanActivateFn = (route, state) => {
  const platform = inject(PlatformAuthService);
  const router = inject(Router);
  return platform.isLoggedIn() ? true : router.createUrlTree(['/admin/login'], { queryParams: { next: state.url } });
};

export const adminGuard: CanActivateFn = () => {
  const auth = inject(AuthService);
  const router = inject(Router);
  return auth.isAdmin() ? true : router.createUrlTree(['/']);
};
