import { Component, inject } from '@angular/core';
import { DatePipe } from '@angular/common';
import { RouterLink } from '@angular/router';
import { forkJoin } from 'rxjs';
import { AuthService } from '../../core/auth/auth.service';
import { Patient } from '../../core/models/patient';
import { RendezVous } from '../../core/models/rendez-vous';
import { PatientService } from '../../core/patients/patient.service';
import { RendezVousService } from '../../core/rendez-vous/rendez-vous.service';

@Component({
  selector: 'app-medecin-dashboard',
  standalone: true,
  imports: [DatePipe, RouterLink],
  templateUrl: './medecin-dashboard.component.html',
  styleUrl: './medecin-dashboard.component.css'
})
export class MedecinDashboardComponent {
  private readonly rendezVousService = inject(RendezVousService);
  private readonly patientService = inject(PatientService);
  readonly auth = inject(AuthService);
  readonly aujourdHui = new Date().toISOString().slice(0, 10);
  rendezVous: RendezVous[] = [];
  patients: Patient[] = [];
  loading = true;
  error = '';

  constructor() {
    forkJoin({ rendezVous: this.rendezVousService.list(this.aujourdHui), patients: this.patientService.list() }).subscribe({
      next: (data) => { this.rendezVous = data.rendezVous; this.patients = data.patients; },
      error: () => { this.error = 'Le tableau de bord médical est momentanément indisponible.'; },
      complete: () => { this.loading = false; }
    });
  }

  get prochains(): RendezVous[] { return this.rendezVous.filter((item) => item.statut !== 'ANNULE' && item.statut !== 'ABSENT').slice(0, 5); }
  get termines(): number { return this.rendezVous.filter((item) => item.statut === 'TERMINE').length; }
  get enAttente(): number { return this.rendezVous.filter((item) => item.statut === 'PLANIFIE' || item.statut === 'CONFIRME').length; }
  get patientsDuJour(): number { return new Set(this.rendezVous.map((item) => item.patientId)).size; }
  statusLabel(value: string): string { return value.replace('_', ' ').toLowerCase().replace(/^\w/, (letter) => letter.toUpperCase()); }
  time(value: string): string { return new Date(value).toLocaleTimeString('fr-FR', { hour: '2-digit', minute: '2-digit' }); }
}
