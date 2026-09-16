import { Component, computed, effect, inject, input, output, signal } from '@angular/core';
import { FormControl, FormGroup, ReactiveFormsModule, ValidatorFn, Validators } from '@angular/forms';
import { TranslocoPipe, TranslocoService } from '@jsverse/transloco';
import { NzAlertModule } from 'ng-zorro-antd/alert';
import { NzButtonModule } from 'ng-zorro-antd/button';
import { NzFormModule } from 'ng-zorro-antd/form';
import { NzInputModule } from 'ng-zorro-antd/input';
import { NzSelectModule } from 'ng-zorro-antd/select';
import { apiMessage, applyFieldErrors } from '../../core/api-error';
import { ActionBar } from '../../layout/action-bar/action-bar';

export type FieldType = 'text' | 'tel' | 'email' | 'date' | 'number' | 'textarea' | 'select';

export interface FormField {
  name: string;
  labelKey: string;
  type?: FieldType;
  required?: boolean;
  ltr?: boolean;
  hintKey?: string;
  maxLength?: number;
  options?: { value: string; labelKey: string }[];
}

/**
 * Config-driven form: fields in, values out. Server field errors land on the controls, and the
 * localized backend message shows above the form. Features keep their own submit logic.
 */
@Component({
  selector: 'app-form-page',
  imports: [ReactiveFormsModule, NzFormModule, NzInputModule, NzSelectModule, NzButtonModule, NzAlertModule, TranslocoPipe, ActionBar],
  templateUrl: './form-page.html',
})
export class FormPage {
  private readonly t = inject(TranslocoService);

  readonly fields = input.required<FormField[]>();
  readonly value = input<object | null>(null);
  readonly busy = input(false);
  readonly submitKey = input('shared.save');
  readonly submitted = output<Record<string, string>>();

  readonly error = signal<string | null>(null);
  readonly form = computed(() => {
    const group: Record<string, FormControl<string>> = {};
    for (const f of this.fields()) {
      const validators: ValidatorFn[] = [];
      if (f.required) validators.push(Validators.required);
      if (f.type === 'email') validators.push(Validators.email);
      if (f.maxLength) validators.push(Validators.maxLength(f.maxLength));
      group[f.name] = new FormControl('', { nonNullable: true, validators });
    }
    return new FormGroup(group);
  });

  constructor() {
    effect(() => {
      const v = this.value();
      if (v) {
        const patch: Record<string, string> = {};
        const record = v as Record<string, unknown>;
        for (const f of this.fields()) patch[f.name] = (record[f.name] as string | null | undefined) ?? '';
        this.form().patchValue(patch);
      }
    });
  }

  submit(): void {
    const form = this.form();
    if (form.invalid) { form.markAllAsTouched(); return; }
    this.error.set(null);
    this.submitted.emit(form.getRawValue());
  }

  /** Called by the feature when the save fails: field messages on controls, or one message above. */
  fail(e: unknown): void {
    if (applyFieldErrors(e, this.form())) { this.error.set(null); return; }
    this.error.set(apiMessage(e, this.t.translate('shared.saveFailed')));
  }

  errorTip(name: string): string {
    const errors = this.form().controls[name]?.errors;
    if (!errors) return '';
    if (errors['server']) return errors['server'];
    if (errors['required']) return this.t.translate('shared.required');
    if (errors['email']) return this.t.translate('shared.invalidEmail');
    if (errors['maxlength']) return this.t.translate('shared.tooLong');
    return this.t.translate('shared.invalid');
  }
}
