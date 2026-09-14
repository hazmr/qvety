import { Component, inject, signal } from '@angular/core';
import { TranslocoPipe } from '@jsverse/transloco';
import { PracticeApi, PracticeDto } from '../api';
import { AuthService } from '../core/auth.service';

/** Shows the caller's practice from GET /api/v1/practice. Part 10 turns this into the day view. */
@Component({
  selector: 'app-home',
  imports: [TranslocoPipe],
  templateUrl: './home.html',
})
export class Home {
  private readonly practiceApi = inject(PracticeApi);
  readonly auth = inject(AuthService);

  readonly practice = signal<PracticeDto | null>(null);
  readonly error = signal(false);

  constructor() {
    this.practiceApi.getPractice().subscribe({
      next: (p) => this.practice.set(p),
      error: () => this.error.set(true),
    });
  }
}
