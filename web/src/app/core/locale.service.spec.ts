import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { provideTransloco } from '@jsverse/transloco';
import { provideNzI18n, ar_EG } from 'ng-zorro-antd/i18n';
import { describe, expect, it, beforeEach } from 'vitest';
import { LocaleService } from './locale.service';
import { TranslocoHttpLoader } from './transloco-loader';

/** Part 05, task 3.1: the chosen language survives a reload (localStorage) and drives dir/lang. */
describe('LocaleService', () => {
  beforeEach(() => {
    // jsdom under the Angular unit-test builder has no origin, so no localStorage; a minimal in-memory shim
    if (typeof globalThis.localStorage === 'undefined') {
      const store = new Map<string, string>();
      Object.defineProperty(globalThis, 'localStorage', {
        configurable: true,
        value: {
          getItem: (k: string) => store.get(k) ?? null,
          setItem: (k: string, v: string) => void store.set(k, String(v)),
          removeItem: (k: string) => void store.delete(k),
          clear: () => store.clear(),
        },
      });
    }
    localStorage.clear();
    document.documentElement.removeAttribute('dir');
    document.documentElement.removeAttribute('lang');
    TestBed.resetTestingModule();
    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        provideNzI18n(ar_EG),
        provideTransloco({
          config: { availableLangs: ['ar', 'en'], defaultLang: 'ar', fallbackLang: 'en', reRenderOnLangChange: true, prodMode: true },
          loader: TranslocoHttpLoader,
        }),
      ],
    });
  });

  it('defaults to Arabic RTL', () => {
    const svc = TestBed.inject(LocaleService);
    expect(svc.locale()).toBe('ar-EG');
    expect(svc.dir()).toBe('rtl');
    expect(document.documentElement.getAttribute('dir')).toBe('rtl');
    expect(document.documentElement.getAttribute('lang')).toBe('ar');
  });

  it('switches to English LTR and persists it', () => {
    const svc = TestBed.inject(LocaleService);
    svc.apply('en-EG');
    expect(svc.dir()).toBe('ltr');
    expect(document.documentElement.getAttribute('dir')).toBe('ltr');
    expect(localStorage.getItem('qvety.locale')).toBe('en-EG');
  });

  it('restores the stored choice on a fresh start (refresh)', () => {
    localStorage.setItem('qvety.locale', 'en-EG');
    const svc = TestBed.inject(LocaleService);
    expect(svc.locale()).toBe('en-EG');
    expect(document.documentElement.getAttribute('dir')).toBe('ltr');
  });
});
