import { Component, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { forkJoin } from 'rxjs';
import { Patient } from '../../core/models/patient';
import { Medecin, RendezVousService } from '../../core/rendez-vous/rendez-vous.service';

@Component({ selector: 'app-nouveau-rendez-vous', standalone: true, imports: [ReactiveFormsModule, RouterLink], templateUrl: './nouveau-rendez-vous.component.html', styleUrl: './nouveau-rendez-vous.component.css' })
export class NouveauRendezVousComponent {
  private readonly builder = inject(FormBuilder);
  private readonly http = inject(HttpClient);
  private readonly service = inject(RendezVousService);
  private readonly router = inject(Router);
  patients: Patient[] = [];
  doctors: Medecin[] = [];
  loading = true;
  saving = false;
  error = '';
  readonly form = this.builder.nonNullable.group({ patientId: ['', Validators.required], medecinId: ['', Validators.required], date: [new Date().toISOString().slice(0, 10), Validators.required], heure: ['09:00', Validators.required], motif: ['', [Validators.required, Validators.maxLength(500)]] });
  constructor() { forkJoin({ patients: this.http.get<Patient[]>('http://localhost:8080/api/patients'), doctors: this.service.doctors() }).subscribe({ next: (data) => { this.patients = data.patients; this.doctors = data.doctors; }, error: () => this.error = 'Impossible de charger les patients et les médecins.', complete: () => this.loading = false }); }
  submit(): void { if (this.form.invalid) { this.form.markAllAsTouched(); return; } this.saving = true; const value = this.form.getRawValue(); this.service.create({ patientId: Number(value.patientId), medecinId: Number(value.medecinId), dateHeure: `${value.date}T${value.heure}`, motif: value.motif }).subscribe({ next: () => this.router.navigate(['/rendez-vous']), error: (response) => { this.error = response.status === 409 ? 'Ce créneau est déjà occupé pour ce médecin.' : 'Le rendez-vous n’a pas pu être enregistré.'; this.saving = false; } }); }
}
