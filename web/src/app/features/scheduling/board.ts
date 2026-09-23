import { Component, computed, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { TranslocoPipe, TranslocoService } from '@jsverse/transloco';
import { NzButtonModule } from 'ng-zorro-antd/button';
import { Subscription, switchMap, timer } from 'rxjs';
import { AppointmentDto, AppointmentsApi, BoardDto, BoardEntryDto, ScheduleApi } from '../../api';
import { AuthService } from '../../core/auth.service';
import { ViewportService } from '../../core/viewport.service';
import { TitleBar } from '../../layout/title-bar/title-bar';

/** How often the board asks again. No websockets in the pilot: one clinic, one screen, nothing to operate. */
const REFRESH_MS = 30_000;

/** The move each column offers, mirroring the server's transition table. */
const NEXT: Record<string, { status: AppointmentDto.StatusEnum; labelKey: string }> = {
  checked_in: { status: 'in_progress', labelKey: 'schedule.startExam' },
  in_progress: { status: 'completed', labelKey: 'schedule.complete' },
};

/**
 * The whiteboard that replaces the paper list at the desk: who is waiting, who is in an exam room, who is
 * done. A view over today's appointments, never a stored table, polled every 30 seconds so a check-in made
 * at the desk shows on the vet's screen without anyone reloading.
 */
@Component({
  selector: 'app-board',
  imports: [TitleBar, NzButtonModule, TranslocoPipe],
  templateUrl: './board.html',
  styleUrl: './board.scss',
})
export class Board {
  private readonly api = inject(ScheduleApi);
  private readonly appointments = inject(AppointmentsApi);
  private readonly t = inject(TranslocoService);
  readonly viewport = inject(ViewportService);
  readonly auth = inject(AuthService);

  readonly board = signal<BoardDto | null>(null);
  readonly canWrite = computed(() => this.auth.canWriteClients());

  readonly columns = computed(() => {
    const b = this.board();
    return [
      { key: 'checked_in', labelKey: 'schedule.columnWaiting', rows: b?.waiting ?? [] },
      { key: 'in_progress', labelKey: 'schedule.columnInExam', rows: b?.inExam ?? [] },
      { key: 'completed', labelKey: 'schedule.columnDone', rows: b?.done ?? [] },
    ];
  });

  constructor() {
    timer(0, REFRESH_MS).pipe(switchMap(() => this.api.getBoard()), takeUntilDestroyed())
      .subscribe({ next: (b) => this.board.set(b), error: () => { /* the next tick tries again */ } });
  }

  nextMove(key: string): { status: AppointmentDto.StatusEnum; labelKey: string } | null {
    return NEXT[key] ?? null;
  }

  move(entry: BoardEntryDto, status: AppointmentDto.StatusEnum): void {
    this.appointments.changeAppointmentStatus(entry.id, { status })
      .subscribe(() => this.api.getBoard().subscribe((b) => this.board.set(b)));
  }

  waited(entry: BoardEntryDto): string {
    return entry.minutesWaiting === undefined || entry.minutesWaiting === null
      ? '' : this.t.translate('schedule.minutesWaiting', { minutes: entry.minutesWaiting });
  }
}
