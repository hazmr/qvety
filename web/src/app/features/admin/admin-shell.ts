import { Component, inject } from '@angular/core';
import { RouterLink, RouterOutlet } from '@angular/router';
import { TranslocoPipe } from '@jsverse/transloco';
import { PlatformAuthService } from '../../core/platform-auth.service';

/**
 * The staff frame. One strip and nothing else: the admin area has a single list today, and a sidebar
 * copied from the clinic shell would be four-fifths empty.
 */
@Component({
  selector: 'app-admin-shell',
  imports: [RouterOutlet, RouterLink, TranslocoPipe],
  template: `
    <header class="strip admin-strip">
      <a class="strip__brand" routerLink="/admin/practices">
        <img src="brand/mark-white.svg" alt="" class="strip__mark" />
        <span class="strip__name">{{ 'admin.title' | transloco }}</span>
      </a>
      <span class="strip__spacer"></span>
      @if (platform.isLoggedIn()) {
        <span class="strip__user">{{ platform.staffName() }}</span>
        <button type="button" class="strip__logout" (click)="platform.logout()">{{ 'admin.signOut' | transloco }}</button>
      }
    </header>
    <main class="page-body admin-page">
      <router-outlet />
    </main>
  `,
  styles: [`
    /* A different strip colour from a clinic's, so nobody confuses the two while sharing a screen. */
    .admin-strip { background: var(--qv-ink); }
    .admin-page { padding: 16px 14px 32px; }
  `],
})
export class AdminShell {
  readonly platform = inject(PlatformAuthService);
}
