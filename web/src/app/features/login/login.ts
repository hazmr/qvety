import { Component, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { TranslocoPipe, TranslocoService } from '@jsverse/transloco';
import { NzAlertModule } from 'ng-zorro-antd/alert';
import { NzButtonModule } from 'ng-zorro-antd/button';
import { NzFormModule } from 'ng-zorro-antd/form';
import { NzIconModule } from 'ng-zorro-antd/icon';
import { NzInputModule } from 'ng-zorro-antd/input';
import { AuthService } from '../../core/auth.service';
import { LocaleService } from '../../core/locale.service';
import { LoginResponseDto, PracticeChoiceDto } from '../../api';
import { apiMessage } from '../../core/api-error';
import { ViewportService } from '../../core/viewport.service';

@Component({
  selector: 'app-login',
  imports: [ReactiveFormsModule, NzFormModule, NzInputModule, NzButtonModule, NzAlertModule, NzIconModule, TranslocoPipe],
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
  readonly viewport = inject(ViewportService);

  readonly form = this.fb.nonNullable.group({
    identifier: ['', [Validators.required]],
    password: ['', [Validators.required]],
  });
  readonly error = signal<string | null>(null);
  readonly busy = signal(false);
  /** Set when the password matched at several practices; the user picks one and we post again. */
  readonly practices = signal<PracticeChoiceDto[] | null>(null);

  toggleLanguage(): void {
    this.locale.apply(this.locale.other());   // before login: device preference only
  }

  submit(): void {
    if (this.form.invalid) { this.form.markAllAsTouched(); return; }
    this.attempt();
  }

  choose(practice: PracticeChoiceDto): void {
    this.attempt(practice.id);
  }

  /** Back to the form: clears the choice and the password so a different person can try. */
  cancelChoice(): void {
    this.practices.set(null);
    this.form.controls.password.reset();
    this.form.controls.identifier.enable();
  }

  private attempt(practiceId?: string): void {
    this.busy.set(true);
    this.error.set(null);
    const { identifier, password } = this.form.getRawValue();
    this.auth.login(identifier, password, practiceId).subscribe({
      next: (r) => this.accept(r),
      error: (e) => {
        this.busy.set(false);
        const key = e?.status === 429 ? 'login.errorTooMany' : e?.status === 401 ? 'login.errorCredentials' : 'login.errorGeneric';
        this.error.set(apiMessage(e, this.t.translate(key)));
      },
    });
  }

  private accept(r: LoginResponseDto): void {
    this.busy.set(false);
    if (r.practices && !r.token) {
      this.practices.set(r.practices);
      this.form.controls.identifier.disable();   // the choice belongs to this identifier
      return;
    }
    const next = this.route.snapshot.queryParamMap.get('next');
    this.router.navigateByUrl(r.user?.mustChangePassword ? '/change-password' : (next ?? '/'));
  }
}
