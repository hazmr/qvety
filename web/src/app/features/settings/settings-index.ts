import { Component } from '@angular/core';
import { RouterLink } from '@angular/router';
import { TranslocoPipe } from '@jsverse/transloco';
import { NzIconModule } from 'ng-zorro-antd/icon';
import { TitleBar } from '../../layout/title-bar/title-bar';

interface SettingsSection { link: string; labelKey: string; hintKey: string; icon: string; }

/**
 * The menu of what an admin configures. One stacked list at both sizes: the same rows the phone needs
 * read as a menu on desktop, and a second sidebar beside the shell's own would only crowd the page.
 * Admin only (route guard + server 403 on every write).
 */
@Component({
  selector: 'app-settings-index',
  imports: [RouterLink, NzIconModule, TranslocoPipe, TitleBar],
  template: `
    <div class="form-layout">
      <app-title-bar titleKey="settings.title" />
      <ul class="rows">
        @for (s of sections; track s.link) {
          <li class="row" [routerLink]="s.link">
            <nz-icon [nzType]="s.icon" class="row__icon" />
            <div class="row__text">
              <div class="row__title">{{ s.labelKey | transloco }}</div>
              <div class="row__secondary">{{ s.hintKey | transloco }}</div>
            </div>
            <nz-icon nzType="right" class="row__chevron mirror-rtl" />
          </li>
        }
      </ul>
    </div>
  `,
})
export class SettingsIndex {
  readonly sections: SettingsSection[] = [
    { link: '/settings/users', labelKey: 'settings.users', hintKey: 'settings.usersHint', icon: 'team' },
    { link: '/settings/rooms', labelKey: 'settings.rooms', hintKey: 'settings.roomsHint', icon: 'home' },
    { link: '/settings/appointment-types', labelKey: 'settings.appointmentTypes', hintKey: 'settings.appointmentTypesHint', icon: 'clock-circle' },
    { link: '/settings/services', labelKey: 'settings.services', hintKey: 'settings.servicesHint', icon: 'tag' },
    { link: '/settings/export', labelKey: 'settings.export', hintKey: 'settings.exportHint', icon: 'download' },
  ];
}
