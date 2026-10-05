import { Component, inject } from '@angular/core';
import { DatePipe } from '@angular/common';
import { FormArray, FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { ConsultationDossier } from '../../core/models/consultation';
import { RendezVous } from '../../core/models/rendez-vous';
import { ConsultationService } from '../../core/consultations/consultation.service';
import { RendezVousService } from '../../core/rendez-vous/rendez-vous.service';
import { PharmacieService } from '../../core/pharmacie/pharmacie.service';
import { Medicament } from '../../core/models/pharmacie';

@Component({
  selector: 'app-consultation',
  standalone: true,
  imports: [DatePipe, ReactiveFormsModule, RouterLink],
  templateUrl: './consultation.component.html',
  styleUrl: './consultation.component.css'
})
export class ConsultationComponent {
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly builder = inject(FormBuilder);
  private readonly rendezVousService = inject(RendezVousService);
  private readonly consultationService = inject(ConsultationService);
  private readonly pharmacieService = inject(PharmacieService);

  readonly rendezVousId = Number(this.route.snapshot.paramMap.get('id'));
  rendezVous: RendezVous | null = null;
  consultation: ConsultationDossier | null = null;
  loading = true;
  saving = false;
  error = '';
  message = '';
  medicaments: Medicament[] = [];

  readonly form = this.builder.nonNullable.group({
    compteRendu: ['', [Validators.required, Validators.maxLength(10000)]],
    instructions: [''],
    lignes: this.builder.array([this.nouvelleLigne()])
  });

  constructor() { this.chargerMedicaments(); this.charger(); }

  get lignes(): FormArray { return this.form.controls.lignes as FormArray; }

  private nouvelleLigne() {
    return this.builder.nonNullable.group({ medicamentId: [0, [Validators.required, Validators.min(1)]], medicament: [''], posologie: [''], duree: [''] });
  }
  ajouterLigne(): void { this.lignes.push(this.nouvelleLigne()); }
  supprimerLigne(index: number): void { if (this.lignes.length > 1) this.lignes.removeAt(index); }

  private chargerMedicaments(): void {
    this.pharmacieService.medicaments().subscribe({
      next: (items) => { this.medicaments = items; },
      error: () => { this.error = 'Le catalogue des médicaments est indisponible.'; }
    });
  }

  charger(): void {
    this.loading = true;
    this.rendezVousService.get(this.rendezVousId).subscribe({
      next: (rendezVous) => {
        this.rendezVous = rendezVous;
        this.consultationService.dossier(rendezVous.patientId).subscribe({
          next: (dossier) => {
            this.consultation = dossier.consultations.find((item) => item.rendezVousId === rendezVous.id) ?? null;
          },
          error: () => { this.consultation = null; },
          complete: () => { this.loading = false; }
        });
      },
      error: (response) => {
        this.error = response.status === 403 ? 'Ce rendez-vous ne concerne pas ce médecin.' : 'Rendez-vous introuvable.';
        this.loading = false;
      }
    });
  }

  /** Cree le compte-rendu, puis l'ordonnance si au moins un medicament a ete saisi. */
  enregistrerConsultation(): void {
    if (this.form.controls.compteRendu.invalid) { this.form.controls.compteRendu.markAsTouched(); return; }
    this.saving = true;
    this.error = '';
    this.consultationService.creer(this.rendezVousId, { compteRendu: this.form.controls.compteRendu.getRawValue() }).subscribe({
      next: (consultation) => this.enregistrerOrdonnance(consultation.id, true),
      error: (response) => {
        this.error = response.status === 409
          ? 'Une consultation a déjà été saisie pour ce rendez-vous.'
          : 'Le compte-rendu n’a pas pu être enregistré.';
        this.saving = false;
      }
    });
  }

  /** Ajoute une ordonnance a une consultation existante. */
  enregistrerOrdonnanceSeule(): void {
    if (!this.consultation) return;
    this.saving = true;
    this.error = '';
    this.enregistrerOrdonnance(this.consultation.id, false);
  }

  private enregistrerOrdonnance(consultationId: number, apresCompteRendu: boolean): void {
    const lignes = this.lignes.getRawValue().filter((ligne) => ligne.medicamentId > 0).map((ligne) => {
      const medicament = this.medicaments.find((item) => item.id === ligne.medicamentId);
      return { ...ligne, medicament: medicament ? `${medicament.nom}${medicament.dosage ? ` ${medicament.dosage}` : ''}` : ligne.medicament };
    });
    if (!lignes.length) { this.terminer(apresCompteRendu ? 'Consultation enregistrée.' : 'Aucun médicament saisi.'); return; }
    this.consultationService.ajouterPrescription(consultationId, {
      datePrescription: new Date().toISOString().slice(0, 10),
      instructions: this.form.controls.instructions.getRawValue(),
      lignes
    }).subscribe({
      next: (prescription) => this.terminer(apresCompteRendu ? 'Consultation et ordonnance enregistrées.' : 'Ordonnance enregistrée.', prescription.id),
      error: () => {
        this.error = apresCompteRendu
          ? 'Le compte-rendu est enregistré, mais l’ordonnance a échoué.'
          : 'L’ordonnance n’a pas pu être enregistrée.';
        this.saving = false;
      }
    });
  }

  private terminer(texte: string, ordonnanceId?: number): void {
    this.message = texte;
    this.saving = false;
    const patientId = this.rendezVous?.patientId;
    setTimeout(() => ordonnanceId ? this.router.navigate(['/impression/ordonnance', ordonnanceId]) : patientId ? this.router.navigate(['/dossier', patientId]) : this.router.navigate(['/rendez-vous']), 900);
  }

  statutLabel(statut: string): string {
    return statut.replace('_', ' ').toLowerCase().replace(/^\w/, (lettre) => lettre.toUpperCase());
  }
}