import { Component, inject, signal } from '@angular/core';
import { TranslocoPipe, TranslocoService } from '@jsverse/transloco';
import { NzButtonModule } from 'ng-zorro-antd/button';
import { PlatformApi } from '../../api';
import { AuthService } from '../../core/auth.service';
import { apiMessage } from '../../core/api-error';
import { save } from '../admin/practice-detail';
import { TitleBar } from '../../layout/title-bar/title-bar';

/**
 * The clinic's own copy of everything. Reachable in every status, including closed, because the answer
 * to "what happens to my data if you disappear" has to be a button rather than a promise.
 */
@Component({
  selector: 'app-practice-export',
  imports: [TitleBar, NzButtonModule, TranslocoPipe],
  template: `
    <div class="form-layout">
      <app-title-bar titleKey="export.title" [back]="['/settings']" />
      <div class="form-card">
        <p>{{ 'export.body' | transloco }}</p>
        @if (error(); as e) { <p class="home__error">{{ e }}</p> }
        <button nz-button nzType="primary" [nzLoading]="busy()" (click)="download()">
          {{ (busy() ? 'export.preparing' : 'export.download') | transloco }}
        </button>
      </div>
    </div>
  `,
})
export class PracticeExport {
  private readonly api = inject(PlatformApi);
  private readonly auth = inject(AuthService);
  private readonly t = inject(TranslocoService);

  readonly busy = signal(false);
  readonly error = signal<string | null>(null);

  download(): void {
    this.busy.set(true);
    this.error.set(null);
    this.api.exportOwnPractice('response').subscribe({
      next: (response) => {
        this.busy.set(false);
        const name = (this.auth.practiceName() ?? 'qvety').replace(/\s+/g, '-').toLowerCase();
        save(JSON.stringify(response.body), `${name}-export.json`);
      },
      error: (e) => {
        this.busy.set(false);
        this.error.set(apiMessage(e, this.t.translate('shared.saveFailed')));
      },
    });
  }
}
