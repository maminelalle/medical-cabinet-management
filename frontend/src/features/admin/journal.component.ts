import { Component, inject } from '@angular/core';
import { DatePipe } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute } from '@angular/router';
import { AdminService } from '../../core/admin/admin.service';
import { EntreeJournal, Utilisateur, libelleRole } from '../../core/models/admin';

/** Journal d'activite : toutes les actions des utilisateurs (connexions, creations, paiements, exports...). */
@Component({
  selector: 'app-admin-journal',
  standalone: true,
  imports: [DatePipe, FormsModule],
  template: `
    <section class="admin-page">
      <header class="page-heading">
        <div><p class="breadcrumb">ADMINISTRATION / JOURNAL</p><h1>Journal d’activité</h1><p class="subtitle">Qui a fait quoi, quand et d’où. Aucune donnée médicale n’y est enregistrée.</p></div>
      </header>
      <article class="card table-card">
        <div class="table-heading">
          <div><h2>{{ filtrees.length }} action(s)</h2><p>Les plus récentes en premier</p></div>
          <div class="filters">
            <select [(ngModel)]="utilisateurId" (change)="charger()" aria-label="Utilisateur">
              <option [ngValue]="null">Tous les utilisateurs</option>
              @for (u of utilisateurs; track u.id) { <option [ngValue]="u.id">{{ nom(u) }} ({{ libelleRole(u.role) }})</option> }
            </select>
            <select [(ngModel)]="jours" (change)="charger()" aria-label="Période">
              <option [ngValue]="1">Aujourd’hui</option><option [ngValue]="7">7 derniers jours</option><option [ngValue]="30">30 derniers jours</option>
            </select>
            <input type="search" [(ngModel)]="recherche" placeholder="Filtrer une action...">
          </div>
        </div>
        <div class="table-wrap"><table>
          <thead><tr><th>Date</th><th>Utilisateur</th><th>Action</th><th>Détail</th><th>Adresse IP</th></tr></thead>
          <tbody>
            @for (e of filtrees; track e.id) {
              <tr>
                <td>{{ e.dateAction | date:'dd/MM/yyyy HH:mm:ss' }}</td>
                <td><strong>{{ e.nomComplet || e.email || 'Anonyme' }}</strong><small>{{ e.role ? libelleRole(e.role) : '' }}</small></td>
                <td><strong>{{ e.action }}</strong></td>
                <td><small>{{ e.description || '' }}{{ e.chemin ? ' · ' + e.methode + ' ' + e.chemin : '' }}</small></td>
                <td>{{ e.adresseIp || '—' }}</td>
              </tr>
            } @empty { <tr><td colspan="5" class="empty-cell">Aucune action sur la période.</td></tr> }
          </tbody>
        </table></div>
      </article>
    </section>
  `,
  styleUrl: './admin.css'
})
export class JournalComponent {
  private readonly service = inject(AdminService);
  readonly libelleRole = libelleRole;
  utilisateurs: Utilisateur[] = [];
  entrees: EntreeJournal[] = [];
  utilisateurId: number | null = Number(inject(ActivatedRoute).snapshot.queryParamMap.get('utilisateurId')) || null;
  jours = 7;
  recherche = '';

  constructor() {
    this.service.utilisateurs().subscribe({ next: (items) => this.utilisateurs = items });
    this.charger();
  }

  charger(): void {
    this.service.journal(this.utilisateurId ?? undefined, this.jours).subscribe({ next: (items) => this.entrees = items, error: () => this.entrees = [] });
  }

  get filtrees(): EntreeJournal[] {
    const terme = this.recherche.trim().toLowerCase();
    return terme ? this.entrees.filter((e) => `${e.action} ${e.description ?? ''} ${e.email ?? ''}`.toLowerCase().includes(terme)) : this.entrees;
  }

  nom(u: Utilisateur): string { return [u.prenom, u.nom].filter(Boolean).join(' ') || u.email; }
}
