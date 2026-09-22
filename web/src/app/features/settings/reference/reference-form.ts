import { Component, computed, inject, signal, viewChild } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { TranslocoPipe, TranslocoService } from '@jsverse/transloco';
import { NzButtonModule } from 'ng-zorro-antd/button';
import { TitleBar } from '../../../layout/title-bar/title-bar';
import { FormPage } from '../../../shared/form-page/form-page';
import { ReferenceRow, referenceConfigs } from './reference-config';

/**
 * Create or edit one reference row. Deactivate and Activate sit beside Save, the way Archive and
 * Unarchive sit on the client form: the row is never deleted, because appointments (part 10) and
 * invoice lines (part 14) point at it.
 */
@Component({
  selector: 'app-reference-form',
  imports: [FormPage, TitleBar, NzButtonModule, TranslocoPipe],
  template: `
    <div class="form-layout">
      <app-title-bar [titleKey]="isNew ? config().addKey : config().titleKey" [back]="['/settings', config().key]" />
      <app-form-page [fields]="config().fields" [value]="current()" [busy]="busy()" (submitted)="save($event)">
        <!-- One button, not two in an @if: ng-content select= does not match nodes inside a control-flow block. -->
        <button secondary-action nz-button type="button" [hidden]="!current()"
                [nzDanger]="current()?.active === true" (click)="toggleActive()">
          {{ (current()?.active === false ? 'reference.activate' : 'reference.deactivate') | transloco }}
        </button>
      </app-form-page>
    </div>
  `,
})
export class ReferenceForm {
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly t = inject(TranslocoService);
  private readonly configs = referenceConfigs();
  private readonly page = viewChild.required(FormPage);

  readonly id = this.route.snapshot.paramMap.get('id');
  readonly isNew = this.id === null;
  readonly busy = signal(false);
  readonly current = signal<ReferenceRow | null>(null);
  readonly config = computed(() => this.configs[this.route.snapshot.data['reference'] as string]);

  constructor() {
    if (this.id) {
      this.config().get(this.id).subscribe((row) => this.current.set(row));
    }
  }

  save(values: Record<string, string>): void {
    this.busy.set(true);
    const request = this.isNew
      ? this.config().create(values)
      : this.config().update(this.id!, this.current()!.version, values);
    request.subscribe({
      next: () => this.router.navigate(['/settings', this.config().key]),
      error: (e) => { this.busy.set(false); this.page().fail(e); },
    });
  }

  /**
   * Deactivate asks first and leaves the screen, because the row disappears from the list behind it.
   * Activate does not: bringing a row back is not destructive, and the screen stays to show the new state.
   */
  toggleActive(): void {
    const row = this.current();
    if (!row) return;
    if (!row.active) {
      this.config().activate(row.id).subscribe((updated) => this.current.set(updated));
      return;
    }
    if (!confirm(this.t.translate('reference.confirmDeactivate', { name: row.name }))) return;
    this.config().deactivate(row.id).subscribe(() => this.router.navigate(['/settings', this.config().key]));
  }
}
