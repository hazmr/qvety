import { Component, computed, inject, signal, viewChild } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute } from '@angular/router';
import { TranslocoPipe, TranslocoService } from '@jsverse/transloco';
import { NzSwitchModule } from 'ng-zorro-antd/switch';
import { map } from 'rxjs/operators';
import { ListColumn, ListPage, ListQuery } from '../../../shared/list-page/list-page';
import { ReferenceConfig, ReferenceRow, referenceConfigs } from './reference-config';

/**
 * One list for all three reference entities; the route says which. "Show inactive" is the one filter,
 * the same switch the clients list uses for archived rows, and inactive rows carry a tag. The endpoints
 * are not paged (a clinic has a handful of rows), so the rows are wrapped in the page shape.
 */
@Component({
  selector: 'app-reference-list',
  imports: [ListPage, FormsModule, NzSwitchModule, TranslocoPipe],
  template: `
    <app-list-page
      [titleKey]="config().titleKey"
      [columns]="columns()"
      [loader]="loader"
      [rowLink]="rowLink"
      [createLink]="['/settings', config().key, 'new']"
      [createKey]="config().addKey"
      [searchable]="false">
      <label list-extra class="list-extra">
        <nz-switch nzSize="small" [ngModel]="includeInactive()" (ngModelChange)="toggleInactive($event)" />
        {{ 'reference.showInactive' | transloco }}
      </label>
      <label list-extra-phone class="list-extra">
        <nz-switch [ngModel]="includeInactive()" (ngModelChange)="toggleInactive($event)" />
        {{ 'reference.showInactive' | transloco }}
      </label>
    </app-list-page>
  `,
})
export class ReferenceList {
  private readonly route = inject(ActivatedRoute);
  private readonly t = inject(TranslocoService);
  private readonly configs = referenceConfigs();
  private readonly page = viewChild.required(ListPage<ReferenceRow>);

  readonly includeInactive = signal(false);
  readonly config = computed(() => this.configs[this.route.snapshot.data['reference'] as string]);

  /** The entity's own columns plus the status tag every reference list shows. */
  readonly columns = computed<ListColumn<ReferenceRow>[]>(() => [
    ...this.config().columns,
    { key: 'active', labelKey: 'reference.status', role: 'tag', value: (r) => (r.active ? '' : this.t.translate('reference.inactive')) },
  ]);

  readonly loader = (_q: ListQuery) =>
    this.config().list(this.includeInactive()).pipe(map((rows) => ({ content: rows, page: { totalElements: rows.length } })));

  readonly rowLink = (row: ReferenceRow) => ['/settings', this.config().key, row.id];

  toggleInactive(on: boolean): void {
    this.includeInactive.set(on);
    this.page().reload();
  }
}
