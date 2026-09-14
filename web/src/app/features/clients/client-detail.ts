import { DatePipe } from '@angular/common';
import { Component, computed, inject, signal } from '@angular/core';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { TranslocoPipe, TranslocoService } from '@jsverse/transloco';
import { NzButtonModule } from 'ng-zorro-antd/button';
import { NzTabsModule } from 'ng-zorro-antd/tabs';
import { NzTagModule } from 'ng-zorro-antd/tag';
import { ClientDto, ClientsApi } from '../../api';
import { AuthService } from '../../core/auth.service';
import { LocaleService } from '../../core/locale.service';
import { HistoryTab } from './history-tab';

/** Client summary with tabs: Patients (part 08 fills it) and History (admin only, audit log). */
@Component({
  selector: 'app-client-detail',
  imports: [RouterLink, NzTabsModule, NzButtonModule, NzTagModule, TranslocoPipe, DatePipe, HistoryTab],
  templateUrl: './client-detail.html',
})
export class ClientDetail {
  private readonly api = inject(ClientsApi);
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly t = inject(TranslocoService);
  readonly auth = inject(AuthService);
  readonly locale = inject(LocaleService);

  readonly id = this.route.snapshot.paramMap.get('id')!;
  readonly client = signal<ClientDto | null>(null);
  readonly archived = computed(() => !!this.client()?.archivedAt);

  constructor() {
    this.route.paramMap.subscribe((p) => this.api.getClient(p.get('id')!).subscribe((c) => this.client.set(c)));
  }

  archive(): void {
    const c = this.client();
    if (!c || !confirm(this.t.translate('clients.confirmArchive', { name: c.fullName }))) return;
    this.api.archiveClient(c.id).subscribe((updated) => this.client.set(updated));
  }
}
