import { Routes } from '@angular/router';
import { authGuard } from '../core/auth/auth.guard';
import { roleGuard } from '../core/auth/role.guard';

export const routes: Routes = [
  { path: 'login', loadComponent: () => import('../features/auth/login.component').then((module) => module.LoginComponent) },
  { path: 'patients', loadComponent: () => import('../features/accueil/patients.component').then((module) => module.PatientsComponent), canActivate: [authGuard, roleGuard], data: { roles: ['ACCUEIL', 'MEDECIN'] } },
  { path: '', pathMatch: 'full', redirectTo: 'patients' },
  { path: '**', redirectTo: 'patients' }
];