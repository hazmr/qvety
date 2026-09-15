import { DatePipe } from '@angular/common';
import { Component, computed, inject, signal } from '@angular/core';
import { RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { TranslocoPipe, TranslocoService } from '@jsverse/transloco';
import { NzButtonModule } from 'ng-zorro-antd/button';
import { NzDrawerModule } from 'ng-zorro-antd/drawer';
import { NzIconModule } from 'ng-zorro-antd/icon';
import { AuthService } from '../core/auth.service';
import { LocaleService } from '../core/locale.service';
import { ViewportService } from '../core/viewport.service';

interface NavItem { link: string; labelKey: string; icon: string; adminOnly?: boolean; exact?: boolean; }

/**
 * The frame. Phone (< 768 px): 32 px brand strip, one scroller, 60 px bottom nav (hidden while a screen
 * shows an action bar), "Me" sheet from the initials. Desktop: 32 px header strip plus a 196 px sidebar
 * that collapses to the mark. docs/design/frontend.md "Application chrome" and docs/design/MOBILE.md.
 */
@Component({
  selector: 'app-shell',
  imports: [RouterOutlet, RouterLink, RouterLinkActive, NzButtonModule, NzIconModule, NzDrawerModule, TranslocoPipe, DatePipe],
  templateUrl: './shell.html',
  styleUrl: './shell.scss',
})
export class Shell {
  readonly auth = inject(AuthService);
  readonly locale = inject(LocaleService);
  readonly viewport = inject(ViewportService);
  private readonly t = inject(TranslocoService);
  readonly today = new Date();
  readonly meOpen = signal(false);

  private readonly allItems: NavItem[] = [
    { link: '/', labelKey: 'app.home', icon: 'home', exact: true },
    { link: '/clients', labelKey: 'app.clients', icon: 'team' },
    { link: '/settings/users', labelKey: 'app.users', icon: 'setting', adminOnly: true },
  ];
  readonly items = computed(() => this.allItems.filter((i) => !i.adminOnly || this.auth.isAdmin()));
  readonly initials = computed(() => initialsOf(this.auth.user()?.fullName ?? ''));
  /** Nav label for the Me cell: the first word of the name, or the generic label while loading. */
  readonly firstName = computed(() => (this.auth.user()?.fullName ?? '').trim().split(/\s+/).filter(Boolean).slice(0, 2).join(' ') || this.t.translate('app.me'));

  toggleLanguage(): void {
    const next = this.locale.other();
    this.locale.apply(next);                       // immediate
    this.auth.setLocale(next).subscribe();         // persisted on the user row
  }
}

/** First letter of the first two words; works for Arabic and Latin names. */
function initialsOf(name: string): string {
  return name.trim().split(/\s+/).filter(Boolean).slice(0, 2).map((w) => w[0]).join('');
}
