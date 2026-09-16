import { Routes } from '@angular/router';
import { adminGuard, authGuard } from './core/auth.guard';
import { ChangePassword } from './features/change-password/change-password';
import { ClientDetail } from './features/clients/client-detail';
import { ClientForm } from './features/clients/client-form';
import { ClientsList } from './features/clients/clients-list';
import { Home } from './home/home';
import { PatientDetail } from './features/patients/patient-detail';
import { PatientForm } from './features/patients/patient-form';
import { Login } from './features/login/login';
import { UserForm } from './features/settings/users/user-form';
import { UsersList } from './features/settings/users/users-list';
import { Shell } from './layout/shell';

export const routes: Routes = [
  { path: 'login', component: Login },
  { path: 'change-password', component: ChangePassword, canActivate: [authGuard] },
  {
    path: '',
    component: Shell,
    canActivate: [authGuard],
    children: [
      { path: '', component: Home },
      { path: 'clients', component: ClientsList },
      { path: 'clients/new', component: ClientForm },
      { path: 'clients/:id', component: ClientDetail },
      { path: 'clients/:id/edit', component: ClientForm },
      { path: 'clients/:id/patients/new', component: PatientForm },
      { path: 'clients/:id/patients/:pid', component: PatientDetail },
      { path: 'clients/:id/patients/:pid/edit', component: PatientForm },
      { path: 'settings/users', canActivate: [adminGuard], children: [
        { path: '', component: UsersList },
        { path: 'new', component: UserForm },
        { path: ':id', component: UserForm },
      ] },
    ],
  },
  { path: '**', redirectTo: '' },
];
