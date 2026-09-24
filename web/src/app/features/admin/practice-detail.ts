import { DatePipe } from '@angular/common';
import { Component, computed, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute } from '@angular/router';
import { TranslocoPipe, TranslocoService } from '@jsverse/transloco';
import { NzButtonModule } from 'ng-zorro-antd/button';
import { NzInputModule } from 'ng-zorro-antd/input';
import { NzSelectModule } from 'ng-zorro-antd/select';
import { PlatformApi, PracticeSummaryDto } from '../../api';
import { apiMessage } from '../../core/api-error';
import { LocaleService } from '../../core/locale.service';
import { TitleBar } from '../../layout/title-bar/title-bar';

/** Statuses a person may set by hand. The rest are the subscription job's (part 12). */
const MANUAL: PracticeSummaryDto.StatusEnum[] = ['active', 'past_due', 'suspended', 'closed'];

/** One practice: what state it is in, how to move it, and how to take a copy of its data. */
@Component({
  selector: 'app-practice-detail',
  imports: [TitleBar, FormsModule, NzButtonModule, NzInputModule, NzSelectModule, TranslocoPipe, DatePipe],
  templateUrl: './practice-detail.html',
})
export class PracticeDetail {
  private readonly api = inject(PlatformApi);
  private readonly route = inject(ActivatedRoute);
  private readonly t = inject(TranslocoService);
  readonly locale = inject(LocaleService);

  readonly id = this.route.snapshot.paramMap.get('id')!;
  readonly practice = signal<PracticeSummaryDto | null>(null);
  readonly error = signal<string | null>(null);
  readonly busy = signal(false);
  readonly exporting = signal(false);

  readonly status = signal<PracticeSummaryDto.StatusEnum>('active');
  readonly reason = signal('');
  /** Moving a practice to the state it is already in is not a change worth an audit row. */
  readonly options = computed(() => MANUAL.filter((s) => s !== this.practice()?.status));

  constructor() {
    this.api.getPlatformPractice(this.id).subscribe((p) => {
      this.practice.set(p);
      this.status.set(this.options()[0] ?? 'suspended');
    });
  }

  changeStatus(): void {
    if (!this.reason().trim()) { return; }
    this.busy.set(true);
    this.error.set(null);
    this.api.changePracticeStatus(this.id, { status: this.status(), reason: this.reason().trim() }).subscribe({
      next: (p) => {
        this.busy.set(false);
        this.practice.set(p);
        this.reason.set('');
        this.status.set(this.options()[0] ?? 'suspended');
      },
      error: (e) => {
        this.busy.set(false);
        this.error.set(apiMessage(e, this.t.translate('shared.saveFailed')));
      },
    });
  }

  /** The browser cannot follow a link that needs a bearer token, so the file is fetched and handed over. */
  download(): void {
    this.exporting.set(true);
    this.api.exportPractice(this.id, 'response').subscribe({
      next: (response) => {
        this.exporting.set(false);
        save(JSON.stringify(response.body), `qvety-export-${this.id}.json`);
      },
      error: (e) => {
        this.exporting.set(false);
        this.error.set(apiMessage(e, this.t.translate('shared.saveFailed')));
      },
    });
  }
}

/** Hands a blob to the browser as a download and cleans the object URL up after. */
export function save(content: string, filename: string): void {
  const url = URL.createObjectURL(new Blob([content], { type: 'application/json' }));
  const link = document.createElement('a');
  link.href = url;
  link.download = filename;
  link.click();
  URL.revokeObjectURL(url);
}
