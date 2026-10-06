import { Routes } from '@angular/router';
import { authGuard } from '../core/auth/auth.guard';
import { roleGuard } from '../core/auth/role.guard';
import { AppShellComponent } from '../shared/layout/app-shell.component';

const TOUS = ['ACCUEIL', 'MEDECIN', 'PHARMACIEN', 'DIRECTION', 'ADMIN'];
const DOSSIERS = ['ACCUEIL', 'MEDECIN', 'DIRECTION', 'ADMIN'];
const impression = () => import('../features/impression/impression.component').then((module) => module.ImpressionComponent);

export const routes: Routes = [
  { path: 'login', loadComponent: () => import('../features/auth/login.component').then((module) => module.LoginComponent) },
  {
    path: '', component: AppShellComponent, canActivate: [authGuard], children: [
      { path: '', pathMatch: 'full', loadComponent: () => import('../shared/layout/default-redirect.component').then((module) => module.DefaultRedirectComponent) },

      // Tableaux de bord
      { path: 'accueil', loadComponent: () => import('../features/accueil/dashboard.component').then((module) => module.DashboardComponent), canActivate: [roleGuard], data: { roles: ['ACCUEIL'] } },
      { path: 'medecin/dashboard', loadComponent: () => import('../features/medecin/medecin-dashboard.component').then((module) => module.MedecinDashboardComponent), canActivate: [roleGuard], data: { roles: ['MEDECIN'] } },
      { path: 'direction/dashboard', loadComponent: () => import('../features/direction/direction-dashboard.component').then((module) => module.DirectionDashboardComponent), canActivate: [roleGuard], data: { roles: ['DIRECTION', 'ADMIN'] } },

      // Parcours patient
      { path: 'patients', loadComponent: () => import('../features/accueil/patients.component').then((module) => module.PatientsComponent), canActivate: [roleGuard], data: { roles: DOSSIERS } },
      { path: 'dossier/:patientId', loadComponent: () => import('../features/medecin/dossier-patient.component').then((module) => module.DossierPatientComponent), canActivate: [roleGuard], data: { roles: DOSSIERS } },
      { path: 'rendez-vous', loadComponent: () => import('../features/accueil/appointments.component').then((module) => module.AppointmentsComponent), canActivate: [roleGuard], data: { roles: ['ACCUEIL', 'MEDECIN'] } },
      { path: 'direction/rendez-vous', loadComponent: () => import('../features/accueil/appointments.component').then((module) => module.AppointmentsComponent), canActivate: [roleGuard], data: { roles: ['DIRECTION', 'ADMIN'] } },
      { path: 'rendez-vous/nouveau', loadComponent: () => import('../features/accueil/nouveau-rendez-vous.component').then((module) => module.NouveauRendezVousComponent), canActivate: [roleGuard], data: { roles: ['ACCUEIL'] } },
      { path: 'rendez-vous/:id/modifier', loadComponent: () => import('../features/accueil/nouveau-rendez-vous.component').then((module) => module.NouveauRendezVousComponent), canActivate: [roleGuard], data: { roles: ['ACCUEIL'] } },
      { path: 'rendez-vous/:id/consultation', loadComponent: () => import('../features/medecin/consultation.component').then((module) => module.ConsultationComponent), canActivate: [roleGuard], data: { roles: ['MEDECIN'] } },
      { path: 'actes', loadComponent: () => import('../features/actes/actes.component').then((module) => module.ActesComponent), canActivate: [roleGuard], data: { roles: DOSSIERS } },
      { path: 'ordonnances', loadComponent: () => import('../features/ordonnances/ordonnances.component').then((module) => module.OrdonnancesComponent), canActivate: [roleGuard], data: { roles: TOUS } },

      // Finance
      { path: 'factures', loadComponent: () => import('../features/facturation/factures.component').then((module) => module.FacturesComponent), canActivate: [roleGuard], data: { roles: TOUS } },
      { path: 'factures/nouveau', loadComponent: () => import('../features/facturation/nouvelle-facture.component').then((module) => module.NouvelleFactureComponent), canActivate: [roleGuard], data: { roles: ['ACCUEIL'] } },
      { path: 'catalogue', loadComponent: () => import('../features/catalogue/catalogue.component').then((module) => module.CatalogueComponent), canActivate: [roleGuard], data: { roles: ['ACCUEIL', 'MEDECIN', 'DIRECTION', 'ADMIN'] } },
      { path: 'pharmacie', loadComponent: () => import('../features/pharmacie/pharmacie.component').then((module) => module.PharmacieComponent), canActivate: [roleGuard], data: { roles: ['PHARMACIEN', 'DIRECTION', 'ADMIN'] } },
      { path: 'pharmacie/medicaments/:id', loadComponent: () => import('../features/pharmacie/medicament-detail.component').then((module) => module.MedicamentDetailComponent), canActivate: [roleGuard], data: { roles: ['PHARMACIEN', 'DIRECTION', 'ADMIN'] } },

      // Documents imprimables
      { path: 'impression/facture/:id', loadComponent: impression, canActivate: [roleGuard], data: { roles: TOUS, type: 'facture' } },
      { path: 'impression/recu/:id', loadComponent: impression, canActivate: [roleGuard], data: { roles: TOUS, type: 'recu' } },
      { path: 'impression/rendez-vous/:id', loadComponent: impression, canActivate: [roleGuard], data: { roles: DOSSIERS, type: 'rendez-vous' } },
      { path: 'impression/ordonnance/:id', loadComponent: impression, canActivate: [roleGuard], data: { roles: TOUS, type: 'ordonnance' } },
      { path: 'impression/dossier/:id', loadComponent: impression, canActivate: [roleGuard], data: { roles: DOSSIERS, type: 'dossier' } },
      { path: 'impression/acte/:id', loadComponent: impression, canActivate: [roleGuard], data: { roles: DOSSIERS, type: 'acte' } },

      // Administration
      { path: 'admin', loadComponent: () => import('../features/admin/admin-dashboard.component').then((module) => module.AdminDashboardComponent), canActivate: [roleGuard], data: { roles: ['ADMIN'] } },
      { path: 'admin/utilisateurs', loadComponent: () => import('../features/admin/utilisateurs.component').then((module) => module.UtilisateursComponent), canActivate: [roleGuard], data: { roles: ['ADMIN'] } },
      { path: 'admin/sessions', loadComponent: () => import('../features/admin/sessions.component').then((module) => module.SessionsComponent), canActivate: [roleGuard], data: { roles: ['ADMIN'] } },
      { path: 'admin/journal', loadComponent: () => import('../features/admin/journal.component').then((module) => module.JournalComponent), canActivate: [roleGuard], data: { roles: ['ADMIN'] } },
      { path: 'admin/permissions', loadComponent: () => import('../features/admin/permissions.component').then((module) => module.PermissionsComponent), canActivate: [roleGuard], data: { roles: ['ADMIN'] } },
      { path: 'admin/cabinet', loadComponent: () => import('../features/admin/parametres-cabinet.component').then((module) => module.ParametresCabinetComponent), canActivate: [roleGuard], data: { roles: ['ADMIN'] } },

      // Systeme
      { path: 'parametres', loadComponent: () => import('../features/settings/settings.component').then((module) => module.SettingsComponent), canActivate: [roleGuard], data: { roles: TOUS } },
      { path: 'design-system', loadComponent: () => import('../features/demo/ui-kit.component').then((module) => module.UiKitComponent), canActivate: [roleGuard], data: { roles: ['DIRECTION', 'ADMIN'] } }
    ]
  },
  { path: '**', redirectTo: '' }
];
