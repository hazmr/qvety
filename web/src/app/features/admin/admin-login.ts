import { Component, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router } from '@angular/router';
import { TranslocoPipe, TranslocoService } from '@jsverse/transloco';
import { NzAlertModule } from 'ng-zorro-antd/alert';
import { NzButtonModule } from 'ng-zorro-antd/button';
import { NzFormModule } from 'ng-zorro-antd/form';
import { NzInputModule } from 'ng-zorro-antd/input';
import { apiMessage } from '../../core/api-error';
import { PlatformAuthService } from '../../core/platform-auth.service';

/** Qvety staff sign-in. Deliberately plain: this is not a clinic screen and no clinic sees it. */
@Component({
  selector: 'app-admin-login',
  imports: [ReactiveFormsModule, NzFormModule, NzInputModule, NzButtonModule, NzAlertModule, TranslocoPipe],
  template: `
    <div class="admin-login">
      <form nz-form nzLayout="vertical" [formGroup]="form" class="form-card" (ngSubmit)="submit()">
        <h1 class="admin-login__title">{{ 'admin.title' | transloco }}</h1>
        @if (error(); as e) { <nz-alert nzType="error" [nzMessage]="e" nzShowIcon style="margin-bottom: 16px" /> }
        <nz-form-item>
          <nz-form-label nzRequired nzFor="email">{{ 'admin.email' | transloco }}</nz-form-label>
          <nz-form-control [nzErrorTip]="'shared.required' | transloco">
            <input nz-input id="email" type="email" formControlName="email" dir="ltr" class="ltr-field" autocomplete="username" />
          </nz-form-control>
        </nz-form-item>
        <nz-form-item>
          <nz-form-label nzRequired nzFor="password">{{ 'admin.password' | transloco }}</nz-form-label>
          <nz-form-control [nzErrorTip]="'shared.required' | transloco">
            <input nz-input id="password" type="password" formControlName="password" dir="ltr" class="ltr-field" autocomplete="current-password" />
          </nz-form-control>
        </nz-form-item>
        <button nz-button nzType="primary" nzBlock type="submit" [nzLoading]="busy()">{{ 'admin.signIn' | transloco }}</button>
      </form>
    </div>
  `,
  styles: [`
    .admin-login { display: flex; justify-content: center; padding: 48px 16px; }
    .admin-login .form-card { width: 100%; max-width: 380px; }
    .admin-login__title { margin: 0 0 20px; font-size: 20px; font-weight: 600; }
  `],
})
export class AdminLogin {
  private readonly fb = inject(FormBuilder);
  private readonly platform = inject(PlatformAuthService);
  private readonly router = inject(Router);
  private readonly t = inject(TranslocoService);

  readonly busy = signal(false);
  readonly error = signal<string | null>(null);

  readonly form = this.fb.nonNullable.group({
    email: ['', [Validators.required, Validators.email]],
    password: ['', [Validators.required]],
  });

  submit(): void {
    if (this.form.invalid) { this.form.markAllAsTouched(); return; }
    this.busy.set(true);
    this.error.set(null);
    const v = this.form.getRawValue();
    this.platform.login(v.email, v.password).subscribe({
      next: () => this.router.navigateByUrl('/admin/practices'),
      error: (e) => {
        this.busy.set(false);
        this.error.set(apiMessage(e, this.t.translate('admin.loginFailed')));
      },
    });
  }
}
