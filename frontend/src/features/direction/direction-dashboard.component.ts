import { Component, inject } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { PresenceMedecinsComponent } from '../../shared/presence/presence-medecins.component';
import { forkJoin } from 'rxjs';
import { ChiffreAffaires, DashboardConsultations, Impayes } from '../../core/models/dashboard';
import { DashboardService } from '../../core/dashboard/dashboard.service';

@Component({
  selector: 'app-direction-dashboard',
  standalone: true,
  imports: [FormsModule, PresenceMedecinsComponent],
  templateUrl: './direction-dashboard.component.html',
  styleUrl: './direction-dashboard.component.css'
})
export class DirectionDashboardComponent {
  private readonly service = inject(DashboardService);

  activite: DashboardConsultations | null = null;
  finances: ChiffreAffaires | null = null;
  impayes: Impayes | null = null;
  loading = true;
  error = '';
  periode = 'mois';
  dateDebut: string | undefined = this.premierJourDuMois();
  dateFin: string | undefined = new Date().toISOString().slice(0, 10);

  constructor() { this.charger(); }

  private premierJourDuMois(): string {
    const maintenant = new Date();
    return new Date(Date.UTC(maintenant.getFullYear(), maintenant.getMonth(), 1)).toISOString().slice(0, 10);
  }
  private premierJourDeLAnnee(): string {
    return `${new Date().getFullYear()}-01-01`;
  }

  /** Agrege les trois jeux d'indicateurs exposes par /api/dashboard. */
  charger(): void {
    this.loading = true;
    this.error = '';
    forkJoin({
      activite: this.service.consultations(this.dateDebut, this.dateFin),
      finances: this.service.chiffreAffaires(this.dateDebut, this.dateFin),
      impayes: this.service.impayes()
    }).subscribe({
      next: (donnees) => {
        this.activite = donnees.activite;
        this.finances = donnees.finances;
        this.impayes = donnees.impayes;
      },
      error: () => { this.error = 'Les indicateurs sont momentanément indisponibles.'; },
      complete: () => { this.loading = false; }
    });
  }

  changerPeriode(periode: string): void {
    this.periode = periode;
    const aujourdHui = new Date().toISOString().slice(0, 10);
    this.dateFin = aujourdHui;
    this.dateDebut = periode === 'mois' ? this.premierJourDuMois() : periode === 'annee' ? this.premierJourDeLAnnee() : undefined;
    this.charger();
  }

  get consultationsPeriode(): number { return this.activite?.consultations ?? 0; }
  get rendezVousPeriode(): number { return this.activite?.rendezVous ?? 0; }
  get revenuEncaisse(): number { return this.finances?.totalEncaisse ?? 0; }
  get revenuFacture(): number { return this.finances?.totalFacture ?? 0; }
  get resteARecouvrer(): number { return this.impayes?.montantRestant ?? 0; }
  get tauxImpayees(): number { return this.impayes?.tauxImpayees ?? 0; }
  get nombreImpayees(): number { return this.impayes?.nombreImpayees ?? 0; }

  countStatut(statut: string): number {
    return this.impayes?.factures.filter((facture) => facture.statut === statut).length ?? 0;
  }

  /** Anneau de progression du taux de recouvrement. */
  get donutStyle(): string {
    const recouvrement = Math.max(0, Math.min(100, 100 - this.tauxImpayees));
    return `radial-gradient(closest-side, var(--bg-surface) 0 68%, transparent 69%), conic-gradient(var(--accent) 0 ${recouvrement}%, var(--bg-hover) ${recouvrement}% 100%)`;
  }

  format(valeur: number): string {
    return new Intl.NumberFormat('fr-FR').format(valeur) + ' MRU';
  }

  typeLabel(type: string): string {
    return type.replace('_', ' ').toLowerCase().replace(/^\w/, (lettre) => lettre.toUpperCase());
  }

  /** Largeur de barre relative au plus gros poste de la periode. */
  barWidth(montant: number): string {
    const montants = this.finances?.parTypeActe.map((item) => item.montant) ?? [];
    const maximum = montants.length ? Math.max(...montants) : 0;
    if (!maximum) return '6%';
    return `${Math.max(6, Math.round((montant / maximum) * 100))}%`;
  }
}