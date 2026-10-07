import { Component, DestroyRef, ElementRef, HostListener, inject, signal, viewChild } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { interval } from 'rxjs';
import { FormsModule } from '@angular/forms';
import { Router, RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { AuthService } from '../../core/auth/auth.service';
import { FactureService } from '../../core/factures/facture.service';
import { TempsReelService } from '../../core/temps-reel/temps-reel.service';
import { ChoixLangueComponent } from '../langue/choix-langue.component';
import { ApparenceService } from '../../core/apparence/apparence.service';

@Component({
  selector: 'app-shell',
  standalone: true,
  imports: [ChoixLangueComponent, FormsModule, RouterLink, RouterLinkActive, RouterOutlet],
  template: `
    <div class="app-shell" [class.is-collapsed]="collapsed()">
      <aside class="sidebar">
        <a class="brand" [routerLink]="homeLink">
          <span class="brand-mark" [class.avec-logo]="apparence().logo">
            @if (apparence().logo) {
              <img [src]="apparence().logo" alt="">
            } @else {
              <svg viewBox="0 0 24 24" aria-hidden="true"><path d="M12 6.5v11M6.5 12h11" /></svg>
            }
          </span>
          <span class="brand-copy">
            @if (apparence().sousTitre) { <small>{{ apparence().sousTitre }}</small> }
            <strong>{{ apparence().nomInterface }}</strong>
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
          @if (role !== 'PHARMACIEN') {
            <a routerLink="/patients" routerLinkActive="active">
              <span class="nav-icon"><svg viewBox="0 0 24 24" aria-hidden="true"><circle cx="10" cy="8" r="3.2" /><path d="M4 19.5v-1.4A3.6 3.6 0 0 1 7.6 14.5h4.8a3.6 3.6 0 0 1 3.6 3.6v1.4" /><path d="M20 19.5v-1.3a3.4 3.4 0 0 0-2.6-3.3" /></svg></span>
              <span class="nav-text">{{ role === 'MEDECIN' ? 'Mes patients' : 'Patients et dossiers' }}</span>
            </a>
            <a [routerLink]="lectureSeule ? '/direction/rendez-vous' : '/rendez-vous'" routerLinkActive="active" [routerLinkActiveOptions]="{ exact: true }">
              <span class="nav-icon"><svg viewBox="0 0 24 24" aria-hidden="true"><rect x="3" y="5" width="18" height="16" rx="3" /><path d="M8 3v4M16 3v4M3 11h18" /></svg></span>
              <span class="nav-text">{{ role === 'MEDECIN' ? 'Mon planning' : 'Rendez-vous' }}</span>
            </a>
            <a routerLink="/actes" routerLinkActive="active">
              <span class="nav-icon"><svg viewBox="0 0 24 24" aria-hidden="true"><rect x="4" y="4" width="16" height="16" rx="3" /><path d="M12 8v8M8 12h8" /></svg></span>
              <span class="nav-text">{{ role === 'MEDECIN' ? 'Mes actes programmés' : 'Actes programmés' }}</span>
            </a>
            <a routerLink="/soins" routerLinkActive="active">
              <span class="nav-icon"><svg viewBox="0 0 24 24" aria-hidden="true"><path d="m18 3 3 3M16.5 4.5l3 3M19.5 7.5 9 18l-3 .9.9-3L17.5 5.5" /><path d="m6 18-3 3M12 9l3 3" /></svg></span>
              <span class="nav-text">Soins et injections</span>
            </a>
          }
          <a routerLink="/ordonnances" routerLinkActive="active">
            <span class="nav-icon"><svg viewBox="0 0 24 24" aria-hidden="true"><path d="M7 3h7l5 5v12a1 1 0 0 1-1 1H7a1 1 0 0 1-1-1V4a1 1 0 0 1 1-1Z" /><path d="M14 3v5h5" /><path d="M9.5 12.5v5M9.5 12.5h2.2a1.6 1.6 0 0 1 0 3.2H9.5M11.4 15.7l2.6 2.8" /></svg></span>
            <span class="nav-text">{{ role === 'MEDECIN' ? 'Mes ordonnances' : 'Ordonnances' }}</span>
          </a>

          <p class="nav-label">Finance</p>
          <a routerLink="/factures" routerLinkActive="active">
            <span class="nav-icon"><svg viewBox="0 0 24 24" aria-hidden="true"><path d="M6 3h12a1 1 0 0 1 1 1v17l-3-2-3 2-3-2-3 2V4a1 1 0 0 1 1-1Z" /><path d="M9.5 8.5h5M9.5 12.5h5" /></svg></span>
            <span class="nav-text">Facturation</span>
          </a>
          @if (role === 'DIRECTION') {
            <a routerLink="/direction/tarifs" routerLinkActive="active">
              <span class="nav-icon"><svg viewBox="0 0 24 24" aria-hidden="true"><path d="M3 12V4h8l10 10-8 8L3 12Z" /><circle cx="7.5" cy="8" r="1.5" /></svg></span>
              <span class="nav-text">Tarifs et gratuité</span>
            </a>
          } @else if (peutVoirCatalogue) {
            <a routerLink="/catalogue" routerLinkActive="active">
              <span class="nav-icon"><svg viewBox="0 0 24 24" aria-hidden="true"><path d="M4 19.5V5.5A1.5 1.5 0 0 1 5.5 4H16a1 1 0 0 1 1 1v14.5" /><path d="M4 19.5A1.5 1.5 0 0 0 5.5 21H19V8" /><path d="M8 8h5M8 12h5" /></svg></span>
              <span class="nav-text">Catalogue des actes</span>
            </a>
          }
          @if (lectureSeule) {
            <a routerLink="/pharmacie" routerLinkActive="active">
              <span class="nav-icon"><svg viewBox="0 0 24 24" aria-hidden="true"><path d="M8 3h8a2 2 0 0 1 2 2v14a2 2 0 0 1-2 2H8a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2Z" /><path d="M9 8h6M9 12h6M9 16h3" /></svg></span>
              <span class="nav-text">Pharmacie interne</span>
            </a>
          }

          @if (role === 'ADMIN') {
            <p class="nav-label">Administration</p>
            <a routerLink="/direction/dashboard" routerLinkActive="active">
              <span class="nav-icon"><svg viewBox="0 0 24 24" aria-hidden="true"><path d="M4 19h16M7 16V9M12 16V5M17 16v-4" /></svg></span>
              <span class="nav-text">Indicateurs direction</span>
            </a>
            <a routerLink="/admin/utilisateurs" routerLinkActive="active">
              <span class="nav-icon"><svg viewBox="0 0 24 24" aria-hidden="true"><circle cx="9" cy="8" r="3" /><path d="M3.5 19v-1a4 4 0 0 1 4-4h3a4 4 0 0 1 4 4v1" /><path d="M17 8v6M14 11h6" /></svg></span>
              <span class="nav-text">Utilisateurs</span>
            </a>
            <a routerLink="/admin/sessions" routerLinkActive="active">
              <span class="nav-icon"><svg viewBox="0 0 24 24" aria-hidden="true"><rect x="3" y="4" width="18" height="12" rx="2" /><path d="M8 20h8M12 16v4" /></svg></span>
              <span class="nav-text">Connexions et appareils</span>
            </a>
            <a routerLink="/admin/journal" routerLinkActive="active">
              <span class="nav-icon"><svg viewBox="0 0 24 24" aria-hidden="true"><path d="M6 4h12v16H6z" /><path d="M9 8h6M9 12h6M9 16h4" /></svg></span>
              <span class="nav-text">Journal d’activité</span>
            </a>
            <a routerLink="/admin/permissions" routerLinkActive="active">
              <span class="nav-icon"><svg viewBox="0 0 24 24" aria-hidden="true"><rect x="5" y="10" width="14" height="10" rx="2" /><path d="M8 10V7a4 4 0 0 1 8 0v3" /></svg></span>
              <span class="nav-text">Rôles et permissions</span>
            </a>
            <a routerLink="/admin/cabinet" routerLinkActive="active">
              <span class="nav-icon"><svg viewBox="0 0 24 24" aria-hidden="true"><path d="M4 20V9l8-5 8 5v11" /><path d="M10 20v-5h4v5" /></svg></span>
              <span class="nav-text">Coordonnées du cabinet</span>
            </a>
          }

          <p class="nav-label">Système</p>
          @if (role === 'DIRECTION') {
            <a routerLink="/direction/parametres" routerLinkActive="active">
              <span class="nav-icon"><svg viewBox="0 0 24 24" aria-hidden="true"><circle cx="9" cy="8" r="3" /><path d="M3.5 19v-1a4 4 0 0 1 4-4h3a4 4 0 0 1 4 4v1" /><path d="M17.5 9.5a2 2 0 1 0 0-4M20.5 19v-1a3.5 3.5 0 0 0-2.5-3.3" /></svg></span>
              <span class="nav-text">Paramètres · employés</span>
            </a>
          }
          @if (lectureSeule) {
            <a routerLink="/design-system" routerLinkActive="active">
              <span class="nav-icon"><svg viewBox="0 0 24 24" aria-hidden="true"><circle cx="12" cy="12" r="9" /><circle cx="9.2" cy="9.6" r="1.1" /><circle cx="14.8" cy="9.6" r="1.1" /><circle cx="10.4" cy="14.8" r="1.1" /><path d="M14.4 17.6c1-1.6 2.1-2.4 3.4-2.4" /></svg></span>
              <span class="nav-text">Design system</span>
            </a>
          }
          <a routerLink="/parametres" routerLinkActive="active">
            <span class="nav-icon"><svg viewBox="0 0 24 24" aria-hidden="true"><path d="M4 7h7M16 7h4M4 12h3M12 12h8M4 17h7M16 17h4" /><circle cx="13.5" cy="7" r="2.1" /><circle cx="9.5" cy="12" r="2.1" /><circle cx="13.5" cy="17" r="2.1" /></svg></span>
            <span class="nav-text">Mon compte</span>
          </a>
        </nav>

        <div class="sidebar-footer">
          <div class="sidebar-user">
            <span class="avatar">{{ auth.initiales() }}</span>
            <span>
              <strong>{{ auth.nomComplet() }}</strong>
              <small>{{ auth.libelleRole() }}</small>
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
          @if (peutRechercher) {
            <div class="search">
              <svg viewBox="0 0 24 24" aria-hidden="true"><circle cx="11" cy="11" r="6.5" /><path d="m16 16 4 4" /></svg>
              <input
                #champRecherche
                [(ngModel)]="recherche"
                (keyup.enter)="rechercher()"
                placeholder="Rechercher un patient puis Entrée..."
                aria-label="Recherche globale des patients"
              >
              <kbd>Ctrl K</kbd>
            </div>
          } @else {
            <p class="topbar-context">{{ role === 'PHARMACIEN' ? 'Pharmacie · stock et ventes' : role === 'ADMIN' ? 'Administration · comptes, sessions et activité' : 'Direction · pilotage de l’activité du cabinet' }}</p>
          }
          <div class="topbar-actions">
            <app-choix-langue />
            @if (billing) {
              <button
                type="button"
                class="icon-button"
                (click)="ouvrirFactures()"
                [attr.aria-label]="facturesImpayees() + ' facture(s) restant à encaisser'"
              >
                <svg viewBox="0 0 24 24" aria-hidden="true"><path d="M18 9a6 6 0 1 0-12 0c0 4.4-1.5 5.6-1.5 5.6h15S18 13.4 18 9Z" /><path d="M10.4 18.4a2 2 0 0 0 3.2 0" /></svg>
                @if (facturesImpayees() > 0) { <i>{{ facturesImpayees() }}</i> }
              </button>
            }
            <a class="profile" routerLink="/parametres" title="Mon compte">
              <span class="avatar">{{ auth.initiales() }}</span>
              <span>
                <strong>{{ auth.nomComplet() }}</strong>
                <small>{{ auth.emailAffiche() }}</small>
              </span>
              <span class="chevron">⌄</span>
            </a>
            <button type="button" class="ghost-button logout-button" (click)="logout()">
              <svg viewBox="0 0 24 24" aria-hidden="true"><path d="M15 4h3a2 2 0 0 1 2 2v12a2 2 0 0 1-2 2h-3" /><path d="m10 8-4 4 4 4M6 12h10" /></svg>
              Déconnexion
            </button>
          </div>
        </header>
        <main class="page-content"><router-outlet /></main>
        <div class="notifications" aria-live="polite">
          @for (notification of notifications(); track notification.id) {
            <div class="notification" [class.hors-ligne]="!notification.enLigne">
              <span class="point"></span>
              <span>{{ notification.texte }}</span>
              <button type="button" (click)="fermerNotification(notification.id)" aria-label="Fermer">×</button>
            </div>
          }
        </div>
      </section>
    </div>
  `,
  styleUrl: './app-shell.component.css'
})
export class AppShellComponent {
  readonly auth = inject(AuthService);
  readonly apparence = inject(ApparenceService).apparence;
  private readonly router = inject(Router);
  private readonly factureService = inject(FactureService);

  readonly role = this.auth.role();
  readonly homeLink = this.role === 'ADMIN' ? '/admin' : this.role === 'DIRECTION' ? '/direction/dashboard' : this.role === 'MEDECIN' ? '/medecin/dashboard' : this.role === 'PHARMACIEN' ? '/pharmacie' : '/accueil';
  /** Direction et administration consultent sans modifier le parcours patient. */
  readonly lectureSeule = this.role === 'DIRECTION' || this.role === 'ADMIN';
  readonly billing = this.role !== null;
  readonly peutRechercher = this.role !== 'PHARMACIEN';
  readonly peutVoirCatalogue = this.role !== 'PHARMACIEN';
  readonly collapsed = signal(false);
  readonly facturesImpayees = signal(0);
  readonly notifications = signal<{ id: number; texte: string; enLigne: boolean }[]>([]);
  private compteurNotifications = 0;
  private readonly tempsReel = inject(TempsReelService);
  /** Accueil, direction et administration sont prevenus des arrivees et departs. */
  readonly voitPresence = this.role === 'ACCUEIL' || this.role === 'DIRECTION' || this.role === 'ADMIN';
  readonly champRecherche = viewChild<ElementRef<HTMLInputElement>>('champRecherche');
  recherche = '';

  constructor() {
    const destruction = inject(DestroyRef);
    // Signal de presence toutes les minutes : alimente "en ligne" (administration) et la presence des medecins.
    interval(60000).pipe(takeUntilDestroyed(destruction)).subscribe(() => this.auth.ping().subscribe({ error: () => undefined }));
    // Flux temps reel : le serveur sait que l'application est ouverte et previent des connexions / departs.
    this.tempsReel.connecter();
    destruction.onDestroy(() => this.tempsReel.deconnecter());
    this.tempsReel.evenements$.pipe(takeUntilDestroyed(destruction)).subscribe((evenement) => {
      const presence = evenement.type === 'EN_LIGNE' || evenement.type === 'HORS_LIGNE';
      if (presence && this.voitPresence && evenement.message && evenement.utilisateurId !== this.auth.profil()?.id) {
        this.notifier(evenement.message, evenement.type === 'EN_LIGNE');
      }
    });
    if (this.billing) {
      this.factureService.list().subscribe({
        next: (factures) => this.facturesImpayees.set(factures.filter((facture) => Number(facture.resteAPayer) > 0).length),
        error: () => this.facturesImpayees.set(0)
      });
    }
  }

  toggleSidebar(): void { this.collapsed.update((value) => !value); }

  /** Recherche globale : ouvre la liste des patients filtree sur le terme saisi. */
  rechercher(): void {
    const terme = this.recherche.trim();
    if (!terme) { return; }
    this.router.navigate(['/patients'], { queryParams: { q: terme } });
    this.recherche = '';
  }

  ouvrirFactures(): void { this.router.navigate(['/factures']); }

  /** Raccourci clavier Ctrl+K (ou Cmd+K) pour atteindre la recherche globale. */
  @HostListener('window:keydown', ['$event'])
  raccourciRecherche(evenement: KeyboardEvent): void {
    if ((evenement.ctrlKey || evenement.metaKey) && evenement.key.toLowerCase() === 'k') {
      evenement.preventDefault();
      this.champRecherche()?.nativeElement.focus();
    }
  }

  /** Notification ephemere (6 s) en bas d'ecran. */
  private notifier(texte: string, enLigne: boolean): void {
    const id = ++this.compteurNotifications;
    this.notifications.update((liste) => [...liste.slice(-3), { id, texte, enLigne }]);
    setTimeout(() => this.fermerNotification(id), 6000);
  }

  fermerNotification(id: number): void { this.notifications.update((liste) => liste.filter((item) => item.id !== id)); }

  /** Deconnexion : la session est fermee cote serveur (et journalisee), le flux temps reel aussi. */
  logout(): void {
    this.tempsReel.deconnecter();
    this.auth.deconnecter().subscribe(() => this.router.navigate(['/login']));
  }
}