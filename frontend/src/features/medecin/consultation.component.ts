import { Component, DestroyRef, inject } from '@angular/core';
import { DatePipe } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { interval } from 'rxjs';
import { ActeService } from '../../core/actes/acte.service';
import { ConsultationService } from '../../core/consultations/consultation.service';
import { messageErreur } from '../../core/http/erreur-api';
import { ActeProgramme, TYPES_ACTE, TypeActe, libelleStatutActe, libelleTypeActe } from '../../core/models/acte';
import { ConsultationDossier } from '../../core/models/consultation';
import { Medicament } from '../../core/models/pharmacie';
import { RendezVous } from '../../core/models/rendez-vous';
import { PharmacieService } from '../../core/pharmacie/pharmacie.service';
import { RendezVousService } from '../../core/rendez-vous/rendez-vous.service';
import { ChoixMedicament, MedicamentRechercheComponent } from '../../shared/medicament-recherche/medicament-recherche.component';

interface LigneOrdonnance { medicamentId: number | null; medicament: string; posologie: string; duree: string; }

/**
 * Consultation cote medecin : demarrer quand le patient arrive, rediger le compte-rendu et terminer,
 * puis rediger l'ordonnance (recherche dans le stock) et programmer un acte (chirurgie, traitement...).
 */
@Component({
  selector: 'app-consultation',
  standalone: true,
  imports: [DatePipe, FormsModule, RouterLink, MedicamentRechercheComponent],
  templateUrl: './consultation.component.html',
  styleUrl: './consultation.component.css'
})
export class ConsultationComponent {
  private readonly route = inject(ActivatedRoute);
  private readonly rendezVousService = inject(RendezVousService);
  private readonly consultationService = inject(ConsultationService);
  private readonly pharmacieService = inject(PharmacieService);
  private readonly acteService = inject(ActeService);
  readonly typesActe = TYPES_ACTE;
  readonly libelleTypeActe = libelleTypeActe;
  readonly libelleStatutActe = libelleStatutActe;

  readonly rendezVousId = Number(this.route.snapshot.paramMap.get('id'));
  rendezVous: RendezVous | null = null;
  consultation: ConsultationDossier | null = null;
  actes: ActeProgramme[] = [];
  medicaments: Medicament[] = [];
  maintenant = Date.now();
  loading = true;
  saving = false;
  error = '';
  message = '';

  compteRendu = '';
  instructions = '';
  lignes: LigneOrdonnance[] = [this.nouvelleLigne()];
  acteOuvert = false;
  acte = { type: 'CHIRURGIE' as TypeActe, intitule: '', details: '', date: '', heure: '09:00', dureeMinutes: 60, lieu: '' };

  constructor() {
    this.pharmacieService.medicaments().subscribe({ next: (items) => this.medicaments = items, error: () => this.medicaments = [] });
    this.charger();
    interval(30000).pipe(takeUntilDestroyed(inject(DestroyRef))).subscribe(() => this.maintenant = Date.now());
  }

  charger(): void {
    this.rendezVousService.get(this.rendezVousId).subscribe({
      next: (rendezVous) => {
        this.rendezVous = rendezVous;
        this.consultationService.dossier(rendezVous.patientId).subscribe({
          next: (dossier) => {
            this.consultation = dossier.consultations.find((item) => item.rendezVousId === rendezVous.id) ?? null;
            this.actes = dossier.actes.filter((item) => item.medecinId === rendezVous.medecinId);
            this.loading = false;
          },
          error: () => { this.consultation = null; this.loading = false; }
        });
      },
      error: (response) => {
        this.error = response.status === 403 ? 'Ce rendez-vous ne concerne pas ce médecin.' : 'Rendez-vous introuvable.';
        this.loading = false;
      }
    });
  }

  /** Etape courante du parcours de consultation. */
  get etape(): 'attente' | 'en-cours' | 'terminee' | 'close' {
    if (!this.rendezVous) return 'close';
    if (this.consultation) return 'terminee';
    if (this.rendezVous.statut === 'PLANIFIE' || this.rendezVous.statut === 'CONFIRME') return 'attente';
    if (this.rendezVous.statut === 'EN_COURS' || this.rendezVous.statut === 'TERMINE') return 'en-cours';
    return 'close';
  }

  get dureeEnCours(): string {
    const debut = this.rendezVous?.debutConsultation ? new Date(this.rendezVous.debutConsultation).getTime() : null;
    if (!debut) return '';
    const minutes = Math.max(0, Math.floor((this.maintenant - debut) / 60000));
    return minutes < 60 ? `${minutes} min` : `${Math.floor(minutes / 60)} h ${String(minutes % 60).padStart(2, '0')}`;
  }

  demarrer(): void {
    this.changerStatut('EN_COURS', 'Consultation démarrée.');
  }

  /** Enregistre le compte-rendu : la consultation est terminee. */
  terminer(): void {
    if (!this.compteRendu.trim()) { this.error = 'Rédigez le compte-rendu avant de terminer la consultation.'; return; }
    this.saving = true;
    this.error = '';
    this.consultationService.creer(this.rendezVousId, { compteRendu: this.compteRendu.trim() }).subscribe({
      next: () => { this.message = 'Consultation terminée. Vous pouvez rédiger l’ordonnance ou programmer un acte.'; this.saving = false; this.charger(); },
      error: (response) => { this.error = messageErreur(response, 'Le compte-rendu n’a pas pu être enregistré.'); this.saving = false; }
    });
  }

  private changerStatut(statut: 'EN_COURS', succes: string): void {
    this.saving = true;
    this.error = '';
    this.rendezVousService.updateStatut(this.rendezVousId, statut).subscribe({
      next: (rendezVous) => { this.rendezVous = rendezVous; this.message = succes; this.saving = false; },
      error: (response) => { this.error = messageErreur(response, 'Le changement de statut a échoué.'); this.saving = false; }
    });
  }

  // --- Ordonnance --------------------------------------------------------------------------------

  private nouvelleLigne(): LigneOrdonnance { return { medicamentId: null, medicament: '', posologie: '', duree: '' }; }
  ajouterLigne(): void { this.lignes.push(this.nouvelleLigne()); }
  supprimerLigne(index: number): void { if (this.lignes.length > 1) this.lignes.splice(index, 1); }
  choisirMedicament(ligne: LigneOrdonnance, choix: ChoixMedicament): void { ligne.medicamentId = choix.medicamentId; ligne.medicament = choix.medicament; }

  enregistrerOrdonnance(): void {
    if (!this.consultation) return;
    const lignes = this.lignes.filter((ligne) => ligne.medicament.trim());
    if (!lignes.length) { this.error = 'Ajoutez au moins un médicament.'; return; }
    this.saving = true;
    this.error = '';
    this.consultationService.ajouterPrescription(this.consultation.id, {
      datePrescription: new Date().toLocaleDateString('sv-SE'),
      instructions: this.instructions.trim() || undefined,
      lignes: lignes.map((ligne) => ({ medicamentId: ligne.medicamentId ?? undefined, medicament: ligne.medicament.trim(), posologie: ligne.posologie, duree: ligne.duree }))
    }).subscribe({
      next: () => { this.message = 'Ordonnance enregistrée : elle apparaît chez le pharmacien et peut être imprimée.'; this.saving = false; this.charger(); },
      error: (response) => { this.error = messageErreur(response, 'L’ordonnance n’a pas pu être enregistrée.'); this.saving = false; }
    });
  }

  // --- Acte programme ------------------------------------------------------------------------------

  ouvrirActe(): void {
    this.acteOuvert = !this.acteOuvert;
    if (!this.acte.date) {
      const demain = new Date(Date.now() + 86400000);
      this.acte.date = demain.toLocaleDateString('sv-SE');
    }
  }

  programmerActe(): void {
    if (!this.rendezVous || !this.acte.intitule.trim() || !this.acte.date || !this.acte.heure) {
      this.error = 'Renseignez le type, l’intitulé, la date et l’heure de l’acte.';
      return;
    }
    this.saving = true;
    this.error = '';
    this.acteService.programmer({
      patientId: this.rendezVous.patientId,
      consultationId: this.consultation?.id ?? null,
      type: this.acte.type,
      intitule: this.acte.intitule.trim(),
      details: this.acte.details.trim() || undefined,
      dateHeure: `${this.acte.date}T${this.acte.heure}`,
      dureeMinutes: Number(this.acte.dureeMinutes),
      lieu: this.acte.lieu.trim() || undefined
    }).subscribe({
      next: (acte) => {
        this.message = `${libelleTypeActe(acte.type)} « ${acte.intitule} » programmé(e) : l’accueil et la direction la voient dans « Actes programmés ».`;
        this.acteOuvert = false;
        this.acte = { type: 'CHIRURGIE', intitule: '', details: '', date: '', heure: '09:00', dureeMinutes: 60, lieu: '' };
        this.saving = false;
        this.charger();
      },
      error: (response) => { this.error = messageErreur(response, 'L’acte n’a pas pu être programmé.'); this.saving = false; }
    });
  }

  statutLabel(statut: string): string {
    return statut.replace('_', ' ').toLowerCase().replace(/^\w/, (lettre) => lettre.toUpperCase());
  }
}
