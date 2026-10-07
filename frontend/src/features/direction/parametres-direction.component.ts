import { Component, inject } from '@angular/core';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { PermissionsComponent } from '../admin/permissions.component';
import { SessionsComponent } from '../admin/sessions.component';
import { UtilisateursComponent } from '../admin/utilisateurs.component';
import { PresenceMedecinsComponent } from '../../shared/presence/presence-medecins.component';

type Onglet = 'employes' | 'sessions' | 'roles';

/**
 * Parametres de la direction : employes du cabinet (ajout, modification, desactivation, suppression),
 * leurs sessions et appareils, et les droits de chaque role. Presence et sessions se mettent a jour en temps reel.
 */
@Component({
  selector: 'app-parametres-direction',
  standalone: true,
  imports: [RouterLink, UtilisateursComponent, SessionsComponent, PermissionsComponent, PresenceMedecinsComponent],
  template: `
    <section class="parametres-page">
      <header class="page-heading">
        <div>
          <p class="breadcrumb">DIRECTION / PARAMÈTRES</p>
          <h1>Paramètres</h1>
          <p class="subtitle">Employés du cabinet, sessions de connexion et rôles. La présence se met à jour automatiquement.</p>
        </div>
        <a class="soft-button" routerLink="/parametres">Mon compte</a>
      </header>

      <nav class="tabs" aria-label="Sections des paramètres">
        <button type="button" [class.active]="onglet === 'employes'" (click)="onglet = 'employes'">Employés</button>
        <button type="button" [class.active]="onglet === 'sessions'" (click)="onglet = 'sessions'">Sessions et appareils</button>
        <button type="button" [class.active]="onglet === 'roles'" (click)="onglet = 'roles'">Rôles et permissions</button>
      </nav>

      @switch (onglet) {
        @case ('employes') {
          <div class="employes-grid">
            <app-admin-utilisateurs [integre]="true" />
            <app-presence-medecins />
          </div>
        }
        @case ('sessions') { <app-admin-sessions [integre]="true" /> }
        @case ('roles') { <app-admin-permissions [integre]="true" /> }
      }
    </section>
  `,
  styles: [`
    .parametres-page { display: grid; gap: 18px; }
    .tabs { display: flex; flex-wrap: wrap; gap: 6px; padding: 6px; border: 1px solid var(--border); border-radius: var(--radius-md); background: var(--bg-surface); }
    .tabs button { padding: 9px 14px; border: 0; border-radius: var(--radius-sm); background: transparent; color: var(--text-muted); font: inherit; font-size: var(--fs-sm); font-weight: 700; cursor: pointer; }
    .tabs button.active { background: var(--brand-soft); color: var(--brand-dark); }
    .employes-grid { display: grid; grid-template-columns: minmax(0, 1fr) minmax(280px, 340px); gap: 18px; align-items: start; }
    @media (max-width: 1200px) { .employes-grid { grid-template-columns: minmax(0, 1fr); } }
  `]
})
export class ParametresDirectionComponent {
  onglet: Onglet = (inject(ActivatedRoute).snapshot.queryParamMap.get('onglet') as Onglet) || 'employes';
}
