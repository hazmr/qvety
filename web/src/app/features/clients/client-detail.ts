import { DatePipe } from '@angular/common';
import { Component, computed, inject, signal } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { TranslocoPipe, TranslocoService } from '@jsverse/transloco';
import { NzButtonModule } from 'ng-zorro-antd/button';
import { NzTabsModule } from 'ng-zorro-antd/tabs';
import { NzTagModule } from 'ng-zorro-antd/tag';
import { ClientDto, ClientsApi } from '../../api';
import { AuthService } from '../../core/auth.service';
import { LocaleService } from '../../core/locale.service';
import { ViewportService } from '../../core/viewport.service';
import { HistoryTab } from './history-tab';
import { PatientsTab } from '../patients/patients-tab';
import { TitleBar } from '../../layout/title-bar/title-bar';
import { ActionBar } from '../../layout/action-bar/action-bar';

/**
 * Client summary with tabs: Patients and History (admin only, audit log). Edit is the title-bar action;
 * Archive sits beside it on desktop and in the phone action bar.
 */
@Component({
  selector: 'app-client-detail',
  imports: [NzTabsModule, NzButtonModule, NzTagModule, TranslocoPipe, DatePipe, HistoryTab, PatientsTab, TitleBar, ActionBar],
  templateUrl: './client-detail.html',
})
export class ClientDetail {
  private readonly api = inject(ClientsApi);
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly t = inject(TranslocoService);
  readonly auth = inject(AuthService);
  readonly locale = inject(LocaleService);
  readonly viewport = inject(ViewportService);

  readonly id = this.route.snapshot.paramMap.get('id')!;
  readonly client = signal<ClientDto | null>(null);
  readonly archived = computed(() => !!this.client()?.archivedAt);
  readonly canEdit = computed(() => this.auth.canWriteClients() && !this.archived());

  constructor() {
    this.route.paramMap.subscribe((p) => this.api.getClient(p.get('id')!).subscribe((c) => this.client.set(c)));
  }

  archive(): void {
    const c = this.client();
    if (!c || !confirm(this.t.translate('clients.confirmArchive', { name: c.fullName }))) return;
    this.api.archiveClient(c.id).subscribe((updated) => this.client.set(updated));
  }
}
