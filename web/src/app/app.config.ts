import { ApplicationConfig, provideBrowserGlobalErrorListeners } from '@angular/core';
import { provideHttpClient, withInterceptors } from '@angular/common/http';
import { provideRouter } from '@angular/router';
import { provideNzI18n, en_US } from 'ng-zorro-antd/i18n';
import { provideApi } from './api';
import { routes } from './app.routes';

// Part 05 replaces en_US with the user's locale and adds Transloco + direction.
export const appConfig: ApplicationConfig = {
  providers: [
    provideBrowserGlobalErrorListeners(),
    provideRouter(routes),
    provideHttpClient(withInterceptors([])),
    // Relative base path: the dev proxy and the jar both serve /api on the same origin.
    provideApi(''),
    provideNzI18n(en_US),
  ],
};
