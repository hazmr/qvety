import { DatePipe } from '@angular/common';
import { Component, computed, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { TranslocoPipe, TranslocoService } from '@jsverse/transloco';
import { NzButtonModule } from 'ng-zorro-antd/button';
import { NzDatePickerModule } from 'ng-zorro-antd/date-picker';
import { AppointmentDto, AppointmentsApi, DayDto, ScheduleApi } from '../../api';
import { AuthService } from '../../core/auth.service';
import { LocaleService } from '../../core/locale.service';
import { ViewportService } from '../../core/viewport.service';
import { TitleBar } from '../../layout/title-bar/title-bar';

/** Rows are 15 minutes tall; the grid is laid out in those units. */
const SLOT_MINUTES = 15;
const SLOT_HEIGHT = 20;
const DEFAULT_OPEN = 8;
const DEFAULT_CLOSE = 22;

type Filter = 'all' | 'waiting' | 'inExam';

/**
 * The clinic day. Phone (MOBILE.md screen 09): a filter row over stacked rows with a fixed time column.
 * Desktop: one column per veterinarian on a CSS grid, appointments placed by start and duration. The grid
 * uses inset-inline-start, never left, so it mirrors in Arabic without a second stylesheet.
 *
 * Every instant comes from the server in UTC and is rendered in the practice timezone the day carries.
 */
@Component({
  selector: 'app-schedule',
  imports: [TitleBar, TranslocoPipe, DatePipe, FormsModule, NzButtonModule, NzDatePickerModule],
  templateUrl: './schedule.html',
  styleUrl: './schedule.scss',
})
export class Schedule {
  private readonly api = inject(ScheduleApi);
  private readonly appointments = inject(AppointmentsApi);
  private readonly router = inject(Router);
  private readonly t = inject(TranslocoService);
  readonly viewport = inject(ViewportService);
  readonly locale = inject(LocaleService);
  readonly auth = inject(AuthService);

  readonly day = signal<DayDto | null>(null);
  readonly loading = signal(true);
  readonly date = signal<Date>(new Date());
  readonly filter = signal<Filter>('all');

  readonly canBook = computed(() => this.auth.canWriteClients());

  /**
   * The grid shows only what holds a slot. A cancelled row sits at the same coordinates as the booking
   * that replaced it, so two cards would stack exactly; the phone list still lists it with its tag.
   */
  readonly gridColumns = computed(() => (this.day()?.columns ?? [])
    .map((c) => ({ ...c, appointments: c.appointments.filter((a) => a.status !== 'cancelled' && a.status !== 'no_show') }))
    .filter((c) => c.appointments.length > 0));

  /** Every appointment of the day, in time order: what the phone list shows. */
  readonly rows = computed(() => {
    const all = (this.day()?.columns ?? []).flatMap((c) => c.appointments);
    return all.slice().sort((a, b) => a.startsAt.localeCompare(b.startsAt));
  });

  readonly visibleRows = computed(() => {
    const f = this.filter();
    return this.rows().filter((a) => (f === 'all' ? true : f === 'waiting' ? a.status === 'checked_in' : a.status === 'in_progress'));
  });

  readonly counts = computed(() => ({
    all: this.rows().length,
    waiting: this.rows().filter((a) => a.status === 'checked_in').length,
    inExam: this.rows().filter((a) => a.status === 'in_progress').length,
  }));

  /** The grid runs from opening to closing, widened if anything is booked outside them. */
  readonly bounds = computed(() => {
    const d = this.day();
    const times = this.rows().flatMap((a) => [this.minutesOfDay(a.startsAt), this.minutesOfDay(a.endsAt)]);
    const open = d?.opens ? this.toMinutes(d.opens) : DEFAULT_OPEN * 60;
    const close = d?.closes ? this.toMinutes(d.closes) : DEFAULT_CLOSE * 60;
    const from = Math.floor(Math.min(open, ...times, open) / 60) * 60;
    const to = Math.ceil(Math.max(close, ...times, close) / 60) * 60;
    return { from, to };
  });

  readonly slots = computed(() => {
    const { from, to } = this.bounds();
    return Array.from({ length: Math.ceil((to - from) / 60) }, (_, i) => from + i * 60);
  });

  readonly gridHeight = computed(() => {
    const { from, to } = this.bounds();
    return ((to - from) / SLOT_MINUTES) * SLOT_HEIGHT;
  });

  constructor() {
    this.load();
  }

  load(): void {
    this.loading.set(true);
    this.api.getDay(this.isoDate(this.date())).subscribe({
      next: (d) => { this.day.set(d); this.loading.set(false); },
      error: () => this.loading.set(false),
    });
  }

  onDate(value: Date | null): void {
    this.date.set(value ?? new Date());
    this.load();
  }

  goToday(): void {
    this.date.set(new Date());
    this.load();
  }

  book(): void {
    this.router.navigate(['/schedule/new'], { queryParams: { date: this.isoDate(this.date()) } });
  }

  open(a: AppointmentDto): void {
    this.router.navigate(['/schedule', a.id]);
  }

  /** Where a card sits in its column, in pixels from the top of the grid. */
  offset(a: AppointmentDto): number {
    return ((this.minutesOfDay(a.startsAt) - this.bounds().from) / SLOT_MINUTES) * SLOT_HEIGHT;
  }

  height(a: AppointmentDto): number {
    const minutes = this.minutesOfDay(a.endsAt) - this.minutesOfDay(a.startsAt);
    return Math.max((minutes / SLOT_MINUTES) * SLOT_HEIGHT, SLOT_HEIGHT);
  }

  slotOffset(minutes: number): number {
    return ((minutes - this.bounds().from) / SLOT_MINUTES) * SLOT_HEIGHT;
  }

  /** Wall-clock label of an instant in the practice timezone, always in Western digits. */
  timeOf(instant: string): string {
    return new Intl.DateTimeFormat('en-GB', {
      hour: '2-digit', minute: '2-digit', hour12: false, timeZone: this.zone(),
    }).format(new Date(instant));
  }

  labelOfSlot(minutes: number): string {
    return `${String(Math.floor(minutes / 60)).padStart(2, '0')}:00`;
  }

  statusKey(a: AppointmentDto): string {
    return 'appointmentStatus.' + a.status;
  }

  private zone(): string {
    return this.day()?.timezone ?? 'Africa/Cairo';
  }

  /** Minutes past midnight in the practice timezone; the grid is drawn in clinic wall-clock. */
  private minutesOfDay(instant: string): number {
    const [h, m] = this.timeOf(instant).split(':').map(Number);
    return h * 60 + m;
  }

  private toMinutes(time: string): number {
    const [h, m] = time.split(':').map(Number);
    return h * 60 + m;
  }

  private isoDate(d: Date): string {
    return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`;
  }
}
