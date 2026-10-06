import { Component, inject } from '@angular/core';
import { DatePipe } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { ActeService } from '../../core/actes/acte.service';
import { AuthService } from '../../core/auth/auth.service';
import { messageErreur } from '../../core/http/erreur-api';
import {
  ActeProgramme, ResultatActe, StatutActe, libelleResultat, libelleStatutActe, libelleTypeActe
} from '../../core/models/acte';

/**
 * Actes programmes (chirurgie, traitement, examen...) : le medecin enregistre le resultat et le rapport,
 * l'accueil organise, facture ou annule, la direction et l'administration consultent.
 */
@Component({ selector: 'app-actes', standalone: true, imports: [DatePipe, FormsModule, RouterLink], templateUrl: './actes.component.html', styleUrl: './actes.component.css' })
export class ActesComponent {
  private readonly service = inject(ActeService);
  private readonly role = inject(AuthService).role();
  readonly isMedecin = this.role === 'MEDECIN';
  readonly isAccueil = this.role === 'ACCUEIL';
  readonly libelleTypeActe = libelleTypeActe;
  readonly libelleStatutActe = libelleStatutActe;
  readonly libelleResultat = libelleResultat;

  actes: ActeProgramme[] = [];
  statut: StatutActe | '' = 'PLANIFIE';
  selection: ActeProgramme | null = null;
  panneau: 'aucun' | 'realisation' | 'annulation' = 'aucun';
  resultat: ResultatActe = 'REUSSI';
  compteRendu = '';
  dateRealisation = '';
  motif = '';
  loading = true;
  saving = false;
  message = '';
  error = '';

  constructor() {
    const id = Number(inject(ActivatedRoute).snapshot.queryParamMap.get('id')) || null;
    this.charger(id);
  }

  charger(selectionId: number | null = null): void {
    this.loading = true;
    this.service.list().subscribe({
      next: (items) => {
        this.actes = items;
        const cible = selectionId ?? this.selection?.id;
        const trouve = cible ? items.find((item) => item.id === cible) ?? null : null;
        if (trouve && selectionId) this.statut = '';
        this.selection = trouve;
        this.loading = false;
      },
      error: () => { this.error = 'Les actes programmés sont indisponibles.'; this.loading = false; }
    });
  }

  get filtres(): ActeProgramme[] {
    const liste = this.actes.filter((item) => !this.statut || item.statut === this.statut);
    // A venir : du plus proche au plus lointain ; historique : du plus recent au plus ancien.
    return this.statut === 'PLANIFIE' ? [...liste].sort((a, b) => a.dateHeure.localeCompare(b.dateHeure)) : liste;
  }
  compte(statut: StatutActe): number { return this.actes.filter((item) => item.statut === statut).length; }
  get aFacturer(): number { return this.actes.filter((item) => item.statut !== 'ANNULE' && !item.factureId).length; }

  choisir(acte: ActeProgramme): void {
    this.selection = acte;
    this.panneau = 'aucun';
    this.message = '';
    this.error = '';
  }

  ouvrir(panneau: 'realisation' | 'annulation'): void {
    this.panneau = this.panneau === panneau ? 'aucun' : panneau;
    this.compteRendu = '';
    this.motif = '';
    this.resultat = 'REUSSI';
    this.dateRealisation = '';
  }

  realiser(): void {
    if (!this.selection || !this.compteRendu.trim()) { this.error = 'Le compte-rendu de l’acte est obligatoire.'; return; }
    this.executer(this.service.realiser(this.selection.id, this.resultat, this.compteRendu.trim(), this.dateRealisation || undefined),
      'Acte enregistré comme réalisé. L’accueil peut maintenant le facturer.');
  }

  annuler(): void {
    if (!this.selection || !this.motif.trim()) { this.error = 'Indiquez le motif de l’annulation.'; return; }
    this.executer(this.service.annuler(this.selection.id, this.motif.trim()), 'Acte annulé.');
  }

  private executer(operation: ReturnType<ActeService['annuler']>, succes: string): void {
    this.saving = true;
    this.error = '';
    operation.subscribe({
      next: (acte) => { this.message = succes; this.panneau = 'aucun'; this.saving = false; this.selection = acte; this.charger(acte.id); },
      error: (response) => { this.error = messageErreur(response, 'L’opération a échoué.'); this.saving = false; }
    });
  }

  libelleFacture(acte: ActeProgramme): string {
    if (!acte.factureId) return 'Non facturé';
    const statut = acte.factureStatut === 'PAYEE' ? 'payée' : acte.factureStatut === 'PARTIELLE' ? 'partielle' : 'à payer';
    return `FAC-${String(acte.factureId).padStart(3, '0')} · ${statut}`;
  }
}
