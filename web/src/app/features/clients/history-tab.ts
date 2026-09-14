import { DatePipe, JsonPipe } from '@angular/common';
import { Component, effect, inject, input, signal } from '@angular/core';
import { TranslocoPipe } from '@jsverse/transloco';
import { NzTableModule } from 'ng-zorro-antd/table';
import { AuditApi, AuditEntryDto } from '../../api';
import { LocaleService } from '../../core/locale.service';

/**
 * "History" tab: who changed what, from the trigger-written audit log. Admin only (the server enforces it).
 * Generic over table/rowId so every later detail page reuses it.
 */
@Component({
  selector: 'app-history-tab',
  imports: [NzTableModule, TranslocoPipe, DatePipe, JsonPipe],
  templateUrl: './history-tab.html',
})
export class HistoryTab {
  private readonly api = inject(AuditApi);
  readonly locale = inject(LocaleService);

  readonly table = input.required<string>();
  readonly rowId = input.required<string>();
  readonly entries = signal<AuditEntryDto[]>([]);
  readonly loading = signal(true);

  constructor() {
    effect(() => {
      this.loading.set(true);
      this.api.listAudit(this.table(), this.rowId(), 0, 50).subscribe({
        next: (p) => { this.entries.set(p.content ?? []); this.loading.set(false); },
        error: () => this.loading.set(false),
      });
    });
  }

  /** Only the columns whose value changed, so an update reads as a diff. */
  changes(e: AuditEntryDto): { field: string; before: unknown; after: unknown }[] {
    const before = (e.before ?? {}) as Record<string, unknown>;
    const after = (e.after ?? {}) as Record<string, unknown>;
    const keys = new Set([...Object.keys(before), ...Object.keys(after)]);
    const out: { field: string; before: unknown; after: unknown }[] = [];
    for (const k of keys) {
      if (k === 'updated_at' || k === 'version') continue;
      if (JSON.stringify(before[k]) !== JSON.stringify(after[k])) out.push({ field: k, before: before[k], after: after[k] });
    }
    return out;
  }
}
