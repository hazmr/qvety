import { DatePipe } from '@angular/common';
import { Component, inject, signal } from '@angular/core';
import { Router, RouterLink } from '@angular/router';
import { TranslocoPipe } from '@jsverse/transloco';
import { NzButtonModule } from 'ng-zorro-antd/button';
import { PlatformApi, PracticeSummaryDto } from '../../api';
import { LocaleService } from '../../core/locale.service';
import { TitleBar } from '../../layout/title-bar/title-bar';

/**
 * Every practice Qvety has. Stacked rows at both sizes: there are tens of these, not thousands, and the
 * same list reads fine on a phone when a clinic calls out of hours.
 */
@Component({
  selector: 'app-practices-list',
  imports: [TitleBar, RouterLink, NzButtonModule, TranslocoPipe, DatePipe],
  template: `
    <app-title-bar titleKey="admin.practices" actionKey="admin.add" [actionLink]="['/admin/practices/new']" />
    <ul class="rows">
      @for (p of practices(); track p.id) {
        <li class="row" [routerLink]="['/admin/practices', p.id]">
          <div class="row__text">
            <div class="row__title">{{ p.name }}</div>
            <div class="row__secondary">
              {{ p.country }} · {{ p.currency }} ·
              {{ 'admin.created' | transloco }} {{ p.createdAt | date:'mediumDate':'':locale.angularLocale() }}
            </div>
          </div>
          <span class="row__tag" [class.row__tag--muted]="p.status === 'closed'"
                [class.row__tag--warn]="p.status === 'past_due' || p.status === 'suspended'">
            {{ 'practiceStatus.' + p.status | transloco }}
          </span>
        </li>
      } @empty {
        @if (!loading()) { <li class="row row--empty muted">{{ 'admin.noPractices' | transloco }}</li> }
      }
    </ul>
  `,
})
export class PracticesList {
  private readonly api = inject(PlatformApi);
  readonly locale = inject(LocaleService);

  readonly practices = signal<PracticeSummaryDto[]>([]);
  readonly loading = signal(true);

  constructor() {
    this.api.listPractices().subscribe({
      next: (rows) => { this.practices.set(rows); this.loading.set(false); },
      error: () => this.loading.set(false),
    });
  }
}
