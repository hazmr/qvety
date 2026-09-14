import { Component, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { TranslocoPipe, TranslocoService } from '@jsverse/transloco';
import { NzAlertModule } from 'ng-zorro-antd/alert';
import { NzButtonModule } from 'ng-zorro-antd/button';
import { NzFormModule } from 'ng-zorro-antd/form';
import { NzInputModule } from 'ng-zorro-antd/input';
import { AuthService } from '../../core/auth.service';
import { LocaleService } from '../../core/locale.service';
import { apiMessage } from '../../core/api-error';

@Component({
  selector: 'app-login',
  imports: [ReactiveFormsModule, NzFormModule, NzInputModule, NzButtonModule, NzAlertModule, TranslocoPipe],
  templateUrl: './login.html',
  styleUrl: './login.scss',
})
export class Login {
  private readonly fb = inject(FormBuilder);
  private readonly auth = inject(AuthService);
  private readonly router = inject(Router);
  private readonly route = inject(ActivatedRoute);
  private readonly t = inject(TranslocoService);
  readonly locale = inject(LocaleService);

  readonly form = this.fb.nonNullable.group({
    identifier: ['', [Validators.required]],
    password: ['', [Validators.required]],
  });
  readonly error = signal<string | null>(null);
  readonly busy = signal(false);

  toggleLanguage(): void {
    this.locale.apply(this.locale.other());   // before login: device preference only
  }

  submit(): void {
    if (this.form.invalid) { this.form.markAllAsTouched(); return; }
    this.busy.set(true);
    this.error.set(null);
    const { identifier, password } = this.form.getRawValue();
    this.auth.login(identifier, password).subscribe({
      next: (r) => {
        this.busy.set(false);
        const next = this.route.snapshot.queryParamMap.get('next');
        this.router.navigateByUrl(r.user.mustChangePassword ? '/change-password' : (next ?? '/'));
      },
      error: (e) => {
        this.busy.set(false);
        const key = e?.status === 429 ? 'login.errorTooMany' : e?.status === 401 ? 'login.errorCredentials' : 'login.errorGeneric';
        this.error.set(apiMessage(e, this.t.translate(key)));
      },
    });
  }
}
