import { Component, effect, inject, input, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { TranslocoPipe, TranslocoService } from '@jsverse/transloco';
import { NzButtonModule } from 'ng-zorro-antd/button';
import { NzTableModule } from 'ng-zorro-antd/table';
import { PatientDto, PatientsApi } from '../../api';
import { ViewportService } from '../../core/viewport.service';
import { ageLabel } from './patient-labels';

/** The client's patients, deceased included and tagged. Phone: stacked rows; desktop: a small table. */
@Component({
  selector: 'app-patients-tab',
  imports: [RouterLink, NzButtonModule, NzTableModule, TranslocoPipe],
  templateUrl: './patients-tab.html',
})
export class PatientsTab {
  private readonly api = inject(PatientsApi);
  private readonly t = inject(TranslocoService);
  readonly viewport = inject(ViewportService);

  readonly clientId = input.required<string>();
  readonly canAdd = input(false);
  readonly patients = signal<PatientDto[]>([]);
  readonly loading = signal(true);

  constructor() {
    effect(() => {
      this.loading.set(true);
      this.api.listPatients(this.clientId(), undefined, undefined, undefined, 0, 100, ['name,asc']).subscribe({
        next: (p) => { this.patients.set(p.content ?? []); this.loading.set(false); },
        error: () => this.loading.set(false),
      });
    });
  }

  /** "Cat · female spayed · 3 y", skipping what is unknown. */
  identity(p: PatientDto): string {
    const parts = [this.t.translate('species.' + p.species)];
    if (p.sex !== 'unknown') parts.push(this.t.translate('sex.' + p.sex));
    const age = ageLabel(this.t, p);
    if (age) parts.push(age);
    return parts.join(' · ');
  }

  age(p: PatientDto): string {
    return ageLabel(this.t, p);
  }

  link(p: PatientDto): unknown[] {
    return ['/clients', this.clientId(), 'patients', p.id];
  }
}
