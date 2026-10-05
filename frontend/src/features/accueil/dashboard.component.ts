import { Component, inject } from '@angular/core';
import { RouterLink } from '@angular/router';
import { forkJoin } from 'rxjs';
import { AuthService } from '../../core/auth/auth.service';
import { Facture } from '../../core/models/facture';
import { RendezVous } from '../../core/models/rendez-vous';
import { FactureService } from '../../core/factures/facture.service';
import { PatientService } from '../../core/patients/patient.service';
import { RendezVousService } from '../../core/rendez-vous/rendez-vous.service';

interface EncaissementJour { date: string; libelle: string; montant: number; }
interface RepartitionPaiement { moyen: string; montant: number; pourcentage: number; couleur: string; }
interface CaseCalendrier { jour: number | null; aujourdHui: boolean; rendezVous: number; titre: string; }

@Component({ selector: 'app-dashboard', standalone: true, imports: [RouterLink], templateUrl: './dashboard.component.html', styleUrl: './dashboard.component.css' })
export class DashboardComponent {
  private readonly service = inject(RendezVousService);
  private readonly factureService = inject(FactureService);
  private readonly patientService = inject(PatientService);
  private readonly auth = inject(AuthService);
  private readonly couleursPaiement = ['var(--brand)', 'var(--accent)', 'var(--violet)', '#cbd5e1'];
  private readonly jours = ['Dim', 'Lun', 'Mar', 'Mer', 'Jeu', 'Ven', 'Sam'];

  readonly aujourdHui = new Date().toISOString().slice(0, 10);
  readonly joursSemaine = ['L', 'M', 'M', 'J', 'V', 'S', 'D'];
  private readonly mois = new Date();
  appointments: RendezVous[] = [];
  rendezVousMois: RendezVous[] = [];
  factures: Facture[] = [];
  nombrePatients = 0;
  loading = true;

  constructor() {
    forkJoin({
      rendezVous: this.service.list(this.aujourdHui),
      patients: this.patientService.list(),
      factures: this.factureService.list()
    }).subscribe({
      next: (donnees) => {
        this.appointments = donnees.rendezVous;
        this.nombrePatients = donnees.patients.length;
        this.factures = donnees.factures;
      },
      error: () => { this.appointments = []; this.factures = []; },
      complete: () => { this.loading = false; }
    });
    this.chargerMois();
  }

  /** Charge les rendez-vous du mois en cours pour alimenter le calendrier. */
  private chargerMois(): void {
    const annee = this.mois.getFullYear();
    const mois = String(this.mois.getMonth() + 1).padStart(2, '0');
    const dernierJour = String(new Date(annee, this.mois.getMonth() + 1, 0).getDate()).padStart(2, '0');
    this.service.listPeriode(`${annee}-${mois}-01`, `${annee}-${mois}-${dernierJour}`).subscribe({
      next: (items) => { this.rendezVousMois = items; },
      error: () => { this.rendezVousMois = []; }
    });
  }

  get prenomAffiche(): string { return this.auth.prenom() || 'à vous'; }

  get dateDuJour(): string {
    const libelle = new Date().toLocaleDateString('fr-FR', { weekday: 'long', day: 'numeric', month: 'long', year: 'numeric' });
    return libelle.charAt(0).toUpperCase() + libelle.slice(1);
  }

  get moisLabel(): string {
    const libelle = this.mois.toLocaleDateString('fr-FR', { month: 'long', year: 'numeric' });
    return libelle.charAt(0).toUpperCase() + libelle.slice(1);
  }

  /** Grille du mois en cours : cases vides, numéro du jour, marqueur « aujourd'hui » et rendez-vous. */
  get casesCalendrier(): CaseCalendrier[] {
    const premierJour = new Date(this.mois.getFullYear(), this.mois.getMonth(), 1);
    const dernierJour = new Date(this.mois.getFullYear(), this.mois.getMonth() + 1, 0).getDate();
    const decalage = (premierJour.getDay() + 6) % 7;
    const cases: CaseCalendrier[] = [];
    for (let index = 0; index < decalage; index++) cases.push({ jour: null, aujourdHui: false, rendezVous: 0, titre: '' });
    for (let jour = 1; jour <= dernierJour; jour++) {
      const cle = `${this.mois.getFullYear()}-${String(this.mois.getMonth() + 1).padStart(2, '0')}-${String(jour).padStart(2, '0')}`;
      const nombre = this.rendezVousMois.filter((item) => item.dateHeure.slice(0, 10) === cle).length;
      cases.push({
        jour,
        aujourdHui: cle === this.aujourdHui,
        rendezVous: nombre,
        titre: nombre === 1 ? '1 rendez-vous' : `${nombre} rendez-vous`
      });
    }
    return cases;
  }

  get prochains(): RendezVous[] { return this.appointments.slice(0, 5); }
  get rendezVousTermines(): number { return this.appointments.filter((item) => item.statut === 'TERMINE').length; }
  get facturesEnAttente(): number { return this.factures.filter((facture) => Number(facture.resteAPayer) > 0).length; }
  get resteARecouvrir(): number { return this.factures.reduce((total, facture) => total + Number(facture.resteAPayer || 0), 0); }
  get montantEncaisseJour(): number {
    return this.factures
      .filter((facture) => facture.dateFacture === this.aujourdHui)
      .reduce((total, facture) => total + Number(facture.montantPaye || 0), 0);
  }
  get montantEncaisseTotal(): number {
    return this.factures.reduce((total, facture) => total + Number(facture.montantPaye || 0), 0);
  }

  /** Encaissements reels des sept derniers jours (paiements enregistres par jour). */
  get encaissementsSemaine(): EncaissementJour[] {
    const resultats: EncaissementJour[] = [];
    for (let index = 6; index >= 0; index--) {
      const jour = new Date();
      jour.setDate(jour.getDate() - index);
      const cle = jour.toISOString().slice(0, 10);
      const montant = this.factures.reduce((total, facture) =>
        total + facture.paiements
          .filter((paiement) => paiement.datePaiement.slice(0, 10) === cle)
          .reduce((somme, paiement) => somme + Number(paiement.montant || 0), 0), 0);
      resultats.push({ date: cle, libelle: this.jours[jour.getDay()], montant });
    }
    return resultats;
  }

  get maximumEncaissement(): number {
    return Math.max(...this.encaissementsSemaine.map((item) => item.montant), 1);
  }
  barreEncaissement(montant: number): string {
    return `${Math.max(4, Math.round((montant / this.maximumEncaissement) * 100))}%`;
  }

  /** Repartition reelle des encaissements par moyen de paiement. */
  get repartitionPaiements(): RepartitionPaiement[] {
    const totaux = new Map<string, number>();
    this.factures.forEach((facture) => facture.paiements.forEach((paiement) => {
      const moyen = paiement.moyenPaiement || 'AUTRE';
      totaux.set(moyen, (totaux.get(moyen) ?? 0) + Number(paiement.montant || 0));
    }));
    const total = [...totaux.values()].reduce((somme, valeur) => somme + valeur, 0);
    return [...totaux.entries()]
      .map(([moyen, montant], index) => ({
        moyen, montant,
        pourcentage: total ? Math.round((montant / total) * 100) : 0,
        couleur: this.couleursPaiement[index % this.couleursPaiement.length]
      }))
      .sort((a, b) => b.montant - a.montant);
  }

  get donutPaiements(): string {
    const parts = this.repartitionPaiements;
    if (!parts.length) return 'conic-gradient(var(--bg-hover) 0 100%)';
    let cumul = 0;
    const segments = parts.map((part, index) => {
      const debut = cumul;
      cumul += part.pourcentage;
      const fin = index === parts.length - 1 ? 100 : cumul;
      return `${part.couleur} ${debut}% ${fin}%`;
    });
    return `radial-gradient(closest-side, var(--bg-surface) 0 66%, transparent 67%), conic-gradient(${segments.join(', ')})`;
  }

  moyenLabel(moyen: string): string {
    return moyen === 'ESPECES' ? 'Espèces' : moyen === 'CARTE' ? 'Carte bancaire' : moyen === 'VIREMENT' ? 'Virement' : 'Autre';
  }
  format(valeur: number): string { return new Intl.NumberFormat('fr-FR').format(Math.round(valeur)); }
  time(value: string): string { return new Date(value).toLocaleTimeString('fr-FR', { hour: '2-digit', minute: '2-digit' }); }
  statusLabel(value: string): string { return value.replace('_', ' ').toLowerCase().replace(/^\w/, (letter) => letter.toUpperCase()); }
}