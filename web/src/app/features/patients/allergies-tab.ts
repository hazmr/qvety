import { DatePipe } from '@angular/common';
import { Component, effect, inject, input, output, signal } from '@angular/core';
import { FormControl, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { TranslocoPipe, TranslocoService } from '@jsverse/transloco';
import { NzAlertModule } from 'ng-zorro-antd/alert';
import { NzButtonModule } from 'ng-zorro-antd/button';
import { NzFormModule } from 'ng-zorro-antd/form';
import { NzInputModule } from 'ng-zorro-antd/input';
import { NzSelectModule } from 'ng-zorro-antd/select';
import { NzTagModule } from 'ng-zorro-antd/tag';
import { AllergyRequestDto, PatientAllergyDto, PatientsApi } from '../../api';
import { apiMessage, applyFieldErrors } from '../../core/api-error';
import { AuthService } from '../../core/auth.service';
import { LocaleService } from '../../core/locale.service';
import { SEVERITIES } from './patient-labels';

/**
 * Allergies, retracted ones included and struck through. Clinical staff add and retract; nothing is edited or
 * deleted, here or in the database.
 */
@Component({
  selector: 'app-allergies-tab',
  imports: [ReactiveFormsModule, NzFormModule, NzInputModule, NzSelectModule, NzButtonModule, NzAlertModule, NzTagModule, TranslocoPipe, DatePipe],
  templateUrl: './allergies-tab.html',
})
export class AllergiesTab {
  private readonly api = inject(PatientsApi);
  private readonly t = inject(TranslocoService);
  readonly auth = inject(AuthService);
  readonly locale = inject(LocaleService);
  readonly severities = SEVERITIES;

  readonly patientId = input.required<string>();
  readonly changed = output<void>();
  readonly allergies = signal<PatientAllergyDto[]>([]);
  readonly loading = signal(true);
  readonly busy = signal(false);
  readonly error = signal<string | null>(null);

  readonly form = new FormGroup({
    substance: new FormControl('', { nonNullable: true, validators: [Validators.required, Validators.maxLength(200)] }),
    reaction: new FormControl('', { nonNullable: true, validators: [Validators.maxLength(1000)] }),
    severity: new FormControl('', { nonNullable: true, validators: [Validators.required] }),
  });

  constructor() {
    effect(() => this.load(this.patientId()));
  }

  load(id: string): void {
    this.loading.set(true);
    this.api.listAllergies(id).subscribe({
      next: (a) => { this.allergies.set(a); this.loading.set(false); },
      error: () => this.loading.set(false),
    });
  }

  add(): void {
    if (this.form.invalid) { this.form.markAllAsTouched(); return; }
    const v = this.form.getRawValue();
    this.busy.set(true);
    this.error.set(null);
    const body: AllergyRequestDto = { substance: v.substance.trim(), reaction: v.reaction.trim() || undefined, severity: v.severity as AllergyRequestDto.SeverityEnum };
    this.api.addAllergy(this.patientId(), body).subscribe({
      next: () => { this.busy.set(false); this.form.reset(); this.load(this.patientId()); this.changed.emit(); },
      error: (e) => {
        this.busy.set(false);
        if (!applyFieldErrors(e, this.form)) this.error.set(apiMessage(e, this.t.translate('shared.saveFailed')));
      },
    });
  }

  retract(a: PatientAllergyDto): void {
    const reason = prompt(this.t.translate('patients.promptRetract', { substance: a.substance }));
    if (!reason || !reason.trim()) return;
    this.api.retractAllergy(this.patientId(), a.id, { reason: reason.trim() }).subscribe({
      next: () => { this.load(this.patientId()); this.changed.emit(); },
      error: (e) => this.error.set(apiMessage(e, this.t.translate('shared.saveFailed'))),
    });
  }

  errorTip(name: 'substance' | 'reaction' | 'severity'): string {
    const errors = this.form.controls[name].errors;
    if (!errors) return '';
    if (errors['server']) return errors['server'];
    if (errors['required']) return this.t.translate('shared.required');
    if (errors['maxlength']) return this.t.translate('shared.tooLong');
    return this.t.translate('shared.invalid');
  }
}
