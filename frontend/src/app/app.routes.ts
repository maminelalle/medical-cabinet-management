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
      { path: 'medecin/dashboard', loadComponent: () => import('../features/medecin/medecin-dashboard.component').then((module) => module.MedecinDashboardComponent), canActivate: [roleGuard], data: { roles: ['MEDECIN'] } },
      { path: 'patients', loadComponent: () => import('../features/accueil/patients.component').then((module) => module.PatientsComponent), canActivate: [roleGuard], data: { roles: ['ACCUEIL', 'MEDECIN', 'DIRECTION'] } },
      { path: 'rendez-vous', loadComponent: () => import('../features/accueil/appointments.component').then((module) => module.AppointmentsComponent), canActivate: [roleGuard], data: { roles: ['ACCUEIL', 'MEDECIN'] } },
      { path: 'direction/rendez-vous', loadComponent: () => import('../features/accueil/appointments.component').then((module) => module.AppointmentsComponent), canActivate: [roleGuard], data: { roles: ['DIRECTION'] } },
      { path: 'rendez-vous/nouveau', loadComponent: () => import('../features/accueil/nouveau-rendez-vous.component').then((module) => module.NouveauRendezVousComponent), canActivate: [roleGuard], data: { roles: ['ACCUEIL'] } },
      { path: 'rendez-vous/:id/modifier', loadComponent: () => import('../features/accueil/nouveau-rendez-vous.component').then((module) => module.NouveauRendezVousComponent), canActivate: [roleGuard], data: { roles: ['ACCUEIL'] } },
      { path: 'factures', loadComponent: () => import('../features/facturation/factures.component').then((module) => module.FacturesComponent), canActivate: [roleGuard], data: { roles: ['ACCUEIL', 'MEDECIN', 'PHARMACIEN', 'DIRECTION'] } },
      { path: 'factures/nouveau', loadComponent: () => import('../features/facturation/nouvelle-facture.component').then((module) => module.NouvelleFactureComponent), canActivate: [roleGuard], data: { roles: ['ACCUEIL'] } },
      { path: 'impression/facture/:id', loadComponent: () => import('../features/impression/impression.component').then((module) => module.ImpressionComponent), canActivate: [roleGuard], data: { roles: ['ACCUEIL', 'MEDECIN', 'PHARMACIEN', 'DIRECTION'], type: 'facture' } },
      { path: 'impression/recu/:id', loadComponent: () => import('../features/impression/impression.component').then((module) => module.ImpressionComponent), canActivate: [roleGuard], data: { roles: ['ACCUEIL', 'MEDECIN', 'PHARMACIEN', 'DIRECTION'], type: 'recu' } },
      { path: 'impression/rendez-vous/:id', loadComponent: () => import('../features/impression/impression.component').then((module) => module.ImpressionComponent), canActivate: [roleGuard], data: { roles: ['ACCUEIL', 'MEDECIN', 'DIRECTION'], type: 'rendez-vous' } },
      { path: 'impression/ordonnance/:id', loadComponent: () => import('../features/impression/impression.component').then((module) => module.ImpressionComponent), canActivate: [roleGuard], data: { roles: ['ACCUEIL', 'MEDECIN', 'PHARMACIEN', 'DIRECTION'], type: 'ordonnance' } },
      { path: 'impression/dossier/:id', loadComponent: () => import('../features/impression/impression.component').then((module) => module.ImpressionComponent), canActivate: [roleGuard], data: { roles: ['ACCUEIL', 'MEDECIN', 'DIRECTION'], type: 'dossier' } },
      { path: 'direction/dashboard', loadComponent: () => import('../features/direction/direction-dashboard.component').then((module) => module.DirectionDashboardComponent), canActivate: [roleGuard], data: { roles: ['DIRECTION'] } },
      { path: 'rendez-vous/:id/consultation', loadComponent: () => import('../features/medecin/consultation.component').then((module) => module.ConsultationComponent), canActivate: [roleGuard], data: { roles: ['MEDECIN'] } },
      { path: 'dossier/:patientId', loadComponent: () => import('../features/medecin/dossier-patient.component').then((module) => module.DossierPatientComponent), canActivate: [roleGuard], data: { roles: ['ACCUEIL', 'MEDECIN', 'DIRECTION'] } },
      { path: 'ordonnances', loadComponent: () => import('../features/ordonnances/ordonnances.component').then((module) => module.OrdonnancesComponent), canActivate: [roleGuard], data: { roles: ['ACCUEIL', 'MEDECIN', 'PHARMACIEN', 'DIRECTION'] } },
      { path: 'catalogue', loadComponent: () => import('../features/catalogue/catalogue.component').then((module) => module.CatalogueComponent), canActivate: [roleGuard], data: { roles: ['ACCUEIL', 'MEDECIN', 'DIRECTION'] } },
      { path: 'pharmacie', loadComponent: () => import('../features/pharmacie/pharmacie.component').then((module) => module.PharmacieComponent), canActivate: [roleGuard], data: { roles: ['PHARMACIEN', 'DIRECTION'] } },
      { path: 'parametres', loadComponent: () => import('../features/settings/settings.component').then((module) => module.SettingsComponent), canActivate: [roleGuard], data: { roles: ['ACCUEIL', 'MEDECIN', 'PHARMACIEN', 'DIRECTION'] } },
      { path: 'design-system', loadComponent: () => import('../features/demo/ui-kit.component').then((module) => module.UiKitComponent), canActivate: [roleGuard], data: { roles: ['DIRECTION'] } }
    ]
  },
  { path: '**', redirectTo: '' }
];