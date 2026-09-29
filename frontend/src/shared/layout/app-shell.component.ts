import { Component, inject, signal } from '@angular/core';
import { Router, RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { AuthService } from '../../core/auth/auth.service';

@Component({
  selector: 'app-shell',
  standalone: true,
  imports: [RouterLink, RouterLinkActive, RouterOutlet],
  template: `
    <div class="app-shell" [class.is-collapsed]="collapsed()">
      <aside class="sidebar">
        <a class="brand" [routerLink]="homeLink">
          <span class="brand-mark">
            <svg viewBox="0 0 24 24" aria-hidden="true"><path d="M12 6.5v11M6.5 12h11" /></svg>
          </span>
          <span class="brand-copy">
            <small>Cabinet de groupe</small>
            <strong>Cabinets Médicaux</strong>
          </span>
        </a>

        <button
          type="button"
          class="collapse-toggle"
          (click)="toggleSidebar()"
          [attr.aria-expanded]="!collapsed()"
          [attr.aria-label]="collapsed() ? 'Déployer la navigation' : 'Réduire la navigation'"
        >
          <svg viewBox="0 0 24 24" aria-hidden="true"><path d="m14 7-5 5 5 5" /></svg>
        </button>

        <nav aria-label="Navigation principale">
          <p class="nav-label">Pilotage</p>
          <a [routerLink]="homeLink" routerLinkActive="active" [routerLinkActiveOptions]="{ exact: true }">
            <span class="nav-icon"><svg viewBox="0 0 24 24" aria-hidden="true"><rect x="3" y="3" width="7.5" height="7.5" rx="2.2" /><rect x="13.5" y="3" width="7.5" height="7.5" rx="2.2" /><rect x="3" y="13.5" width="7.5" height="7.5" rx="2.2" /><rect x="13.5" y="13.5" width="7.5" height="7.5" rx="2.2" /></svg></span>
            <span class="nav-text">Tableau de bord</span>
          </a>

          <p class="nav-label">Parcours patient</p>
          @if (patientFlow) {
            <a routerLink="/patients" routerLinkActive="active">
              <span class="nav-icon"><svg viewBox="0 0 24 24" aria-hidden="true"><circle cx="10" cy="8" r="3.2" /><path d="M4 19.5v-1.4A3.6 3.6 0 0 1 7.6 14.5h4.8a3.6 3.6 0 0 1 3.6 3.6v1.4" /><path d="M20 19.5v-1.3a3.4 3.4 0 0 0-2.6-3.3" /></svg></span>
              <span class="nav-text">Patients</span>
            </a>
            <a routerLink="/rendez-vous" routerLinkActive="active">
              <span class="nav-icon"><svg viewBox="0 0 24 24" aria-hidden="true"><rect x="3" y="5" width="18" height="16" rx="3" /><path d="M8 3v4M16 3v4M3 11h18" /></svg></span>
              <span class="nav-text">Rendez-vous</span>
            </a>
          } @else {
            <span class="muted-link">
              <span class="nav-icon"><svg viewBox="0 0 24 24" aria-hidden="true"><rect x="3" y="5" width="18" height="16" rx="3" /><path d="M8 3v4M16 3v4M3 11h18" /></svg></span>
              <span class="nav-text">Rendez-vous</span>
              <em>Sur rendez-vous</em>
            </span>
          }

          <p class="nav-label">Finance</p>
          @if (billing) {
            <a routerLink="/factures" routerLinkActive="active">
              <span class="nav-icon"><svg viewBox="0 0 24 24" aria-hidden="true"><path d="M6 3h12a1 1 0 0 1 1 1v17l-3-2-3 2-3-2-3 2V4a1 1 0 0 1 1-1Z" /><path d="M9.5 8.5h5M9.5 12.5h5" /></svg></span>
              <span class="nav-text">Facturation</span>
            </a>
          } @else {
            <span class="muted-link">
              <span class="nav-icon"><svg viewBox="0 0 24 24" aria-hidden="true"><path d="M6 3h12a1 1 0 0 1 1 1v17l-3-2-3 2-3-2-3 2V4a1 1 0 0 1 1-1Z" /><path d="M9.5 8.5h5M9.5 12.5h5" /></svg></span>
              <span class="nav-text">Facturation</span>
              <em>Bientôt</em>
            </span>
          }
          <span class="muted-link">
            <span class="nav-icon"><svg viewBox="0 0 24 24" aria-hidden="true"><path d="M4 19.5V5.5A1.5 1.5 0 0 1 5.5 4H16a1 1 0 0 1 1 1v14.5" /><path d="M4 19.5A1.5 1.5 0 0 0 5.5 21H19V8" /><path d="M8 8h5M8 12h5" /></svg></span>
            <span class="nav-text">Catalogue des actes</span>
            <em>Bientôt</em>
          </span>

          <p class="nav-label">Système</p>
          <a routerLink="/design-system" routerLinkActive="active">
            <span class="nav-icon"><svg viewBox="0 0 24 24" aria-hidden="true"><circle cx="12" cy="12" r="9" /><circle cx="9.2" cy="9.6" r="1.1" /><circle cx="14.8" cy="9.6" r="1.1" /><circle cx="10.4" cy="14.8" r="1.1" /><path d="M14.4 17.6c1-1.6 2.1-2.4 3.4-2.4" /></svg></span>
            <span class="nav-text">Design system</span>
          </a>
          <span class="muted-link">
            <span class="nav-icon"><svg viewBox="0 0 24 24" aria-hidden="true"><path d="M4 7h7M16 7h4M4 12h3M12 12h8M4 17h7M16 17h4" /><circle cx="13.5" cy="7" r="2.1" /><circle cx="9.5" cy="12" r="2.1" /><circle cx="13.5" cy="17" r="2.1" /></svg></span>
            <span class="nav-text">Paramètres</span>
            <em>Bientôt</em>
          </span>
        </nav>

        <div class="sidebar-footer">
          <div class="sidebar-user">
            <span class="avatar">{{ initials }}</span>
            <span>
              <strong>{{ displayName }}</strong>
              <small>{{ roleLabel }}</small>
            </span>
          </div>
          <button type="button" class="signout-button" (click)="logout()">
            <span class="nav-icon"><svg viewBox="0 0 24 24" aria-hidden="true"><path d="M15 4h3a2 2 0 0 1 2 2v12a2 2 0 0 1-2 2h-3" /><path d="m10 8-4 4 4 4M6 12h10" /></svg></span>
            <span class="nav-text">Déconnexion</span>
          </button>
        </div>
      </aside>

      <section class="main-area">
        <header class="topbar">
          <div class="search">
            <svg viewBox="0 0 24 24" aria-hidden="true"><circle cx="11" cy="11" r="6.5" /><path d="m16 16 4 4" /></svg>
            <input placeholder="Rechercher un patient, un rendez-vous..." aria-label="Recherche globale">
            <kbd>K</kbd>
          </div>
          <div class="topbar-actions">
            <button type="button" class="icon-button" aria-label="Notifications">
              <svg viewBox="0 0 24 24" aria-hidden="true"><path d="M18 9a6 6 0 1 0-12 0c0 4.4-1.5 5.6-1.5 5.6h15S18 13.4 18 9Z" /><path d="M10.4 18.4a2 2 0 0 0 3.2 0" /></svg>
              <i></i>
            </button>
            <div class="profile">
              <span class="avatar">{{ initials }}</span>
              <span>
                <strong>{{ displayName }}</strong>
                <small>{{ email }}</small>
              </span>
              <span class="chevron">⌄</span>
            </div>
            <button type="button" class="ghost-button logout-button" (click)="logout()">
              <svg viewBox="0 0 24 24" aria-hidden="true"><path d="M15 4h3a2 2 0 0 1 2 2v12a2 2 0 0 1-2 2h-3" /><path d="m10 8-4 4 4 4M6 12h10" /></svg>
              Déconnexion
            </button>
          </div>
        </header>
        <main class="page-content"><router-outlet /></main>
      </section>
    </div>
  `,
  styleUrl: './app-shell.component.css'
})
export class AppShellComponent {
  private readonly auth = inject(AuthService);
  private readonly router = inject(Router);

  readonly role = this.auth.role();
  readonly roleLabel = this.auth.role() === 'MEDECIN' ? 'MÉDECIN' : this.auth.role() === 'DIRECTION' ? 'DIRECTION' : 'ACCUEIL / CAISSE';
  readonly homeLink = this.role === 'DIRECTION' ? '/direction/dashboard' : this.role === 'MEDECIN' ? '/rendez-vous' : '/accueil';
  readonly displayName = this.auth.role() === 'MEDECIN' ? 'Dr. Ahmed Diallo' : 'Aminata Diallo';
  readonly email = this.auth.email() ?? 'contact@cabinet-medical.fr';
  readonly initials = this.displayName.split(' ').map((part) => part[0]).join('').slice(0, 2);
  readonly patientFlow = this.role !== 'DIRECTION';
  readonly billing = this.role === 'ACCUEIL' || this.role === 'DIRECTION';
  readonly collapsed = signal(false);

  toggleSidebar(): void { this.collapsed.update((value) => !value); }

  logout(): void {
    this.auth.logout();
    this.router.navigate(['/login']);
  }
}