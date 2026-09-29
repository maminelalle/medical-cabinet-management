import { Routes } from '@angular/router';
import { authGuard } from '../core/auth/auth.guard';
import { roleGuard } from '../core/auth/role.guard';
import { AppShellComponent } from '../shared/layout/app-shell.component';

export const routes: Routes = [
  { path: 'login', loadComponent: () => import('../features/auth/login.component').then((module) => module.LoginComponent) },
  {
    path: '', component: AppShellComponent, canActivate: [authGuard], children: [
      { path: '', pathMatch: 'full', loadComponent: () => import('../shared/layout/default-redirect.component').then((module) => module.DefaultRedirectComponent) },
      { path: 'accueil', loadComponent: () => import('../features/accueil/dashboard.component').then((module) => module.DashboardComponent), canActivate: [roleGuard], data: { roles: ['ACCUEIL'] } },
      { path: 'patients', loadComponent: () => import('../features/accueil/patients.component').then((module) => module.PatientsComponent), canActivate: [roleGuard], data: { roles: ['ACCUEIL', 'MEDECIN'] } },
      { path: 'rendez-vous', loadComponent: () => import('../features/accueil/appointments.component').then((module) => module.AppointmentsComponent), canActivate: [roleGuard], data: { roles: ['ACCUEIL', 'MEDECIN'] } },
      { path: 'rendez-vous/nouveau', loadComponent: () => import('../features/accueil/nouveau-rendez-vous.component').then((module) => module.NouveauRendezVousComponent), canActivate: [roleGuard], data: { roles: ['ACCUEIL'] } },
      { path: 'factures', loadComponent: () => import('../features/facturation/factures.component').then((module) => module.FacturesComponent), canActivate: [roleGuard], data: { roles: ['ACCUEIL', 'DIRECTION'] } },
      { path: 'factures/nouveau', loadComponent: () => import('../features/facturation/nouvelle-facture.component').then((module) => module.NouvelleFactureComponent), canActivate: [roleGuard], data: { roles: ['ACCUEIL'] } },
      { path: 'direction/dashboard', loadComponent: () => import('../features/direction/direction-dashboard.component').then((module) => module.DirectionDashboardComponent), canActivate: [roleGuard], data: { roles: ['DIRECTION'] } },
      { path: 'rendez-vous/:id/consultation', loadComponent: () => import('../features/medecin/consultation.component').then((module) => module.ConsultationComponent), canActivate: [roleGuard], data: { roles: ['MEDECIN'] } },
      { path: 'dossier/:patientId', loadComponent: () => import('../features/medecin/dossier-patient.component').then((module) => module.DossierPatientComponent), canActivate: [roleGuard], data: { roles: ['MEDECIN'] } },
      { path: 'design-system', loadComponent: () => import('../features/demo/ui-kit.component').then((module) => module.UiKitComponent) }
    ]
  },
  { path: '**', redirectTo: '' }
];