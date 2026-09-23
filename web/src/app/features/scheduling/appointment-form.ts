import { Component, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { TranslocoPipe, TranslocoService } from '@jsverse/transloco';
import { NzAlertModule } from 'ng-zorro-antd/alert';
import { NzButtonModule } from 'ng-zorro-antd/button';
import { NzDatePickerModule } from 'ng-zorro-antd/date-picker';
import { NzFormModule } from 'ng-zorro-antd/form';
import { NzInputModule } from 'ng-zorro-antd/input';
import { NzSelectModule } from 'ng-zorro-antd/select';
import { forkJoin } from 'rxjs';
import {
  AppointmentDto, AppointmentTypeDto, AppointmentsApi, ClientDto, ClientsApi, PatientDto, PatientsApi,
  ReferenceApi, RoomDto, UsersApi, VeterinarianDto,
} from '../../api';
import { apiMessage, applyFieldErrors } from '../../core/api-error';
import { ActionBar } from '../../layout/action-bar/action-bar';
import { TitleBar } from '../../layout/title-bar/title-bar';

/**
 * Book or reschedule. The patient list drives the client: an appointment always names the owner of
 * record, and the server takes it from the patient rather than trusting the form. Picking a type fills
 * in its default length, which the desk can still change.
 */
@Component({
  selector: 'app-appointment-form',
  imports: [ReactiveFormsModule, NzFormModule, NzInputModule, NzSelectModule, NzDatePickerModule,
    NzButtonModule, NzAlertModule, TranslocoPipe, TitleBar, ActionBar],
  templateUrl: './appointment-form.html',
})
export class AppointmentForm {
  private readonly fb = inject(FormBuilder);
  private readonly api = inject(AppointmentsApi);
  private readonly reference = inject(ReferenceApi);
  private readonly patientsApi = inject(PatientsApi);
  private readonly clientsApi = inject(ClientsApi);
  private readonly usersApi = inject(UsersApi);
  private readonly router = inject(Router);
  private readonly route = inject(ActivatedRoute);
  private readonly t = inject(TranslocoService);

  readonly id = this.route.snapshot.paramMap.get('id');
  readonly isNew = this.id === null;
  readonly busy = signal(false);
  readonly error = signal<string | null>(null);
  readonly warnings = signal<string[]>([]);
  readonly current = signal<AppointmentDto | null>(null);

  readonly patients = signal<PatientDto[]>([]);
  private readonly clientNames = signal<Map<string, string>>(new Map());
  readonly vets = signal<VeterinarianDto[]>([]);
  readonly rooms = signal<RoomDto[]>([]);
  readonly types = signal<AppointmentTypeDto[]>([]);

  readonly form = this.fb.nonNullable.group({
    patientId: ['', [Validators.required]],
    veterinarianId: ['', [Validators.required]],
    roomId: ['', [Validators.required]],
    appointmentTypeId: ['', [Validators.required]],
    start: this.fb.nonNullable.control<Date | null>(null, [Validators.required]),
    durationMinutes: [20, [Validators.required, Validators.min(5), Validators.max(480)]],
    reason: [''],
    notes: [''],
  });

  constructor() {
    forkJoin({
      patients: this.patientsApi.listPatients(undefined, undefined, false, undefined, 0, 100),
      clients: this.clientsApi.listClients(undefined, undefined, 0, 100),
      // not listUsers: the staff record is admin-only and booking is front-desk work
      vets: this.usersApi.listVeterinarians(),
      rooms: this.reference.listRooms(false),
      types: this.reference.listAppointmentTypes(false),
    }).subscribe(({ patients, clients, vets, rooms, types }) => {
      this.patients.set(patients.content ?? []);
      this.clientNames.set(new Map((clients.content ?? []).map((c: ClientDto) => [c.id, c.fullName])));
      this.vets.set(vets);
      this.rooms.set(rooms);
      this.types.set(types);
    });

    if (this.id) {
      this.api.getAppointment(this.id).subscribe((a) => {
        this.current.set(a);
        this.form.patchValue({
          patientId: a.patientId, veterinarianId: a.veterinarianId, roomId: a.roomId,
          appointmentTypeId: a.appointmentTypeId, start: new Date(a.startsAt),
          durationMinutes: Math.round((new Date(a.endsAt).getTime() - new Date(a.startsAt).getTime()) / 60000),
          reason: a.reason ?? '', notes: a.notes ?? '',
        });
      });
    } else {
      const date = this.route.snapshot.queryParamMap.get('date');
      const start = date ? new Date(`${date}T10:00:00`) : new Date();
      start.setSeconds(0, 0);
      this.form.patchValue({ start });
    }

    // A type carries the length the clinic expects; the desk may still override it.
    this.form.controls.appointmentTypeId.valueChanges.subscribe((typeId) => {
      const type = this.types().find((t) => t.id === typeId);
      if (type && this.isNew) {
        this.form.controls.durationMinutes.setValue(type.durationMinutes);
      }
    });
  }

  /** "بسبس · أحمد محمد" — a patient name alone is not enough to pick from at a busy desk. */
  patientLabel(p: PatientDto): string {
    const owner = this.clientNames().get(p.clientId);
    return owner ? `${p.name} · ${owner}` : p.name;
  }

  save(walkIn: boolean): void {
    if (this.form.invalid) { this.form.markAllAsTouched(); return; }
    this.busy.set(true);
    this.error.set(null);
    this.warnings.set([]);
    const v = this.form.getRawValue();
    const body = {
      patientId: v.patientId, veterinarianId: v.veterinarianId, roomId: v.roomId,
      appointmentTypeId: v.appointmentTypeId, start: v.start!.toISOString(),
      durationMinutes: v.durationMinutes, reason: v.reason || undefined, notes: v.notes || undefined,
      walkIn,
    };
    const request = this.isNew
      ? this.api.createAppointment(body)
      : this.api.updateAppointment(this.id!, this.current()!.version, body);
    request.subscribe({
      next: (saved) => {
        this.busy.set(false);
        if (saved.warnings.length) {
          // Saved: the desk is told about the hours, not stopped by them.
          this.warnings.set(saved.warnings);
          this.current.set(saved.appointment);
          return;
        }
        this.router.navigate(['/schedule', saved.appointment.id]);
      },
      error: (e) => {
        this.busy.set(false);
        if (applyFieldErrors(e, this.form)) { this.error.set(null); return; }
        this.error.set(apiMessage(e, this.t.translate('shared.saveFailed')));
      },
    });
  }
}
