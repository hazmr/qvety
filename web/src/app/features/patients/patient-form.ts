import { Component, inject, signal, viewChild } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { PatientDto, PatientRequestDto, PatientsApi } from '../../api';
import { FormField, FormPage } from '../../shared/form-page/form-page';
import { TitleBar } from '../../layout/title-bar/title-bar';
import { SEXES, SPECIES } from './patient-labels';

/** Create or edit a patient under one client. The owner is the route's client; changing it is a transfer. */
@Component({
  selector: 'app-patient-form',
  imports: [FormPage, TitleBar],
  templateUrl: './patient-form.html',
})
export class PatientForm {
  private readonly api = inject(PatientsApi);
  private readonly router = inject(Router);
  private readonly route = inject(ActivatedRoute);
  private readonly page = viewChild.required(FormPage);

  readonly clientId = this.route.snapshot.paramMap.get('id')!;
  readonly id = this.route.snapshot.paramMap.get('pid');
  readonly isNew = this.id === null;
  readonly current = signal<PatientDto | null>(null);
  readonly busy = signal(false);

  readonly fields: FormField[] = [
    { name: 'name', labelKey: 'patients.name', required: true, maxLength: 100 },
    { name: 'species', labelKey: 'patients.species', type: 'select', required: true,
      options: SPECIES.map((s) => ({ value: s, labelKey: 'species.' + s })) },
    { name: 'breed', labelKey: 'patients.breed', maxLength: 100 },
    { name: 'sex', labelKey: 'patients.sex', type: 'select', options: SEXES.map((s) => ({ value: s, labelKey: 'sex.' + s })) },
    { name: 'dateOfBirth', labelKey: 'patients.dateOfBirth', type: 'date', ltr: true },
    { name: 'ageApproximate', labelKey: 'patients.ageApproximate', maxLength: 50, hintKey: 'patients.ageApproximateHint' },
    { name: 'color', labelKey: 'patients.color', maxLength: 50 },
    { name: 'microchip', labelKey: 'patients.microchip', ltr: true, maxLength: 30 },
    { name: 'notes', labelKey: 'patients.notes', type: 'textarea', maxLength: 2000 },
  ];

  constructor() {
    if (this.id) {
      this.api.getPatient(this.id).subscribe((d) => this.current.set(d.patient));
    }
  }

  save(values: Record<string, string>): void {
    this.busy.set(true);
    const body: PatientRequestDto = {
      clientId: this.clientId,
      name: values['name'],
      species: values['species'] as PatientRequestDto.SpeciesEnum,
      breed: values['breed'] || undefined,
      sex: (values['sex'] || undefined) as PatientRequestDto.SexEnum | undefined,
      dateOfBirth: values['dateOfBirth'] || undefined,
      ageApproximate: values['ageApproximate'] || undefined,
      color: values['color'] || undefined,
      microchip: values['microchip'] || undefined,
      notes: values['notes'] || undefined,
    };
    const request = this.isNew
      ? this.api.createPatient(body)
      : this.api.updatePatient(this.id!, this.current()!.version, body);
    request.subscribe({
      next: (p) => { this.busy.set(false); this.router.navigate(['/clients', this.clientId, 'patients', p.id]); },
      error: (e) => { this.busy.set(false); this.page().fail(e); },
    });
  }
}
