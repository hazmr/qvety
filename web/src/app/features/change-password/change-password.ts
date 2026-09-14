import { Component, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router } from '@angular/router';
import { NzAlertModule } from 'ng-zorro-antd/alert';
import { NzButtonModule } from 'ng-zorro-antd/button';
import { NzFormModule } from 'ng-zorro-antd/form';
import { NzInputModule } from 'ng-zorro-antd/input';
import { AuthService } from '../../core/auth.service';

/** Forced by the guard while mustChangePassword is set; also reachable by choice later. */
@Component({
  selector: 'app-change-password',
  imports: [ReactiveFormsModule, NzFormModule, NzInputModule, NzButtonModule, NzAlertModule],
  templateUrl: './change-password.html',
  styleUrl: '../login/login.scss',
})
export class ChangePassword {
  private readonly fb = inject(FormBuilder);
  readonly auth = inject(AuthService);
  private readonly router = inject(Router);

  readonly form = this.fb.nonNullable.group({
    currentPassword: ['', [Validators.required]],
    newPassword: ['', [Validators.required, Validators.minLength(10)]],
  });
  readonly error = signal<string | null>(null);
  readonly busy = signal(false);

  submit(): void {
    if (this.form.invalid) { this.form.markAllAsTouched(); return; }
    this.busy.set(true);
    this.error.set(null);
    this.auth.changePassword(this.form.getRawValue()).subscribe({
      next: () => { this.busy.set(false); this.router.navigateByUrl('/'); },
      error: (e) => {
        this.busy.set(false);
        this.error.set(e?.status === 400 ? 'Current password is wrong.' : 'Could not change the password.');
      },
    });
  }
}
