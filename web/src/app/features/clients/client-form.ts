import { Component, inject, signal, viewChild } from '@angular/core';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { TranslocoPipe } from '@jsverse/transloco';
import { NzAlertModule } from 'ng-zorro-antd/alert';
import { NzButtonModule } from 'ng-zorro-antd/button';
import { ClientDto, ClientRequestDto, ClientSavedDto, ClientsApi } from '../../api';
import { FormField, FormPage } from '../../shared/form-page/form-page';
import { TitleBar } from '../../layout/title-bar/title-bar';

/** Create or edit a client. Duplicate warnings show as a non-blocking alert with links; the save already happened. */
@Component({
  selector: 'app-client-form',
  imports: [FormPage, RouterLink, NzAlertModule, NzButtonModule, TranslocoPipe, TitleBar],
  templateUrl: './client-form.html',
})
export class ClientForm {
  private readonly api = inject(ClientsApi);
  private readonly router = inject(Router);
  private readonly route = inject(ActivatedRoute);
  private readonly page = viewChild.required(FormPage);

  readonly id = this.route.snapshot.paramMap.get('id');
  readonly isNew = this.id === null;
  readonly current = signal<ClientDto | null>(null);
  readonly busy = signal(false);
  readonly warnings = signal<ClientDto[]>([]);
  readonly savedId = signal<string | null>(null);

  readonly fields: FormField[] = [
    { name: 'fullName', labelKey: 'clients.fullName', required: true, maxLength: 200, hintKey: 'clients.fullNameHint' },
    { name: 'preferredName', labelKey: 'clients.preferredName', maxLength: 100 },
    { name: 'phone', labelKey: 'clients.phone', type: 'tel', ltr: true, maxLength: 30, hintKey: 'clients.reachableHint' },
    { name: 'phoneSecondary', labelKey: 'clients.phoneSecondary', type: 'tel', ltr: true, maxLength: 30 },
    { name: 'email', labelKey: 'clients.email', type: 'email', ltr: true, maxLength: 200 },
    { name: 'address', labelKey: 'clients.address', maxLength: 500 },
    { name: 'preferredLocale', labelKey: 'clients.preferredLocale', type: 'select',
      options: [{ value: 'ar-EG', labelKey: 'locales.ar-EG' }, { value: 'en-EG', labelKey: 'locales.en-EG' }] },
    { name: 'notes', labelKey: 'clients.notes', type: 'textarea', maxLength: 2000 },
  ];

  constructor() {
    if (this.id) {
      this.api.getClient(this.id).subscribe((c) => this.current.set(c));
    }
  }

  save(values: Record<string, string>): void {
    this.busy.set(true);
    const body: ClientRequestDto = {
      fullName: values['fullName'],
      preferredName: values['preferredName'] || undefined,
      phone: values['phone'] || undefined,
      phoneSecondary: values['phoneSecondary'] || undefined,
      email: values['email'] || undefined,
      address: values['address'] || undefined,
      notes: values['notes'] || undefined,
      preferredLocale: values['preferredLocale'] || undefined,
    };
    const request = this.isNew
      ? this.api.createClient(body)
      : this.api.updateClient(this.id!, this.current()!.version, body);
    request.subscribe({
      next: (r: ClientSavedDto) => {
        this.busy.set(false);
        if (r.warnings.length) {
          // saved; show the possible duplicates and let the desk decide
          this.warnings.set(r.warnings);
          this.savedId.set(r.client.id);
          this.current.set(r.client);
          return;
        }
        this.router.navigate(['/clients', r.client.id]);
      },
      error: (e) => { this.busy.set(false); this.page().fail(e); },
    });
  }
}
