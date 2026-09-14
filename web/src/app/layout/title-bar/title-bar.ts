import { Component, inject, input, output } from '@angular/core';
import { RouterLink } from '@angular/router';
import { TranslocoPipe } from '@jsverse/transloco';
import { NzButtonModule } from 'ng-zorro-antd/button';
import { NzIconModule } from 'ng-zorro-antd/icon';
import { ViewportService } from '../../core/viewport.service';

/**
 * Page title. Phone: the 48 px title bar (back chevron, name, one action). Desktop: the title row
 * (20 px / 600, context beside it, primary action at the trailing edge). One component, both sizes.
 */
@Component({
  selector: 'app-title-bar',
  imports: [RouterLink, NzButtonModule, NzIconModule, TranslocoPipe],
  templateUrl: './title-bar.html',
  styleUrl: './title-bar.scss',
})
export class TitleBar {
  readonly viewport = inject(ViewportService);

  readonly titleKey = input<string>();
  readonly title = input<string>();
  readonly subtitle = input<string>();
  readonly back = input<unknown[]>();
  readonly actionKey = input<string>();
  readonly actionLink = input<unknown[]>();
  readonly actionClick = output<void>();
}
