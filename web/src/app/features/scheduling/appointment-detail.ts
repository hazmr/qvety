import { DatePipe } from '@angular/common';
import { Component, computed, inject, signal } from '@angular/core';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { TranslocoPipe, TranslocoService } from '@jsverse/transloco';
import { NzButtonModule } from 'ng-zorro-antd/button';
import { AppointmentDto, AppointmentsApi } from '../../api';
import { AuthService } from '../../core/auth.service';
import { LocaleService } from '../../core/locale.service';
import { apiMessage } from '../../core/api-error';
import { ActionBar } from '../../layout/action-bar/action-bar';
import { TitleBar } from '../../layout/title-bar/title-bar';

/** What the desk can do next, mirroring the server's transition table. The server still decides. */
const NEXT: Record<string, { status: AppointmentDto.StatusEnum; labelKey: string; danger?: boolean }[]> = {
  scheduled: [
    { status: 'checked_in', labelKey: 'schedule.checkIn' },
    { status: 'no_show', labelKey: 'schedule.noShow', danger: true },
  ],
  checked_in: [{ status: 'in_progress', labelKey: 'schedule.startExam' }],
  in_progress: [{ status: 'completed', labelKey: 'schedule.complete' }],
};

@Component({
  selector: 'app-appointment-detail',
  imports: [TitleBar, ActionBar, RouterLink, NzButtonModule, TranslocoPipe, DatePipe],
  templateUrl: './appointment-detail.html',
})
export class AppointmentDetail {
  private readonly api = inject(AppointmentsApi);
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly t = inject(TranslocoService);
  readonly locale = inject(LocaleService);
  readonly auth = inject(AuthService);

  readonly id = this.route.snapshot.paramMap.get('id')!;
  readonly appointment = signal<AppointmentDto | null>(null);
  readonly error = signal<string | null>(null);

  readonly canWrite = computed(() => this.auth.canWriteClients());
  readonly moves = computed(() => NEXT[this.appointment()?.status ?? ''] ?? []);
  /** Cancelling is possible while the row still holds its slot. */
  readonly canCancel = computed(() => {
    const s = this.appointment()?.status;
    return s === 'scheduled' || s === 'checked_in';
  });

  constructor() {
    this.api.getAppointment(this.id).subscribe((a) => this.appointment.set(a));
  }

  move(status: AppointmentDto.StatusEnum): void {
    this.error.set(null);
    this.api.changeAppointmentStatus(this.id, { status }).subscribe({
      next: (a) => this.appointment.set(a),
      error: (e) => this.error.set(apiMessage(e, this.t.translate('shared.saveFailed'))),
    });
  }

  cancel(): void {
    if (!confirm(this.t.translate('schedule.confirmCancel'))) { return; }
    this.error.set(null);
    this.api.changeAppointmentStatus(this.id, { status: 'cancelled' }).subscribe({
      next: () => this.router.navigateByUrl('/schedule'),
      error: (e) => this.error.set(apiMessage(e, this.t.translate('shared.saveFailed'))),
    });
  }
}
