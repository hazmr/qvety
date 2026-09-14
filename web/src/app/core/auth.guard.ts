import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { catchError, map, of } from 'rxjs';
import { AuthService } from './auth.service';

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

export const adminGuard: CanActivateFn = () => {
  const auth = inject(AuthService);
  const router = inject(Router);
  return auth.isAdmin() ? true : router.createUrlTree(['/']);
};
