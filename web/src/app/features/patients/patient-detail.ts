import { DatePipe, DecimalPipe } from '@angular/common';
import { Component, computed, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { TranslocoPipe, TranslocoService } from '@jsverse/transloco';
import { NzAlertModule } from 'ng-zorro-antd/alert';
import { NzButtonModule } from 'ng-zorro-antd/button';
import { NzSelectModule } from 'ng-zorro-antd/select';
import { NzTabsModule } from 'ng-zorro-antd/tabs';
import { NzTagModule } from 'ng-zorro-antd/tag';
import { Subject, debounceTime, distinctUntilChanged, switchMap } from 'rxjs';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { ClientDto, ClientsApi, PatientDetailDto, PatientsApi } from '../../api';
import { apiMessage } from '../../core/api-error';
import { AuthService } from '../../core/auth.service';
import { LocaleService } from '../../core/locale.service';
import { ViewportService } from '../../core/viewport.service';
import { ActionBar } from '../../layout/action-bar/action-bar';
import { TitleBar } from '../../layout/title-bar/title-bar';
import { HistoryTab } from '../clients/history-tab';
import { AllergiesTab } from './allergies-tab';
import { ageLabel, isoDate } from './patient-labels';
import { WeightsTab } from './weights-tab';

/**
 * Patient record (MOBILE.md screen 10): title bar with back chevron, identity block, allergy banner, tab strip
 * (Summary, Weights, Allergies, Visits). Edit is the title-bar action; Transfer sits on the owner row;
 * Mark deceased is beside Edit on desktop and in the phone action bar. Visits fill in part 13.
 */
@Component({
  selector: 'app-patient-detail',
  imports: [RouterLink, FormsModule, NzTabsModule, NzButtonModule, NzTagModule, NzAlertModule, NzSelectModule, TranslocoPipe, DatePipe, DecimalPipe,
    TitleBar, ActionBar, HistoryTab, WeightsTab, AllergiesTab],
  templateUrl: './patient-detail.html',
})
export class PatientDetail {
  private readonly api = inject(PatientsApi);
  private readonly clientsApi = inject(ClientsApi);
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly t = inject(TranslocoService);
  readonly auth = inject(AuthService);
  readonly locale = inject(LocaleService);
  readonly viewport = inject(ViewportService);

  readonly clientId = signal(this.route.snapshot.paramMap.get('id')!);
  readonly id = signal(this.route.snapshot.paramMap.get('pid')!);
  readonly detail = signal<PatientDetailDto | null>(null);
  readonly owner = signal<ClientDto | null>(null);
  readonly error = signal<string | null>(null);
  readonly patient = computed(() => this.detail()?.patient ?? null);
  readonly deceased = computed(() => !!this.patient()?.deceasedAt);
  readonly activeAllergies = computed(() => this.detail()?.activeAllergies ?? []);
  /** "Cat · female spayed · 3 y · 4.20 kg" */
  readonly identityLine = computed(() => {
    const p = this.patient();
    if (!p) return '';
    const parts = [this.t.translate('species.' + p.species)];
    if (p.sex !== 'unknown') parts.push(this.t.translate('sex.' + p.sex));
    const age = ageLabel(this.t, p);
    if (age) parts.push(age);
    return parts.join(' · ');
  });

  // transfer: a searchable client picker that opens under the owner row
  readonly transferOpen = signal(false);
  readonly candidates = signal<ClientDto[]>([]);
  readonly searching = signal(false);
  targetClientId: string | null = null;
  private readonly search$ = new Subject<string>();

  constructor() {
    this.route.paramMap.subscribe((p) => {
      this.clientId.set(p.get('id')!);
      this.id.set(p.get('pid')!);
      this.reload();
    });
    this.search$.pipe(
      debounceTime(300), distinctUntilChanged(),
      switchMap((q) => { this.searching.set(true); return this.clientsApi.listClients(q || undefined, false, 0, 10); }),
      takeUntilDestroyed(),
    ).subscribe({
      next: (page) => { this.candidates.set((page.content ?? []).filter((c) => c.id !== this.clientId())); this.searching.set(false); },
      error: () => this.searching.set(false),
    });
  }

  reload(): void {
    this.api.getPatient(this.id()).subscribe({
      next: (d) => {
        this.detail.set(d);
        // the route's client is the owner the user came from; the record's owner wins after a transfer
        if (d.patient.clientId !== this.clientId()) {
          this.router.navigate(['/clients', d.patient.clientId, 'patients', d.patient.id], { replaceUrl: true });
          return;
        }
        this.clientsApi.getClient(d.patient.clientId).subscribe((c) => this.owner.set(c));
      },
      error: (e) => this.error.set(apiMessage(e, this.t.translate('patients.loadFailed'))),
    });
  }

  markDeceased(): void {
    const p = this.patient();
    if (!p) return;
    const date = prompt(this.t.translate('patients.promptDeceased', { name: p.name }), isoDate());
    if (date === null) return;
    if (!/^\d{4}-\d{2}-\d{2}$/.test(date.trim())) { alert(this.t.translate('patients.invalidDate')); return; }
    this.api.markDeceased(p.id, { date: date.trim() }).subscribe({
      next: () => this.reload(),
      error: (e) => this.error.set(apiMessage(e, this.t.translate('shared.saveFailed'))),
    });
  }

  searchClients(q: string): void {
    this.search$.next(q.trim());
  }

  transfer(): void {
    const p = this.patient();
    const target = this.candidates().find((c) => c.id === this.targetClientId);
    if (!p || !target) return;
    if (!confirm(this.t.translate('patients.confirmTransfer', { name: p.name, client: target.fullName }))) return;
    this.api.transferPatient(p.id, { clientId: target.id }).subscribe({
      next: (moved) => {
        this.transferOpen.set(false);
        this.targetClientId = null;
        this.router.navigate(['/clients', moved.clientId, 'patients', moved.id], { replaceUrl: true });
      },
      error: (e) => this.error.set(apiMessage(e, this.t.translate('shared.saveFailed'))),
    });
  }
}
