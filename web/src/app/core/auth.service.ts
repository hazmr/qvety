import { Injectable, computed, inject, signal } from '@angular/core';
import { Router } from '@angular/router';
import { Observable, finalize, tap } from 'rxjs';
import { AuthApi, ChangePasswordRequestDto, LoginResponseDto, MeApi, PracticeApi, UserDto } from '../api';
import { AppLocale, LocaleService } from './locale.service';

const TOKEN_KEY = 'qvety.token';

/**
 * Token in memory plus sessionStorage (survives refresh, dies with the tab). The user record is
 * loaded from /me on restore so a deactivated or reset account drops out on the next page load.
 */
@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly authApi = inject(AuthApi);
  private readonly meApi = inject(MeApi);
  private readonly practiceApi = inject(PracticeApi);
  private readonly router = inject(Router);
  private readonly locale = inject(LocaleService);

  private readonly tokenSignal = signal<string | null>(readStoredToken());
  readonly user = signal<UserDto | null>(null);
  /** Clinic name for the brand strip; loaded once per login. */
  readonly practiceName = signal<string | null>(null);
  /** Set from a response header (part 11): payment is late, but nothing is blocked yet. */
  readonly pastDue = signal(false);

  readonly token = this.tokenSignal.asReadonly();
  readonly isLoggedIn = computed(() => this.tokenSignal() !== null);
  readonly isAdmin = computed(() => this.user()?.role === 'admin');
  /** Mirrors the server rule (front desk and admin write clients); the server still decides. */
  readonly canWriteClients = computed(() => this.user()?.role === 'admin' || this.user()?.role === 'front_desk');
  /** Clinical data (allergies now, notes and vitals later): everyone but front desk; the server still decides. */
  readonly canWriteClinical = computed(() => !!this.user() && this.user()!.role !== 'front_desk');
  readonly mustChangePassword = computed(() => this.user()?.mustChangePassword === true);

  /**
   * identifier: phone (any Egyptian shape) or email. When the password matches at several practices
   * the response carries `practices` and no token; call again with the chosen practiceId.
   */
  login(identifier: string, password: string, practiceId?: string): Observable<LoginResponseDto> {
    return this.authApi.login({ identifier, password, practiceId }).pipe(tap((r) => this.accept(r)));
  }

  changePassword(request: ChangePasswordRequestDto): Observable<LoginResponseDto> {
    return this.meApi.changePassword(request).pipe(tap((r) => this.accept(r)));
  }

  /** Called by the guard when a token exists but the user is not loaded yet (page refresh). */
  loadMe(): Observable<UserDto> {
    return this.meApi.me().pipe(tap((u) => this.setUser(u)));
  }

  /** Saves the preference on the user row and applies it at once. */
  setLocale(locale: AppLocale): Observable<UserDto> {
    return this.meApi.updateMe({ locale }).pipe(tap((u) => this.setUser(u)));
  }

  /**
   * Revokes the session on the server (every device, the accepted pilot behaviour), then drops the client
   * state in finalize so a failed call still logs the user out here.
   */
  logout(): void {
    this.authApi.logout().pipe(finalize(() => this.dropSession())).subscribe({ error: () => { /* dropped anyway */ } });
  }

  /** Client-side only: the token is already dead (401) or has just been revoked. */
  dropSession(): void {
    this.tokenSignal.set(null);
    this.user.set(null);
    this.practiceName.set(null);
    this.pastDue.set(false);
    try { sessionStorage.removeItem(TOKEN_KEY); } catch { /* storage unavailable */ }
    this.router.navigateByUrl('/login');
  }

  /** The user's stored locale drives language and direction everywhere. */
  private setUser(u: UserDto): void {
    this.user.set(u);
    if (!this.practiceName()) {
      this.practiceApi.getPractice().subscribe({ next: (p) => this.practiceName.set(p.name), error: () => { /* strip falls back to the app name */ } });
    }
    if (u.locale === 'ar-EG' || u.locale === 'en-EG') {
      this.locale.apply(u.locale);
    }
  }

  /** Only a response with a token logs the user in; a practice list leaves the state untouched. */
  private accept(r: LoginResponseDto): void {
    if (!r.token || !r.user) { return; }
    this.tokenSignal.set(r.token);
    this.setUser(r.user);
    try { sessionStorage.setItem(TOKEN_KEY, r.token); } catch { /* storage unavailable */ }
  }
}

function readStoredToken(): string | null {
  try { return sessionStorage.getItem(TOKEN_KEY); } catch { return null; }
}
