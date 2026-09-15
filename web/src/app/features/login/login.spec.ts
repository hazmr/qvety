import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { provideRouter, Router } from '@angular/router';
import { provideTransloco } from '@jsverse/transloco';
import { provideNzI18n, ar_EG } from 'ng-zorro-antd/i18n';
import { provideNzIcons } from 'ng-zorro-antd/icon';
import { RightOutline } from '@ant-design/icons-angular/icons';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import { provideApi } from '../../api';
import { AuthService } from '../../core/auth.service';
import { TranslocoHttpLoader } from '../../core/transloco-loader';
import { Login } from './login';

/** Part 03b: the same phone at two practices. One match logs in; several show the picker; the choice posts again. */
describe('Login', () => {
  let http: HttpTestingController;
  let router: Router;

  beforeEach(() => {
    try { sessionStorage.clear(); } catch { /* no storage in this jsdom */ }   // AuthService restores a token from it
    TestBed.resetTestingModule();
    TestBed.configureTestingModule({
      imports: [Login],
      providers: [
        provideRouter([]),
        provideHttpClient(),
        provideHttpClientTesting(),
        provideApi(''),
        provideNzI18n(ar_EG),
        provideNzIcons([RightOutline]),
        provideTransloco({
          config: { availableLangs: ['ar', 'en'], defaultLang: 'ar', fallbackLang: 'en', reRenderOnLangChange: true, prodMode: true },
          loader: TranslocoHttpLoader,
        }),
      ],
    });
    http = TestBed.inject(HttpTestingController);
    router = TestBed.inject(Router);
    vi.spyOn(router, 'navigateByUrl').mockResolvedValue(true);
  });

  function loginRequest() {
    return http.expectOne((r) => r.method === 'POST' && r.url.endsWith('/api/v1/auth/login'));
  }

  const user = { id: 'u1', phone: '+201000000104', fullName: 'Desk', role: 'front_desk', locale: 'ar-EG', mustChangePassword: false };

  it('logs in straight away when the password matched one practice', () => {
    const fixture = TestBed.createComponent(Login);
    const cmp = fixture.componentInstance;
    cmp.form.setValue({ identifier: '01000000104', password: 'password123' });
    cmp.submit();
    const req = loginRequest();
    expect(req.request.body).toEqual({ identifier: '01000000104', password: 'password123', practiceId: undefined });
    req.flush({ token: 't', user });

    expect(cmp.practices()).toBeNull();
    expect(TestBed.inject(AuthService).token()).toBe('t');
    expect(router.navigateByUrl).toHaveBeenCalledWith('/');
  });

  it('shows the practices when the password matched several, then posts the choice', () => {
    const fixture = TestBed.createComponent(Login);
    const cmp = fixture.componentInstance;
    cmp.form.setValue({ identifier: '01000000104', password: 'password123' });
    cmp.submit();
    loginRequest().flush({ practices: [{ id: 'p1', name: 'Clinic A' }, { id: 'p4', name: 'Fourth Clinic' }] });

    expect(cmp.practices()?.map((p) => p.name)).toEqual(['Clinic A', 'Fourth Clinic']);
    expect(TestBed.inject(AuthService).token()).toBeNull();          // nothing accepted yet
    expect(cmp.form.controls.identifier.disabled).toBe(true);        // the choice belongs to this identifier
    expect(router.navigateByUrl).not.toHaveBeenCalled();

    cmp.choose({ id: 'p4', name: 'Fourth Clinic' });
    const second = loginRequest();
    expect(second.request.body).toEqual({ identifier: '01000000104', password: 'password123', practiceId: 'p4' });
    second.flush({ token: 't4', user });

    expect(TestBed.inject(AuthService).token()).toBe('t4');
    expect(router.navigateByUrl).toHaveBeenCalledWith('/');
  });

  it('goes back to the form on "use another account"', () => {
    const fixture = TestBed.createComponent(Login);
    const cmp = fixture.componentInstance;
    cmp.form.setValue({ identifier: '01000000104', password: 'password123' });
    cmp.submit();
    loginRequest().flush({ practices: [{ id: 'p1', name: 'A' }, { id: 'p4', name: 'D' }] });

    cmp.cancelChoice();
    expect(cmp.practices()).toBeNull();
    expect(cmp.form.controls.identifier.enabled).toBe(true);
    expect(cmp.form.controls.password.value).toBe('');
  });
});
