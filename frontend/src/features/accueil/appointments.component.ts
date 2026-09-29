import { Component, inject } from '@angular/core';
import { DatePipe } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { AuthService } from '../../core/auth/auth.service';
import { RendezVous, StatutRendezVous } from '../../core/models/rendez-vous';
import { Medecin, RendezVousService } from '../../core/rendez-vous/rendez-vous.service';

@Component({ selector: 'app-appointments', standalone: true, imports: [DatePipe, FormsModule, RouterLink], templateUrl: './appointments.component.html', styleUrl: './appointments.component.css' })
export class AppointmentsComponent {
  private readonly service = inject(RendezVousService);
  private readonly auth = inject(AuthService);
  readonly statuses: StatutRendezVous[] = ['PLANIFIE', 'CONFIRME', 'EN_COURS', 'TERMINE', 'ANNULE', 'ABSENT'];
  readonly isDoctor = this.auth.role() === 'MEDECIN';
  readonly isAccueil = this.auth.role() === 'ACCUEIL';
  appointments: RendezVous[] = [];
  doctors: Medecin[] = [];
  date = new Date().toISOString().slice(0, 10);
  status = '';
  medecinId: number | '' = '';
  loading = false;
  savingId: number | null = null;
  message = '';
  error = '';

  constructor() {
    this.load();
    if (this.isAccueil) this.service.doctors().subscribe({ next: (items) => this.doctors = items });
  }
  load(): void {
    this.loading = true;
    this.error = '';
    this.message = '';
    this.service.list(this.date, this.medecinId === '' ? undefined : Number(this.medecinId), this.status as StatutRendezVous || undefined).subscribe({
      next: (items) => this.appointments = items,
      error: () => { this.error = 'Impossible de charger le planning. Vérifiez que le serveur est démarré, puis réessayez.'; this.loading = false; },
      complete: () => this.loading = false
    });
  }
  /** Met a jour le statut du rendez-vous (ACCUEIL et MEDECIN proprietaire). */
  changerStatut(appointment: RendezVous, statut: string): void {
    if (!statut || statut === appointment.statut) return;
    this.savingId = appointment.id;
    this.message = '';
    this.error = '';
    this.service.updateStatut(appointment.id, statut as StatutRendezVous).subscribe({
      next: (updated) => {
        this.appointments = this.appointments.map((item) => item.id === updated.id ? updated : item);
        this.message = `Statut du rendez-vous de ${updated.patientPrenom} ${updated.patientNom} : « ${this.statusLabel(updated.statut)} ».`;
      },
      error: (response) => {
        this.error = response.status === 403
          ? 'Ce rendez-vous ne concerne pas ce médecin.'
          : 'Le changement de statut a échoué.';
      },
      complete: () => { this.savingId = null; }
    });
  }

  /** Un rendez-vous annule ou absent ne peut plus donner lieu a une consultation. */
  estOuvert(appointment: RendezVous): boolean {
    return appointment.statut !== 'ANNULE' && appointment.statut !== 'ABSENT';
  }

  get contextLabel(): string { return this.isDoctor ? 'MÉDECIN / PLANNING' : 'ACCUEIL / RENDEZ-VOUS'; }
  time(value: string): string { return new Date(value).toLocaleTimeString('fr-FR', { hour: '2-digit', minute: '2-digit' }); }
  statusLabel(value: string): string { return value.replace('_', ' ').toLowerCase().replace(/^\w/, (letter) => letter.toUpperCase()); }
}