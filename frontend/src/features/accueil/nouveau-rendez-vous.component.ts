import { Component, inject } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { forkJoin } from 'rxjs';
import { messageErreur } from '../../core/http/erreur-api';
import { Patient } from '../../core/models/patient';
import { PatientService } from '../../core/patients/patient.service';
import { Medecin, RendezVousService } from '../../core/rendez-vous/rendez-vous.service';

/** Creation d'un rendez-vous, ou modification / reprogrammation si la route porte un identifiant. */
@Component({ selector: 'app-nouveau-rendez-vous', standalone: true, imports: [ReactiveFormsModule, RouterLink], templateUrl: './nouveau-rendez-vous.component.html', styleUrl: './nouveau-rendez-vous.component.css' })
export class NouveauRendezVousComponent {
  private readonly builder = inject(FormBuilder);
  private readonly patientService = inject(PatientService);
  private readonly service = inject(RendezVousService);
  private readonly router = inject(Router);
  private readonly route = inject(ActivatedRoute);
  private readonly patientPreselectionne = this.route.snapshot.queryParamMap.get('patientId') ?? '';
  readonly rendezVousId = Number(this.route.snapshot.paramMap.get('id')) || null;
  readonly modeModification = this.rendezVousId !== null;
  patients: Patient[] = [];
  doctors: Medecin[] = [];
  loading = true;
  saving = false;
  error = '';
  readonly form = this.builder.nonNullable.group({ patientId: ['', Validators.required], medecinId: ['', Validators.required], date: [new Date().toISOString().slice(0, 10), Validators.required], heure: ['09:00', Validators.required], motif: ['', [Validators.required, Validators.maxLength(500)]] });

  constructor() {
    forkJoin({ patients: this.patientService.list(), doctors: this.service.doctors() }).subscribe({
      next: (data) => {
        this.patients = data.patients;
        this.doctors = data.doctors;
        if (this.rendezVousId) this.chargerRendezVous(this.rendezVousId);
        else { if (this.patientPreselectionne) this.form.patchValue({ patientId: this.patientPreselectionne }); this.loading = false; }
      },
      error: () => { this.error = 'Impossible de charger les patients et les médecins.'; this.loading = false; }
    });
  }

  private chargerRendezVous(id: number): void {
    this.service.get(id).subscribe({
      next: (rendezVous) => {
        if (rendezVous.statut !== 'PLANIFIE' && rendezVous.statut !== 'CONFIRME') {
          this.error = 'Ce rendez-vous ne peut plus être modifié (seuls les rendez-vous planifiés ou confirmés le peuvent).';
          this.form.disable();
        }
        this.form.patchValue({
          patientId: String(rendezVous.patientId),
          medecinId: String(rendezVous.medecinId),
          date: rendezVous.dateHeure.slice(0, 10),
          heure: rendezVous.dateHeure.slice(11, 16),
          motif: rendezVous.motif ?? ''
        });
        this.loading = false;
      },
      error: () => { this.error = 'Rendez-vous introuvable.'; this.loading = false; }
    });
  }

  submit(): void {
    if (this.form.invalid) { this.form.markAllAsTouched(); return; }
    this.saving = true;
    this.error = '';
    const value = this.form.getRawValue();
    const request = { patientId: Number(value.patientId), medecinId: Number(value.medecinId), dateHeure: `${value.date}T${value.heure}`, motif: value.motif };
    const operation = this.rendezVousId ? this.service.update(this.rendezVousId, request) : this.service.create(request);
    operation.subscribe({
      next: (rendezVous) => this.modeModification
        ? this.router.navigate(['/rendez-vous'], { queryParams: { date: rendezVous.dateHeure.slice(0, 10) } })
        : this.router.navigate(['/impression/rendez-vous', rendezVous.id]),
      error: (response) => {
        this.error = response.status === 409 ? messageErreur(response, 'Ce créneau est déjà occupé pour ce médecin.') : messageErreur(response, 'Le rendez-vous n’a pas pu être enregistré.');
        this.saving = false;
      }
    });
  }
}
