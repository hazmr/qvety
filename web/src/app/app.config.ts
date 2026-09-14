import { registerLocaleData } from '@angular/common';
import localeAr from '@angular/common/locales/ar-EG';
import localeEn from '@angular/common/locales/en';
import { ApplicationConfig, isDevMode, provideBrowserGlobalErrorListeners } from '@angular/core';
import { provideHttpClient, withInterceptors } from '@angular/common/http';
import { provideRouter } from '@angular/router';
import { provideTransloco } from '@jsverse/transloco';
import { provideNzI18n, ar_EG } from 'ng-zorro-antd/i18n';
import { provideApi } from './api';
import { authInterceptor } from './core/auth.interceptor';
import { TranslocoHttpLoader } from './core/transloco-loader';
import { routes } from './app.routes';

registerLocaleData(localeAr);
registerLocaleData(localeEn);

export const appConfig: ApplicationConfig = {
  providers: [
    provideBrowserGlobalErrorListeners(),
    provideRouter(routes),
    provideHttpClient(withInterceptors([authInterceptor])),
    // Relative base path: the dev proxy and the jar both serve /api on the same origin.
    provideApi(''),
    // Arabic first; LocaleService switches language, direction, and NG-ZORRO i18n at runtime.
    provideNzI18n(ar_EG),
    provideTransloco({
      config: {
        availableLangs: ['ar', 'en'],
        defaultLang: 'ar',
        fallbackLang: 'en',
        reRenderOnLangChange: true,
        prodMode: !isDevMode(),
      },
      loader: TranslocoHttpLoader,
    }),
  ],
};
