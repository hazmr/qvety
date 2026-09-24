import { Component, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router } from '@angular/router';
import { TranslocoPipe, TranslocoService } from '@jsverse/transloco';
import { NzAlertModule } from 'ng-zorro-antd/alert';
import { NzButtonModule } from 'ng-zorro-antd/button';
import { NzCheckboxModule } from 'ng-zorro-antd/checkbox';
import { NzFormModule } from 'ng-zorro-antd/form';
import { NzInputModule } from 'ng-zorro-antd/input';
import { PlatformApi, PracticeCreatedDto } from '../../api';
import { apiMessage, applyFieldErrors } from '../../core/api-error';
import { ActionBar } from '../../layout/action-bar/action-bar';
import { TitleBar } from '../../layout/title-bar/title-bar';

/**
 * Opening a clinic. On success the temporary password is shown once and the form is replaced by the
 * handover panel, because closing this screen is the only chance to read it.
 */
@Component({
  selector: 'app-practice-create',
  imports: [ReactiveFormsModule, NzFormModule, NzInputModule, NzButtonModule, NzAlertModule,
    NzCheckboxModule, TranslocoPipe, TitleBar, ActionBar],
  templateUrl: './practice-create.html',
})
export class PracticeCreate {
  private readonly fb = inject(FormBuilder);
  private readonly api = inject(PlatformApi);
  private readonly router = inject(Router);
  private readonly t = inject(TranslocoService);

  readonly busy = signal(false);
  readonly error = signal<string | null>(null);
  readonly created = signal<PracticeCreatedDto | null>(null);

  readonly form = this.fb.nonNullable.group({
    name: ['', [Validators.required]],
    country: ['EG', [Validators.required, Validators.pattern(/^[A-Za-z]{2}$/)]],
    currency: ['EGP', [Validators.required, Validators.pattern(/^[A-Za-z]{3}$/)]],
    locale: ['ar-EG', [Validators.required]],
    timezone: ['Africa/Cairo', [Validators.required]],
    adminName: ['', [Validators.required]],
    adminPhone: ['', [Validators.required]],
    adminEmail: [''],
    adminVeterinarian: [true],
  });

  submit(): void {
    if (this.form.invalid) { this.form.markAllAsTouched(); return; }
    this.busy.set(true);
    this.error.set(null);
    const v = this.form.getRawValue();
    this.api.createPractice({
      name: v.name, country: v.country, currency: v.currency, locale: v.locale, timezone: v.timezone,
      adminName: v.adminName, adminPhone: v.adminPhone,
      adminEmail: v.adminEmail || undefined, adminVeterinarian: v.adminVeterinarian,
    }).subscribe({
      next: (r) => { this.busy.set(false); this.created.set(r); },
      error: (e) => {
        this.busy.set(false);
        if (applyFieldErrors(e, this.form)) { this.error.set(null); return; }
        this.error.set(apiMessage(e, this.t.translate('shared.saveFailed')));
      },
    });
  }

  done(): void {
    this.router.navigate(['/admin/practices', this.created()!.practice.id]);
  }
}
