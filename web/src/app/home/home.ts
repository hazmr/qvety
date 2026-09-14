import { Component, inject, signal } from '@angular/core';
import { PracticeApi, PracticeDto } from '../api';
import { AuthService } from '../core/auth.service';

/** Shows the caller's practice from GET /api/v1/practice. Part 05 moves the strings into i18n files. */
@Component({
  selector: 'app-home',
  templateUrl: './home.html',
})
export class Home {
  private readonly practiceApi = inject(PracticeApi);
  readonly auth = inject(AuthService);

  readonly practice = signal<PracticeDto | null>(null);
  readonly error = signal<string | null>(null);

  constructor() {
    this.practiceApi.getPractice().subscribe({
      next: (p) => this.practice.set(p),
      error: (e) => this.error.set(e?.status ? `HTTP ${e.status}` : 'error'),
    });
  }
}
