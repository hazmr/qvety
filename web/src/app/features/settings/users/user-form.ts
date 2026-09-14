import { Component, computed, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { TranslocoPipe, TranslocoService } from '@jsverse/transloco';
import { NzAlertModule } from 'ng-zorro-antd/alert';
import { NzButtonModule } from 'ng-zorro-antd/button';
import { NzFormModule } from 'ng-zorro-antd/form';
import { NzInputModule } from 'ng-zorro-antd/input';
import { NzSelectModule } from 'ng-zorro-antd/select';
import { NzSwitchModule } from 'ng-zorro-antd/switch';
import { UserDto, UsersApi } from '../../../api';
import { AuthService } from '../../../core/auth.service';
import { TitleBar } from '../../../layout/title-bar/title-bar';
import { ActionBar } from '../../../layout/action-bar/action-bar';
import { apiMessage, applyFieldErrors } from '../../../core/api-error';

const ROLES: UserDto.RoleEnum[] = Object.values(UserDto.RoleEnum);

/** Create (with temporary password) or edit. The role/flag rule mirrors the database check. */
@Component({
  selector: 'app-user-form',
  imports: [ReactiveFormsModule, NzFormModule, NzInputModule, NzSelectModule, NzSwitchModule, NzButtonModule, NzAlertModule, TranslocoPipe, TitleBar, ActionBar],
  templateUrl: './user-form.html',
})
export class UserForm {
  private readonly fb = inject(FormBuilder);
  private readonly api = inject(UsersApi);
  private readonly router = inject(Router);
  private readonly route = inject(ActivatedRoute);
  private readonly t = inject(TranslocoService);
  private readonly auth = inject(AuthService);

  readonly roles = ROLES;
  readonly id = this.route.snapshot.paramMap.get('id');
  readonly isNew = this.id === null;
  readonly error = signal<string | null>(null);
  readonly busy = signal(false);
  readonly current = signal<UserDto | null>(null);

  readonly form = this.fb.nonNullable.group({
    phone: ['', [Validators.required]],
    email: ['', [Validators.email]],
    temporaryPassword: ['', this.isNew ? [Validators.required, Validators.minLength(10)] : []],
    fullName: ['', [Validators.required]],
    role: this.fb.nonNullable.control<UserDto.RoleEnum>(UserDto.RoleEnum.FrontDesk, [Validators.required]),
    veterinarian: [false],
    licenseNumber: [''],
  });

  constructor() {
    if (this.id) {
      this.api.getUser(this.id).subscribe((u) => {
        this.current.set(u);
        this.form.patchValue({
          phone: u.phone, email: u.email ?? '', fullName: u.fullName, role: u.role, veterinarian: u.veterinarian,
          licenseNumber: u.licenseNumber ?? '',
        });
      });
    }
    // veterinarian role always has the flag; technician and front desk never do
    this.form.controls.role.valueChanges.subscribe((role) => {
      const vet = this.form.controls.veterinarian;
      if (role === 'veterinarian') { vet.setValue(true); vet.disable(); }
      else if (role === 'admin') { vet.enable(); }
      else { vet.setValue(false); vet.disable(); }
    });
  }

  /** Own account: password changes go through /change-password; deactivation needs another admin. */
  readonly isSelf = computed(() => !!this.current() && this.current()!.id === this.auth.user()?.id);

  deactivate(): void {
    const u = this.current();
    if (!u || !confirm(this.t.translate('users.confirmDeactivate', { name: u.fullName }))) return;
    this.api.deactivateUser(u.id).subscribe(() => this.router.navigateByUrl('/settings/users'));
  }

  activate(): void {
    const u = this.current();
    if (!u) return;
    const temporary = prompt(this.t.translate('users.promptActivate', { name: u.fullName }));
    if (!temporary) return;
    this.api.activateUser(u.id, { temporaryPassword: temporary }).subscribe((updated) => this.current.set(updated));
  }

  resetPassword(): void {
    const u = this.current();
    if (!u) return;
    const temporary = prompt(this.t.translate('users.promptTemporary', { name: u.fullName }));
    if (!temporary) return;
    this.api.resetUserPassword(u.id, { temporaryPassword: temporary }).subscribe((updated) => this.current.set(updated));
  }

  submit(): void {
    if (this.form.invalid) { this.form.markAllAsTouched(); return; }
    this.busy.set(true);
    this.error.set(null);
    const v = this.form.getRawValue();
    const done = () => this.router.navigateByUrl('/settings/users');
    const fail = (e: { status?: number }) => {
      this.busy.set(false);
      // backend text is already in the user's language; field messages go on the controls
      if (applyFieldErrors(e, this.form)) { this.error.set(null); return; }
      const fallback = this.t.translate(e?.status === 409 ? 'users.errorConflict' : e?.status === 400 ? 'users.errorPhone' : 'users.errorGeneric');
      this.error.set(apiMessage(e, fallback));
    };
    if (this.isNew) {
      this.api.createUser({
        phone: v.phone, email: v.email || undefined, temporaryPassword: v.temporaryPassword, fullName: v.fullName, role: v.role,
        veterinarian: v.veterinarian, licenseNumber: v.licenseNumber || undefined,
      }).subscribe({ next: done, error: fail });
    } else {
      this.api.updateUser(this.id!, {
        phone: v.phone, email: v.email || undefined, fullName: v.fullName, role: v.role, veterinarian: v.veterinarian,
        licenseNumber: v.licenseNumber || undefined,
      }).subscribe({ next: done, error: fail });
    }
  }
}
