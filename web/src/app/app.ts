import { Component, inject } from '@angular/core';
import { BidiModule } from '@angular/cdk/bidi';
import { RouterOutlet } from '@angular/router';
import { LocaleService } from './core/locale.service';

/** The CDK dir directive here is what NG-ZORRO components read (Directionality); it follows the locale signal. */
@Component({
  imports: [RouterOutlet, BidiModule],
  selector: 'app-root',
  templateUrl: './app.html',
})
export class App {
  readonly locale = inject(LocaleService);
}
