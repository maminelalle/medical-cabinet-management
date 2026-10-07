import { Component, DestroyRef, inject, signal } from '@angular/core';
import { DatePipe } from '@angular/common';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { debounceTime, merge, switchMap, timer } from 'rxjs';
import { TempsReelService } from '../../core/temps-reel/temps-reel.service';
import { AdminService } from '../../core/admin/admin.service';
import { PresenceMedecin, StatutPresence } from '../../core/models/admin';

/** Medecins presents au cabinet : connectes, en consultation, disponibles ou en retard (rafraichi toutes les 30 s). */
@Component({
  selector: 'app-presence-medecins',
  standalone: true,
  imports: [DatePipe],
  template: `
    <article class="card presence-card">
      <div class="card-heading">
        <div>
          <h2>Médecins au cabinet</h2>
          <p>{{ presents() }} connecté(s) sur {{ medecins().length }} · actualisé {{ derniereMaj() | date:'HH:mm:ss' }}</p>
        </div>
        <span class="live-dot" aria-hidden="true"></span>
      </div>
      @if (erreur()) { <div class="empty-state">{{ erreur() }}</div> }
      <div class="presence-list">
        @for (medecin of medecins(); track medecin.medecinId) {
          <div class="presence-row" [class]="'presence-row ' + medecin.statut.toLowerCase()">
            <span class="presence-avatar">{{ medecin.prenom[0] }}{{ medecin.nom[0] }}<i [class.on]="medecin.connecte"></i></span>
            <span class="presence-info">
              <strong>Dr. {{ medecin.prenom }} {{ medecin.nom }}</strong>
              <small>{{ medecin.specialite || 'Médecine générale' }} · {{ detail(medecin) }}</small>
            </span>
            <span class="presence-badge">{{ libelle(medecin.statut) }}</span>
          </div>
        } @empty {
          @if (!erreur()) { <div class="empty-state">Aucun médecin enregistré.</div> }
        }
      </div>
    </article>
  `,
  styles: [`
    .presence-card { display: grid; gap: 14px; }
    .live-dot { width: 10px; height: 10px; border-radius: 50%; background: var(--accent); box-shadow: 0 0 0 5px var(--accent-soft); }
    .presence-list { display: grid; gap: 8px; }
    .presence-row { display: flex; align-items: center; gap: 12px; padding: 10px 12px; border: 1px solid var(--border-soft); border-inline-start-width: 3px; border-radius: var(--radius-sm); background: var(--bg-subtle); }
    .presence-row.disponible { border-inline-start-color: var(--accent); }
    .presence-row.en_consultation { border-inline-start-color: var(--brand); }
    .presence-row.en_retard { border-inline-start-color: var(--warn); }
    .presence-row.absent { border-inline-start-color: var(--border-strong); opacity: .8; }
    .presence-avatar { position: relative; display: grid; place-items: center; width: 36px; height: 36px; flex: none; border-radius: 50%; background: var(--brand-soft); color: var(--brand-dark); font-size: var(--fs-xs); font-weight: 800; }
    .presence-avatar i { position: absolute; inset-inline-end: -1px; bottom: -1px; width: 11px; height: 11px; border: 2px solid var(--bg-surface); border-radius: 50%; background: var(--border-strong); }
    .presence-avatar i.on { background: var(--accent); }
    .presence-info { display: grid; gap: 2px; flex: 1; min-width: 0; }
    .presence-info strong { color: var(--text-strong); font-size: var(--fs-sm); }
    .presence-info small { overflow: hidden; color: var(--text-soft); font-size: var(--fs-label); text-overflow: ellipsis; white-space: nowrap; }
    .presence-badge { padding: 4px 10px; border-radius: var(--radius-pill); background: var(--bg-hover); font-size: var(--fs-label); font-weight: 800; white-space: nowrap; }
    .disponible .presence-badge { background: var(--accent-soft); color: var(--accent-text); }
    .en_consultation .presence-badge { background: var(--brand-soft); color: var(--brand-dark); }
    .en_retard .presence-badge { background: var(--warn-soft); color: var(--warn-text); }
  `]
})
export class PresenceMedecinsComponent {
  private readonly service = inject(AdminService);
  readonly medecins = signal<PresenceMedecin[]>([]);
  readonly derniereMaj = signal<Date | null>(null);
  readonly erreur = signal('');

  constructor() {
    // Rechargement a chaque connexion, depart ou consultation (temps reel), et toutes les 60 s par securite.
    merge(timer(0, 60000), inject(TempsReelService).changements$.pipe(debounceTime(300))).pipe(
      switchMap(() => this.service.presenceMedecins()),
      takeUntilDestroyed(inject(DestroyRef))
    ).subscribe({
      next: (items) => { this.medecins.set(items); this.derniereMaj.set(new Date()); this.erreur.set(''); },
      error: () => this.erreur.set('Présence des médecins indisponible.')
    });
  }

  presents(): number { return this.medecins().filter((medecin) => medecin.connecte).length; }

  libelle(statut: StatutPresence): string {
    return statut === 'EN_CONSULTATION' ? 'En consultation' : statut === 'EN_RETARD' ? 'En retard' : statut === 'DISPONIBLE' ? 'Disponible' : 'Absent';
  }

  detail(medecin: PresenceMedecin): string {
    if (medecin.statut === 'EN_CONSULTATION') return 'avec ' + medecin.patientEnCours;
    if (medecin.statut === 'EN_RETARD') return `${medecin.retardMinutes} min de retard · ${medecin.rendezVousRestants} RDV en attente`;
    if (medecin.prochainRendezVous) {
      const heure = new Date(medecin.prochainRendezVous).toLocaleTimeString('fr-FR', { hour: '2-digit', minute: '2-digit' });
      return `prochain RDV ${heure} · ${medecin.prochainPatient}`;
    }
    return medecin.rendezVousTermines ? `${medecin.rendezVousTermines} consultation(s) terminée(s)` : 'aucun RDV restant aujourd’hui';
  }
}
