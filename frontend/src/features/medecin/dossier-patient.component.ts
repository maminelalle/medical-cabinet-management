import { Component, inject } from '@angular/core';
import { DatePipe } from '@angular/common';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { ConsultationDossier, DossierPatient } from '../../core/models/consultation';
import { ConsultationService } from '../../core/consultations/consultation.service';

@Component({
  selector: 'app-dossier-patient',
  standalone: true,
  imports: [DatePipe, RouterLink],
  templateUrl: './dossier-patient.component.html',
  styleUrl: './dossier-patient.component.css'
})
export class DossierPatientComponent {
  private readonly route = inject(ActivatedRoute);
  private readonly service = inject(ConsultationService);

  readonly patientId = Number(this.route.snapshot.paramMap.get('patientId'));
  dossier: DossierPatient | null = null;
  loading = true;
  error = '';

  constructor() { this.charger(); }

  charger(): void {
    this.loading = true;
    this.error = '';
    this.service.dossier(this.patientId).subscribe({
      next: (dossier) => { this.dossier = dossier; },
      error: (response) => {
        this.error = response.status === 403
          ? 'Ce dossier médical ne fait pas partie de vos patients.'
          : 'Le dossier médical est momentanément indisponible.';
        this.loading = false;
      },
      complete: () => { this.loading = false; }
    });
  }

  get age(): number | null {
    const naissance = this.dossier?.patient.dateNaissance;
    if (!naissance) return null;
    return Math.floor((Date.now() - new Date(naissance).getTime()) / 31557600000);
  }

  get derniereConsultation(): ConsultationDossier | null {
    return this.dossier?.consultations.length ? this.dossier.consultations[0] : null;
  }

  get prochainRendezVous(): number {
    return this.dossier?.prochainsRendezVous.length ?? 0;
  }

  get nombrePrescriptions(): number {
    return this.dossier?.consultations.filter((consultation) => consultation.prescription !== null).length ?? 0;
  }

  statutLabel(statut: string): string {
    return statut.replace('_', ' ').toLowerCase().replace(/^\w/, (lettre) => lettre.toUpperCase());
  }
}