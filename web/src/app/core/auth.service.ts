import { Injectable, computed, inject, signal } from '@angular/core';
import { Router } from '@angular/router';
import { Observable, tap } from 'rxjs';
import { AuthApi, ChangePasswordRequestDto, LoginResponseDto, MeApi, UserDto } from '../api';

const TOKEN_KEY = 'qvety.token';

/**
 * Token in memory plus sessionStorage (survives refresh, dies with the tab). The user record is
 * loaded from /me on restore so a deactivated or reset account drops out on the next page load.
 */
@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly authApi = inject(AuthApi);
  private readonly meApi = inject(MeApi);
  private readonly router = inject(Router);

  private readonly tokenSignal = signal<string | null>(readStoredToken());
  readonly user = signal<UserDto | null>(null);

  readonly token = this.tokenSignal.asReadonly();
  readonly isLoggedIn = computed(() => this.tokenSignal() !== null);
  readonly isAdmin = computed(() => this.user()?.role === 'admin');
  readonly mustChangePassword = computed(() => this.user()?.mustChangePassword === true);

  login(email: string, password: string): Observable<LoginResponseDto> {
    return this.authApi.login({ email, password }).pipe(tap((r) => this.accept(r)));
  }

  changePassword(request: ChangePasswordRequestDto): Observable<LoginResponseDto> {
    return this.meApi.changePassword(request).pipe(tap((r) => this.accept(r)));
  }

  /** Called by the guard when a token exists but the user is not loaded yet (page refresh). */
  loadMe(): Observable<UserDto> {
    return this.meApi.me().pipe(tap((u) => this.user.set(u)));
  }

  logout(): void {
    this.tokenSignal.set(null);
    this.user.set(null);
    try { sessionStorage.removeItem(TOKEN_KEY); } catch { /* storage unavailable */ }
    this.router.navigateByUrl('/login');
  }

  private accept(r: LoginResponseDto): void {
    this.tokenSignal.set(r.token);
    this.user.set(r.user);
    try { sessionStorage.setItem(TOKEN_KEY, r.token); } catch { /* storage unavailable */ }
  }
}

function readStoredToken(): string | null {
  try { return sessionStorage.getItem(TOKEN_KEY); } catch { return null; }
}
