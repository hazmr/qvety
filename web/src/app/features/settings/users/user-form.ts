import { Component, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { NzAlertModule } from 'ng-zorro-antd/alert';
import { NzButtonModule } from 'ng-zorro-antd/button';
import { NzFormModule } from 'ng-zorro-antd/form';
import { NzInputModule } from 'ng-zorro-antd/input';
import { NzSelectModule } from 'ng-zorro-antd/select';
import { NzSwitchModule } from 'ng-zorro-antd/switch';
import { UserDto, UsersApi } from '../../../api';

const ROLES: UserDto.RoleEnum[] = Object.values(UserDto.RoleEnum);

/** Create (with temporary password) or edit. The role/flag rule mirrors the database check. */
@Component({
  selector: 'app-user-form',
  imports: [ReactiveFormsModule, RouterLink, NzFormModule, NzInputModule, NzSelectModule, NzSwitchModule, NzButtonModule, NzAlertModule],
  templateUrl: './user-form.html',
})
export class UserForm {
  private readonly fb = inject(FormBuilder);
  private readonly api = inject(UsersApi);
  private readonly router = inject(Router);
  private readonly route = inject(ActivatedRoute);

  readonly roles = ROLES;
  readonly id = this.route.snapshot.paramMap.get('id');
  readonly isNew = this.id === null;
  readonly error = signal<string | null>(null);
  readonly busy = signal(false);

  readonly form = this.fb.nonNullable.group({
    email: ['', [Validators.required, Validators.email]],
    temporaryPassword: ['', this.isNew ? [Validators.required, Validators.minLength(10)] : []],
    fullName: ['', [Validators.required]],
    role: this.fb.nonNullable.control<UserDto.RoleEnum>(UserDto.RoleEnum.FrontDesk, [Validators.required]),
    veterinarian: [false],
    licenseNumber: [''],
    phone: [''],
  });

  constructor() {
    if (this.id) {
      this.api.get(this.id).subscribe((u) => {
        this.form.patchValue({
          email: u.email, fullName: u.fullName, role: u.role, veterinarian: u.veterinarian,
          licenseNumber: u.licenseNumber ?? '', phone: u.phone ?? '',
        });
        this.form.controls.email.disable();
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

  submit(): void {
    if (this.form.invalid) { this.form.markAllAsTouched(); return; }
    this.busy.set(true);
    this.error.set(null);
    const v = this.form.getRawValue();
    const done = () => this.router.navigateByUrl('/settings/users');
    const fail = (e: { status?: number }) => {
      this.busy.set(false);
      this.error.set(e?.status === 409 ? 'This email already exists in the practice.' : 'Could not save.');
    };
    if (this.isNew) {
      this.api.create({
        email: v.email, temporaryPassword: v.temporaryPassword, fullName: v.fullName, role: v.role,
        veterinarian: v.veterinarian, licenseNumber: v.licenseNumber || undefined, phone: v.phone || undefined,
      }).subscribe({ next: done, error: fail });
    } else {
      this.api.update(this.id!, {
        fullName: v.fullName, role: v.role, veterinarian: v.veterinarian,
        licenseNumber: v.licenseNumber || undefined, phone: v.phone || undefined,
      }).subscribe({ next: done, error: fail });
    }
  }
}
