import { Injectable, signal } from '@angular/core';

const PHONE = '(max-width: 767.98px)';
const SIDEBAR_KEY = 'qvety.sidebar';

/**
 * One phone layout below 768 px, desktop above (docs/design/MOBILE.md). Templates switch structure on
 * isPhone(); an action bar on screen hides the bottom nav (they are mutually exclusive).
 */
@Injectable({ providedIn: 'root' })
export class ViewportService {
  readonly isPhone = signal(false);
  readonly actionBarPresent = signal(false);
  readonly sidebarCollapsed = signal(readSidebar());

  constructor() {
    if (typeof window !== 'undefined' && window.matchMedia) {
      const mq = window.matchMedia(PHONE);
      this.isPhone.set(mq.matches);
      mq.addEventListener('change', (e) => this.isPhone.set(e.matches));
    }
  }

  toggleSidebar(): void {
    this.sidebarCollapsed.update((v) => !v);
    try { localStorage.setItem(SIDEBAR_KEY, String(this.sidebarCollapsed())); } catch { /* storage unavailable */ }
  }
}

function readSidebar(): boolean {
  try { return localStorage.getItem(SIDEBAR_KEY) === 'true'; } catch { return false; }
}
