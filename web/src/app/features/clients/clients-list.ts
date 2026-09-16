import { Component, inject, signal, viewChild } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { TranslocoPipe, TranslocoService } from '@jsverse/transloco';
import { NzSwitchModule } from 'ng-zorro-antd/switch';
import { ClientDto, ClientsApi } from '../../api';
import { ListColumn, ListPage, ListQuery } from '../../shared/list-page/list-page';

/** Clients list. "Show archived" (part 06c) is the one filter; archived rows carry a tag. */
@Component({
  selector: 'app-clients-list',
  imports: [ListPage, FormsModule, NzSwitchModule, TranslocoPipe],
  template: `
    <app-list-page
      titleKey="clients.title"
      [columns]="columns"
      [loader]="loader"
      [rowLink]="rowLink"
      [createLink]="['/clients/new']"
      createKey="clients.add">
      <label list-extra class="list-extra">
        <nz-switch nzSize="small" [ngModel]="includeArchived()" (ngModelChange)="toggleArchived($event)" />
        {{ 'clients.showArchived' | transloco }}
      </label>
      <label list-extra-phone class="list-extra">
        <nz-switch [ngModel]="includeArchived()" (ngModelChange)="toggleArchived($event)" />
        {{ 'clients.showArchived' | transloco }}
      </label>
    </app-list-page>
  `,
})
export class ClientsList {
  private readonly api = inject(ClientsApi);
  private readonly t = inject(TranslocoService);
  private readonly page = viewChild.required(ListPage<ClientDto>);

  readonly includeArchived = signal(false);

  readonly columns: ListColumn<ClientDto>[] = [
    { key: 'fullName', labelKey: 'clients.fullName', sortable: true, role: 'title' },
    { key: 'phone', labelKey: 'clients.phone', ltr: true, mono: true, role: 'secondary' },
    { key: 'email', labelKey: 'clients.email', ltr: true, role: 'secondary' },
    { key: 'address', labelKey: 'clients.address', role: 'hidden' },
    { key: 'archivedAt', labelKey: 'clients.status', role: 'tag', value: (c) => (c.archivedAt ? this.t.translate('clients.archived') : '') },
  ];

  readonly loader = (q: ListQuery) =>
    this.api.listClients(q.q || undefined, this.includeArchived(), q.page, q.size, q.sort ? [q.sort] : ['fullName,asc']);

  readonly rowLink = (c: ClientDto) => ['/clients', c.id];

  toggleArchived(on: boolean): void {
    this.includeArchived.set(on);
    this.page().reload();
  }
}
