import { Component, inject } from '@angular/core';
import { DatePipe } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { AdminService } from '../../core/admin/admin.service';
import { messageErreur } from '../../core/http/erreur-api';
import { SessionUtilisateur, libelleRole } from '../../core/models/admin';

/** Sessions de connexion : qui est connecte, depuis quel appareil, et revocation. */
@Component({
  selector: 'app-admin-sessions',
  standalone: true,
  imports: [DatePipe, FormsModule],
  template: `
    <section class="admin-page">
      <header class="page-heading">
        <div><p class="breadcrumb">ADMINISTRATION / SESSIONS</p><h1>Connexions et appareils</h1><p class="subtitle">Sessions ouvertes, appareils, navigateurs et adresses IP. Une session révoquée est déconnectée immédiatement.</p></div>
        <div class="page-actions">
          <select [(ngModel)]="periode" (change)="charger()" aria-label="Période">
            <option value="actives">Sessions ouvertes</option>
            <option value="jour">Connexions d’aujourd’hui</option>
            <option value="semaine">Connexions des 7 derniers jours</option>
          </select>
          <button class="soft-button" type="button" (click)="charger()">↻</button>
        </div>
      </header>
      @if (message) { <div class="alert success"><span>✓</span><div><strong>{{ message }}</strong></div></div> }
      @if (erreur) { <div class="alert danger"><span>×</span><div><strong>Action impossible</strong>{{ erreur }}</div></div> }
      <article class="card table-card">
        <div class="table-heading"><div><h2>{{ sessions.length }} session(s)</h2><p>{{ enLigne }} en ligne maintenant</p></div></div>
        <div class="table-wrap"><table>
          <thead><tr><th>Utilisateur</th><th>Appareil</th><th>Adresse IP</th><th>Connexion</th><th>Dernière activité</th><th>État</th><th></th></tr></thead>
          <tbody>
            @for (s of sessions; track s.id) {
              <tr>
                <td><strong>{{ s.nomComplet }}</strong><small>{{ s.email }} · {{ libelleRole(s.role) }}</small></td>
                <td>{{ s.appareil || '—' }}<small>{{ s.navigateur || '—' }} · {{ s.systeme || '—' }}</small></td>
                <td>{{ s.adresseIp || '—' }}</td>
                <td>{{ s.dateConnexion | date:'dd/MM/yyyy HH:mm' }}</td>
                <td>{{ s.derniereActivite | date:'dd/MM HH:mm:ss' }}@if (s.dateFin) { <small>fin {{ s.dateFin | date:'dd/MM HH:mm' }}</small> }</td>
                <td><span class="etat" [class]="'etat ' + s.statut.toLowerCase()">{{ libelleStatut(s.statut) }}</span></td>
                <td>@if (s.statut === 'EN_LIGNE' || s.statut === 'INACTIVE') { <button class="soft-button danger petit" type="button" (click)="revoquer(s)">Révoquer</button> }</td>
              </tr>
            } @empty { <tr><td colspan="7" class="empty-cell">Aucune session sur cette période.</td></tr> }
          </tbody>
        </table></div>
      </article>
    </section>
  `,
  styleUrl: './admin.css'
})
export class SessionsComponent {
  private readonly service = inject(AdminService);
  readonly libelleRole = libelleRole;
  periode: 'actives' | 'jour' | 'semaine' = 'actives';
  sessions: SessionUtilisateur[] = [];
  message = '';
  erreur = '';

  constructor() { this.charger(); }

  charger(): void {
    this.service.sessions(this.periode).subscribe({ next: (items) => this.sessions = items, error: () => this.erreur = 'Sessions indisponibles.' });
  }

  get enLigne(): number { return this.sessions.filter((s) => s.statut === 'EN_LIGNE').length; }

  revoquer(s: SessionUtilisateur): void {
    this.service.revoquerSession(s.id).subscribe({
      next: () => { this.message = `Session de ${s.nomComplet} révoquée : l’utilisateur est déconnecté.`; this.charger(); },
      error: (response) => this.erreur = messageErreur(response, 'Révocation impossible.')
    });
  }

  libelleStatut(statut: SessionUtilisateur['statut']): string {
    return ({ EN_LIGNE: 'En ligne', INACTIVE: 'Inactive', EXPIREE: 'Expirée', DECONNEXION: 'Déconnecté', REVOQUEE: 'Révoquée' } as const)[statut];
  }
}
