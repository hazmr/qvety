import { DatePipe } from '@angular/common';
import { Component, inject } from '@angular/core';
import { RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { TranslocoPipe } from '@jsverse/transloco';
import { NzButtonModule } from 'ng-zorro-antd/button';
import { AuthService } from '../core/auth.service';
import { LocaleService } from '../core/locale.service';

/** 32 px primary strip: mark, clinic name, today's date, language toggle, user, logout (frontend.md "Application chrome"). */
@Component({
  selector: 'app-shell',
  imports: [RouterOutlet, RouterLink, RouterLinkActive, NzButtonModule, TranslocoPipe, DatePipe],
  templateUrl: './shell.html',
  styleUrl: './shell.scss',
})
export class Shell {
  readonly auth = inject(AuthService);
  readonly locale = inject(LocaleService);
  readonly today = new Date();

  toggleLanguage(): void {
    const next = this.locale.other();
    this.locale.apply(next);                       // immediate
    this.auth.setLocale(next).subscribe();         // persisted on the user row
  }
}
