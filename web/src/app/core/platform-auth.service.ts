import { Injectable, computed, inject, signal } from '@angular/core';
import { Router } from '@angular/router';
import { Observable, finalize, tap } from 'rxjs';
import { PlatformApi, PlatformLoginResponseDto } from '../api';

const TOKEN_KEY = 'qvety.platform.token';
const NAME_KEY = 'qvety.platform.name';

/**
 * Qvety staff, kept entirely apart from the clinic session. A separate token under its own key, so
 * signing into the admin area never touches a practice login and neither can stand in for the other.
 * The interceptor picks between them by URL.
 */
@Injectable({ providedIn: 'root' })
export class PlatformAuthService {
  private readonly api = inject(PlatformApi);
  private readonly router = inject(Router);

  private readonly tokenSignal = signal<string | null>(read(TOKEN_KEY));
  readonly staffName = signal<string | null>(read(NAME_KEY));

  readonly token = this.tokenSignal.asReadonly();
  readonly isLoggedIn = computed(() => this.tokenSignal() !== null);

  login(email: string, password: string): Observable<PlatformLoginResponseDto> {
    return this.api.platformLogin({ email, password }).pipe(tap((r) => this.accept(r)));
  }

  /** No server call: a platform token carries no session to revoke beyond its own expiry. */
  logout(): void {
    this.dropSession();
  }

  dropSession(): void {
    this.tokenSignal.set(null);
    this.staffName.set(null);
    write(TOKEN_KEY, null);
    write(NAME_KEY, null);
    this.router.navigateByUrl('/admin/login');
  }

  private accept(r: PlatformLoginResponseDto): void {
    this.tokenSignal.set(r.token);
    this.staffName.set(r.fullName);
    write(TOKEN_KEY, r.token);
    write(NAME_KEY, r.fullName);
  }
}

function read(key: string): string | null {
  try { return sessionStorage.getItem(key); } catch { return null; }
}

function write(key: string, value: string | null): void {
  try {
    if (value === null) { sessionStorage.removeItem(key); } else { sessionStorage.setItem(key, value); }
  } catch { /* storage unavailable */ }
}
