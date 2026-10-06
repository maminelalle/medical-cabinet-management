import { Component, DestroyRef, inject } from '@angular/core';
import { DatePipe } from '@angular/common';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { RouterLink } from '@angular/router';
import { switchMap, timer } from 'rxjs';
import { AdminService } from '../../core/admin/admin.service';
import { TableauBordAdmin, Utilisateur, libelleRole } from '../../core/models/admin';
import { PresenceMedecinsComponent } from '../../shared/presence/presence-medecins.component';

/** Tableau de bord de l'administrateur : comptes, connexions du jour, sessions, activite recente. */
@Component({
  selector: 'app-admin-dashboard',
  standalone: true,
  imports: [DatePipe, RouterLink, PresenceMedecinsComponent],
  template: `
    <section class="admin-page">
      <header class="page-heading">
        <div><p class="breadcrumb">ADMINISTRATION / TABLEAU DE BORD</p><h1>Administration</h1><p class="subtitle">Comptes, connexions, sessions et activité de toute l’application.</p></div>
        <div class="page-actions"><a class="soft-button" routerLink="/admin/journal">Journal d’activité</a><a class="primary-button" routerLink="/admin/utilisateurs">Gérer les utilisateurs</a></div>
      </header>
      @if (erreur) { <div class="alert danger"><span>×</span><div><strong>Indisponible</strong>{{ erreur }}</div></div> }
      @if (donnees; as d) {
        <section class="stats-grid">
          <article class="stat-card"><span class="stat-icon blue">♙</span><div><span>Utilisateurs</span><strong>{{ d.utilisateurs }}</strong><small>{{ d.utilisateursActifs }} compte(s) actif(s)</small></div></article>
          <article class="stat-card"><span class="stat-icon teal">●</span><div><span>En ligne maintenant</span><strong>{{ d.enLigne }}</strong><small>{{ d.sessionsOuvertes }} session(s) ouverte(s)</small></div></article>
          <article class="stat-card"><span class="stat-icon violet">◷</span><div><span>Connectés aujourd’hui</span><strong>{{ d.connectesAujourdhui }}</strong><small>{{ d.utilisateursActifs - d.connectesAujourdhui }} ne se sont pas connectés</small></div></article>
          <article class="stat-card"><span class="stat-icon orange">⚑</span><div><span>Actions aujourd’hui</span><strong>{{ d.actionsAujourdhui }}</strong><small>{{ d.echecsConnexionAujourdhui }} échec(s) de connexion</small></div></article>
        </section>
        <section class="admin-grid">
          <article class="card">
            <div class="card-heading"><div><h2>Qui s’est connecté aujourd’hui ?</h2><p>Tous les comptes et leur dernière activité</p></div><a class="soft-button petit" routerLink="/admin/sessions">Sessions et appareils</a></div>
            <div class="liste-presence">
              @for (u of d.presence; track u.id) {
                <div>
                  <span class="avatar">{{ initiales(u) }}</span>
                  <span><strong>{{ nom(u) }}</strong><small class="muted">{{ libelleRole(u.role) }} · {{ u.derniereConnexion ? 'dernière connexion ' + (u.derniereConnexion | date:'dd/MM HH:mm') : 'jamais connecté' }}</small></span>
                  @if (!u.actif) { <span class="etat desactive">Désactivé</span> }
                  @else if (u.enLigne) { <span class="etat en_ligne">En ligne</span> }
                  @else if (u.connecteAujourdhui) { <span class="etat oui">Venu aujourd’hui</span> }
                  @else { <span class="etat non">Pas connecté</span> }
                </div>
              }
            </div>
          </article>
          <div class="admin-page">
            <app-presence-medecins />
            <article class="card">
              <div class="card-heading"><div><h2>Activité récente</h2><p>Dernières actions enregistrées</p></div><a class="soft-button petit" routerLink="/admin/journal">Tout voir</a></div>
              <ul class="journal-list">
                @for (e of d.dernieresActions; track e.id) {
                  <li><time>{{ e.dateAction | date:'HH:mm' }}</time><span><strong>{{ e.action }}</strong><small class="muted">{{ e.nomComplet || e.email || 'Anonyme' }}{{ e.description ? ' · ' + e.description : '' }}</small></span></li>
                } @empty { <li><span></span><span class="muted">Aucune action aujourd’hui.</span></li> }
              </ul>
            </article>
          </div>
        </section>
      } @else if (!erreur) { <section class="card loading-state">Chargement...</section> }
    </section>
  `,
  styleUrl: './admin.css'
})
export class AdminDashboardComponent {
  private readonly service = inject(AdminService);
  readonly libelleRole = libelleRole;
  donnees: TableauBordAdmin | null = null;
  erreur = '';

  constructor() {
    timer(0, 30000).pipe(switchMap(() => this.service.tableauDeBord()), takeUntilDestroyed(inject(DestroyRef))).subscribe({
      next: (donnees) => { this.donnees = donnees; this.erreur = ''; },
      error: () => this.erreur = 'Le tableau de bord d’administration est indisponible.'
    });
  }

  nom(u: Utilisateur): string { return [u.prenom, u.nom].filter(Boolean).join(' ') || u.email; }
  initiales(u: Utilisateur): string { return this.nom(u).split(/\s+/).map((mot) => mot[0]).join('').slice(0, 2).toUpperCase(); }
}
