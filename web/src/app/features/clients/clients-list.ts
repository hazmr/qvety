import { Component, inject } from '@angular/core';
import { ClientDto, ClientsApi } from '../../api';
import { ListColumn, ListPage, ListQuery } from '../../shared/list-page/list-page';

@Component({
  selector: 'app-clients-list',
  imports: [ListPage],
  template: `
    <app-list-page
      titleKey="clients.title"
      [columns]="columns"
      [loader]="loader"
      [rowLink]="rowLink"
      [createLink]="['/clients/new']"
      createKey="clients.add" />
  `,
})
export class ClientsList {
  private readonly api = inject(ClientsApi);

  readonly columns: ListColumn<ClientDto>[] = [
    { key: 'fullName', labelKey: 'clients.fullName', sortable: true, role: 'title' },
    { key: 'phone', labelKey: 'clients.phone', ltr: true, mono: true, role: 'secondary' },
    { key: 'email', labelKey: 'clients.email', ltr: true, role: 'secondary' },
    { key: 'address', labelKey: 'clients.address', role: 'hidden' },
  ];

  readonly loader = (q: ListQuery) =>
    this.api.listClients(q.q || undefined, q.page, q.size, q.sort ? [q.sort] : ['fullName,asc']);

  readonly rowLink = (c: ClientDto) => ['/clients', c.id];
}
