import { Component, inject } from '@angular/core';
import { DatePipe } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { messageErreur } from '../../core/http/erreur-api';
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
  readonly isDirection = this.auth.role() === 'DIRECTION' || this.auth.role() === 'ADMIN';
  /** L'accueil confirme, annule ou declare absent ; demarrer et terminer appartiennent au medecin. */
  readonly statutsAccueil: StatutRendezVous[] = ['PLANIFIE', 'CONFIRME', 'ANNULE', 'ABSENT'];
  private readonly router = inject(Router);
  aSupprimer: number | null = null;
  erreurAction = '';
  appointments: RendezVous[] = [];
  doctors: Medecin[] = [];
  date = inject(ActivatedRoute).snapshot.queryParamMap.get('date') ?? new Date().toISOString().slice(0, 10);
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
    this.erreurAction = '';
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
    this.erreurAction = '';
    this.error = '';
    this.service.updateStatut(appointment.id, statut as StatutRendezVous).subscribe({
      next: (updated) => {
        this.appointments = this.appointments.map((item) => item.id === updated.id ? updated : item);
        this.message = `Statut du rendez-vous de ${updated.patientPrenom} ${updated.patientNom} : « ${this.statusLabel(updated.statut)} ».`;
      },
      error: (response) => {
        this.savingId = null;
        this.load();
        this.erreurAction = response.status === 403
          ? 'Ce rendez-vous ne concerne pas ce médecin.'
          : messageErreur(response, 'Le changement de statut a échoué.');
      },
      complete: () => { this.savingId = null; }
    });
  }

  /** Le medecin demarre la consultation puis ouvre l'ecran de compte-rendu. */
  demarrer(appointment: RendezVous): void {
    this.savingId = appointment.id;
    this.error = '';
    this.service.updateStatut(appointment.id, 'EN_COURS').subscribe({
      next: () => this.router.navigate(['/rendez-vous', appointment.id, 'consultation']),
      error: (response) => { this.erreurAction = messageErreur(response, 'La consultation n’a pas pu démarrer.'); this.savingId = null; }
    });
  }

  /** Suppression definitive : libere le creneau du medecin. */
  supprimer(appointment: RendezVous): void {
    this.savingId = appointment.id;
    this.service.delete(appointment.id).subscribe({
      next: () => {
        this.appointments = this.appointments.filter((item) => item.id !== appointment.id);
        this.message = `Rendez-vous de ${appointment.patientPrenom} ${appointment.patientNom} supprimé : le créneau est libéré.`;
        this.aSupprimer = null;
        this.savingId = null;
      },
      error: (response) => { this.erreurAction = messageErreur(response, 'La suppression a échoué.'); this.aSupprimer = null; this.savingId = null; }
    });
  }

  estModifiable(appointment: RendezVous): boolean {
    return appointment.statut === 'PLANIFIE' || appointment.statut === 'CONFIRME';
  }

  /** Un rendez-vous annule ou absent ne peut plus donner lieu a une consultation. */
  estOuvert(appointment: RendezVous): boolean {
    return appointment.statut !== 'ANNULE' && appointment.statut !== 'ABSENT';
  }

  get contextLabel(): string { return this.isDoctor ? 'MÉDECIN / PLANNING' : this.isDirection ? 'DIRECTION / RENDEZ-VOUS' : 'ACCUEIL / RENDEZ-VOUS'; }
  time(value: string): string { return new Date(value).toLocaleTimeString('fr-FR', { hour: '2-digit', minute: '2-digit' }); }
  statusLabel(value: string): string { return value.replace('_', ' ').toLowerCase().replace(/^\w/, (letter) => letter.toUpperCase()); }
}