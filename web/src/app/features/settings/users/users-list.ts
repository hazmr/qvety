import { Component, inject } from '@angular/core';
import { TranslocoService } from '@jsverse/transloco';
import { map } from 'rxjs/operators';
import { UserDto, UsersApi } from '../../../api';
import { ListColumn, ListPage, ListQuery } from '../../../shared/list-page/list-page';

/** Admin only (route guard + server 403). Row actions (reset, deactivate) live on the user form. */
@Component({
  selector: 'app-users-list',
  imports: [ListPage],
  template: `
    <app-list-page
      titleKey="users.title"
      [columns]="columns"
      [loader]="loader"
      [rowLink]="rowLink"
      [createLink]="['/settings/users/new']"
      createKey="users.add"
      [searchable]="false" />
  `,
})
export class UsersList {
  private readonly api = inject(UsersApi);
  private readonly t = inject(TranslocoService);

  readonly columns: ListColumn<UserDto>[] = [
    { key: 'fullName', labelKey: 'users.name', role: 'title' },
    { key: 'phone', labelKey: 'users.phone', ltr: true, mono: true, role: 'secondary' },
    { key: 'email', labelKey: 'users.email', ltr: true, role: 'hidden' },
    { key: 'role', labelKey: 'users.role', role: 'secondary', value: (u) => this.t.translate('roles.' + u.role) },
    { key: 'licenseNumber', labelKey: 'users.license', ltr: true, mono: true, role: 'hidden' },
    { key: 'status', labelKey: 'users.status', role: 'tag', value: (u) => this.t.translate(
        !u.active ? 'users.statusInactive' : u.mustChangePassword ? 'users.statusPending' : 'users.statusActive') },
  ];

  /** The users endpoint is not paged (a clinic has a handful); wrap the list in the page shape. */
  readonly loader = (_q: ListQuery) => this.api.listUsers().pipe(map((users) => ({ content: users, page: { totalElements: users.length } })));

  readonly rowLink = (u: UserDto) => ['/settings/users', u.id];

}
