import { DOCUMENT } from '@angular/common';
import { Injectable, computed, inject, signal } from '@angular/core';
import { TranslocoService } from '@jsverse/transloco';
import { NzI18nService, ar_EG, en_US } from 'ng-zorro-antd/i18n';

export type AppLocale = 'ar-EG' | 'en-EG';
export type AppLang = 'ar' | 'en';

const STORAGE_KEY = 'qvety.locale';

/**
 * One place that switches language and direction: Transloco, document dir/lang (the CDK [dir] on the app
 * root follows the signal and tells NG-ZORRO components) and NG-ZORRO i18n. The user's stored locale (users.locale) wins; before login the last choice on this
 * device is used, Arabic by default.
 */
@Injectable({ providedIn: 'root' })
export class LocaleService {
  private readonly document = inject(DOCUMENT);
  private readonly transloco = inject(TranslocoService);
  private readonly nzI18n = inject(NzI18nService);

  readonly locale = signal<AppLocale>(readStored() ?? 'ar-EG');
  readonly lang = computed<AppLang>(() => (this.locale() === 'ar-EG' ? 'ar' : 'en'));
  readonly dir = computed<'rtl' | 'ltr'>(() => (this.lang() === 'ar' ? 'rtl' : 'ltr'));
  /** The other locale, for the toggle. */
  readonly other = computed<AppLocale>(() => (this.locale() === 'ar-EG' ? 'en-EG' : 'ar-EG'));

  constructor() {
    this.apply(this.locale());
  }

  apply(locale: AppLocale): void {
    this.locale.set(locale);
    const lang = locale === 'ar-EG' ? 'ar' : 'en';
    const dir = lang === 'ar' ? 'rtl' : 'ltr';
    this.transloco.setActiveLang(lang);
    this.document.documentElement.setAttribute('lang', lang);
    this.document.documentElement.setAttribute('dir', dir);
    this.nzI18n.setLocale(lang === 'ar' ? ar_EG : en_US);
    try { localStorage.setItem(STORAGE_KEY, locale); } catch { /* storage unavailable */ }
  }
}

function readStored(): AppLocale | null {
  try {
    const v = localStorage.getItem(STORAGE_KEY);
    return v === 'ar-EG' || v === 'en-EG' ? v : null;
  } catch {
    return null;
  }
}
