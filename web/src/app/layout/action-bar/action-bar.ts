import { Component, OnDestroy, inject } from '@angular/core';
import { ViewportService } from '../../core/viewport.service';

/**
 * The screen's actions. Phone: a 48 px bar stuck to the bottom (safe-area aware) that replaces the bottom
 * nav while present. Desktop: an inline row. Put one or two buttons inside; nothing else.
 */
@Component({
  selector: 'app-action-bar',
  template: `<div class="action-bar" [class.action-bar--phone]="viewport.isPhone()"><ng-content /></div>`,
  styleUrl: './action-bar.scss',
})
export class ActionBar implements OnDestroy {
  readonly viewport = inject(ViewportService);

  constructor() {
    this.viewport.actionBarPresent.set(true);
  }

  ngOnDestroy(): void {
    this.viewport.actionBarPresent.set(false);
  }
}
