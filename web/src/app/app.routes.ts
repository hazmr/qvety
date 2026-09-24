import { Routes } from '@angular/router';
import { adminGuard, authGuard, platformGuard } from './core/auth.guard';
import { ChangePassword } from './features/change-password/change-password';
import { ClientDetail } from './features/clients/client-detail';
import { ClientForm } from './features/clients/client-form';
import { ClientsList } from './features/clients/clients-list';
import { AdminLogin } from './features/admin/admin-login';
import { AdminShell } from './features/admin/admin-shell';
import { PracticeCreate } from './features/admin/practice-create';
import { PracticeDetail } from './features/admin/practice-detail';
import { PracticesList } from './features/admin/practices-list';
import { PracticeExport } from './features/settings/practice-export';
import { Board } from './features/scheduling/board';
import { AppointmentDetail } from './features/scheduling/appointment-detail';
import { AppointmentForm } from './features/scheduling/appointment-form';
import { Schedule } from './features/scheduling/schedule';
import { Home } from './home/home';
import { PatientDetail } from './features/patients/patient-detail';
import { PatientForm } from './features/patients/patient-form';
import { Login } from './features/login/login';
import { ReferenceForm } from './features/settings/reference/reference-form';
import { ReferenceList } from './features/settings/reference/reference-list';
import { SettingsIndex } from './features/settings/settings-index';
import { UserForm } from './features/settings/users/user-form';
import { UsersList } from './features/settings/users/users-list';
import { Shell } from './layout/shell';

export const routes: Routes = [
  { path: 'login', component: Login },
  // Qvety staff. Its own shell, its own session; a clinic login is no help here (part 11).
  { path: 'admin', component: AdminShell, children: [
    { path: '', pathMatch: 'full', redirectTo: 'practices' },
    { path: 'login', component: AdminLogin },
    { path: 'practices', canActivate: [platformGuard], children: [
      { path: '', component: PracticesList },
      { path: 'new', component: PracticeCreate },
      { path: ':id', component: PracticeDetail },
    ] },
  ] },
  { path: 'change-password', component: ChangePassword, canActivate: [authGuard] },
  {
    path: '',
    component: Shell,
    canActivate: [authGuard],
    children: [
      { path: '', component: Home },
      { path: 'schedule', component: Schedule },
      { path: 'schedule/new', component: AppointmentForm },
      { path: 'schedule/:id', component: AppointmentDetail },
      { path: 'schedule/:id/edit', component: AppointmentForm },
      { path: 'board', component: Board },
      { path: 'clients', component: ClientsList },
      { path: 'clients/new', component: ClientForm },
      { path: 'clients/:id', component: ClientDetail },
      { path: 'clients/:id/edit', component: ClientForm },
      { path: 'clients/:id/patients/new', component: PatientForm },
      { path: 'clients/:id/patients/:pid', component: PatientDetail },
      { path: 'clients/:id/patients/:pid/edit', component: PatientForm },
      { path: 'settings', canActivate: [adminGuard], children: [
        { path: '', component: SettingsIndex },
        { path: 'export', component: PracticeExport },
        { path: 'users', children: [
          { path: '', component: UsersList },
          { path: 'new', component: UserForm },
          { path: ':id', component: UserForm },
        ] },
        // One list and one form for every reference entity; `reference` names the config object (part 09).
        ...['rooms', 'appointment-types', 'services'].map((key) => ({
          path: key,
          data: { reference: key },
          children: [
            { path: '', component: ReferenceList, data: { reference: key } },
            { path: 'new', component: ReferenceForm, data: { reference: key } },
            { path: ':id', component: ReferenceForm, data: { reference: key } },
          ],
        })),
      ] },
    ],
  },
  { path: '**', redirectTo: '' },
];
