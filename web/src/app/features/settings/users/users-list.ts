import { Component, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { NzButtonModule } from 'ng-zorro-antd/button';
import { NzTableModule } from 'ng-zorro-antd/table';
import { NzTagModule } from 'ng-zorro-antd/tag';
import { UserDto, UsersApi } from '../../../api';

/** Admin only (route guard + server 403). Part 06 replaces this with the generic list-page. */
@Component({
  selector: 'app-users-list',
  imports: [RouterLink, NzTableModule, NzButtonModule, NzTagModule],
  templateUrl: './users-list.html',
})
export class UsersList {
  private readonly api = inject(UsersApi);
  readonly users = signal<UserDto[]>([]);
  readonly loading = signal(true);

  constructor() {
    this.reload();
  }

  reload(): void {
    this.loading.set(true);
    this.api.list().subscribe({
      next: (u) => { this.users.set(u); this.loading.set(false); },
      error: () => this.loading.set(false),
    });
  }

  deactivate(u: UserDto): void {
    if (!confirm(`Deactivate ${u.fullName}?`)) return;
    this.api.deactivate(u.id).subscribe(() => this.reload());
  }

  resetPassword(u: UserDto): void {
    const temporary = prompt(`Temporary password for ${u.fullName} (at least 10 characters):`);
    if (!temporary) return;
    this.api.resetPassword(u.id, { temporaryPassword: temporary }).subscribe(() => this.reload());
  }
}
