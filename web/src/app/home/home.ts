import { Component, inject, signal } from '@angular/core';
import { PracticeApi, PracticeDto } from '../api';

/**
 * Part 02: the first end-to-end line. Shows the practice name from GET /api/v1/practice.
 * The practice id is the dev seed's fixed uuid sent as X-Practice-Id; part 03 replaces the
 * header with the JWT and part 05 moves the strings into i18n files.
 */
const DEV_PRACTICE_ID = '00000000-0000-7000-8000-000000000001';

@Component({
  selector: 'app-home',
  templateUrl: './home.html',
})
export class Home {
  private readonly practiceApi = inject(PracticeApi);

  readonly practice = signal<PracticeDto | null>(null);
  readonly error = signal<string | null>(null);

  constructor() {
    this.practiceApi.getPractice(DEV_PRACTICE_ID).subscribe({
      next: (p) => this.practice.set(p),
      error: (e) => this.error.set(e?.status ? `HTTP ${e.status}` : 'error'),
    });
  }
}
