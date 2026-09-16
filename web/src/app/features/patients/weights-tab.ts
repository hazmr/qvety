import { DatePipe, DecimalPipe } from '@angular/common';
import { Component, effect, inject, input, output, signal } from '@angular/core';
import { FormControl, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { TranslocoPipe, TranslocoService } from '@jsverse/transloco';
import { NzAlertModule } from 'ng-zorro-antd/alert';
import { NzButtonModule } from 'ng-zorro-antd/button';
import { NzFormModule } from 'ng-zorro-antd/form';
import { NzInputModule } from 'ng-zorro-antd/input';
import { PatientWeightDto, PatientsApi } from '../../api';
import { apiMessage, applyFieldErrors } from '../../core/api-error';
import { LocaleService } from '../../core/locale.service';
import { isoLocalDateTime } from './patient-labels';

/** Dated weights, newest first, and a two-field form to add one. A wrong row is voided with a reason, never edited. */
@Component({
  selector: 'app-weights-tab',
  imports: [ReactiveFormsModule, NzFormModule, NzInputModule, NzButtonModule, NzAlertModule, TranslocoPipe, DatePipe, DecimalPipe],
  templateUrl: './weights-tab.html',
})
export class WeightsTab {
  private readonly api = inject(PatientsApi);
  private readonly t = inject(TranslocoService);
  readonly locale = inject(LocaleService);

  readonly patientId = input.required<string>();
  readonly added = output<PatientWeightDto>();
  readonly weights = signal<PatientWeightDto[]>([]);
  readonly loading = signal(true);
  readonly busy = signal(false);
  readonly error = signal<string | null>(null);

  readonly form = new FormGroup({
    measuredAt: new FormControl(isoLocalDateTime(), { nonNullable: true, validators: [Validators.required] }),
    weightKg: new FormControl('', { nonNullable: true, validators: [Validators.required, Validators.min(0.01), Validators.max(9999.99)] }),
  });

  constructor() {
    effect(() => this.load(this.patientId()));
  }

  load(id: string): void {
    this.loading.set(true);
    this.api.listWeights(id).subscribe({
      next: (w) => { this.weights.set(w); this.loading.set(false); },
      error: () => this.loading.set(false),
    });
  }

  add(): void {
    if (this.form.invalid) { this.form.markAllAsTouched(); return; }
    const v = this.form.getRawValue();
    this.busy.set(true);
    this.error.set(null);
    this.api.addWeight(this.patientId(), { measuredAt: new Date(v.measuredAt).toISOString(), weightKg: Number(v.weightKg) }).subscribe({
      next: (w) => {
        this.busy.set(false);
        this.form.reset({ measuredAt: isoLocalDateTime(), weightKg: '' });
        this.load(this.patientId());
        this.added.emit(w);
      },
      error: (e) => {
        this.busy.set(false);
        if (!applyFieldErrors(e, this.form)) this.error.set(apiMessage(e, this.t.translate('shared.saveFailed')));
      },
    });
  }

  voidWeight(w: PatientWeightDto): void {
    const reason = prompt(this.t.translate('patients.promptVoid', { weight: w.weightKg }));
    if (!reason || !reason.trim()) return;
    this.api.voidWeight(this.patientId(), w.id, { reason: reason.trim() }).subscribe({
      next: (voided) => { this.load(this.patientId()); this.added.emit(voided); },
      error: (e) => this.error.set(apiMessage(e, this.t.translate('shared.saveFailed'))),
    });
  }

  errorTip(name: 'measuredAt' | 'weightKg'): string {
    const errors = this.form.controls[name].errors;
    if (!errors) return '';
    if (errors['server']) return errors['server'];
    if (errors['required']) return this.t.translate('shared.required');
    return this.t.translate('shared.invalid');
  }
}
