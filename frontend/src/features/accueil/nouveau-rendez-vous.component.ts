import { Component, inject } from '@angular/core';
import { DatePipe } from '@angular/common';
import { FormBuilder, FormsModule, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { forkJoin } from 'rxjs';
import { messageErreur } from '../../core/http/erreur-api';
import { Patient } from '../../core/models/patient';
import { Creneau, RendezVous, RendezVousRequest } from '../../core/models/rendez-vous';
import { PatientService } from '../../core/patients/patient.service';
import { Medecin, RendezVousService } from '../../core/rendez-vous/rendez-vous.service';

/**
 * Prise de rendez-vous par l'accueil : patient existant (recherche) ou nouveau patient cree en meme temps,
 * choix d'un creneau libre du medecin, puis acces direct a la facture et au ticket.
 * Avec un identifiant dans la route, l'ecran modifie / reprogramme le rendez-vous.
 */
@Component({ selector: 'app-nouveau-rendez-vous', standalone: true, imports: [DatePipe, FormsModule, ReactiveFormsModule, RouterLink], templateUrl: './nouveau-rendez-vous.component.html', styleUrl: './nouveau-rendez-vous.component.css' })
export class NouveauRendezVousComponent {
  private readonly builder = inject(FormBuilder);
  private readonly patientService = inject(PatientService);
  private readonly service = inject(RendezVousService);
  private readonly router = inject(Router);
  private readonly route = inject(ActivatedRoute);
  readonly rendezVousId = Number(this.route.snapshot.paramMap.get('id')) || null;
  readonly modeModification = this.rendezVousId !== null;
  readonly durees = [15, 30, 45, 60, 90];

  patients: Patient[] = [];
  doctors: Medecin[] = [];
  creneaux: Creneau[] = [];
  modePatient: 'existant' | 'nouveau' = 'existant';
  recherche = '';
  patientChoisi: Patient | null = null;
  cree: RendezVous | null = null;
  loading = true;
  chargementCreneaux = false;
  saving = false;
  error = '';

  readonly form = this.builder.nonNullable.group({
    medecinId: ['', Validators.required],
    date: [new Date().toISOString().slice(0, 10), Validators.required],
    heure: ['', Validators.required],
    dureeMinutes: [30, Validators.required],
    motif: ['', [Validators.required, Validators.maxLength(500)]]
  });
  readonly nouveauPatient = this.builder.nonNullable.group({
    nom: ['', Validators.required],
    prenom: ['', Validators.required],
    dateNaissance: ['', Validators.required],
    telephone: ['', Validators.required],
    email: ['', Validators.email],
    adresse: ['']
  });

  constructor() {
    forkJoin({ patients: this.patientService.list(), doctors: this.service.doctors() }).subscribe({
      next: (data) => {
        this.patients = data.patients;
        this.doctors = data.doctors;
        const preselection = Number(this.route.snapshot.queryParamMap.get('patientId'));
        if (preselection) this.patientChoisi = this.patients.find((patient) => patient.id === preselection) ?? null;
        if (this.rendezVousId) this.chargerRendezVous(this.rendezVousId); else this.loading = false;
      },
      error: () => { this.error = 'Impossible de charger les patients et les médecins.'; this.loading = false; }
    });
  }

  /** Recherche par nom, prenom ou telephone (10 resultats au plus). */
  get resultats(): Patient[] {
    const terme = this.recherche.trim().toLowerCase();
    if (terme.length < 2) return [];
    return this.patients.filter((patient) =>
      `${patient.prenom} ${patient.nom} ${patient.nom} ${patient.prenom} ${patient.telephone ?? ''}`.toLowerCase().includes(terme)).slice(0, 10);
  }

  choisirPatient(patient: Patient): void { this.patientChoisi = patient; this.recherche = ''; }
  changerPatient(): void { this.patientChoisi = null; }
  basculer(mode: 'existant' | 'nouveau'): void {
    this.modePatient = mode;
    if (mode === 'nouveau' && this.recherche.trim()) {
      // Reprend le texte recherche comme nom du nouveau patient.
      const [premier, ...reste] = this.recherche.trim().split(/\s+/);
      this.nouveauPatient.patchValue({ prenom: premier, nom: reste.join(' ') });
    }
  }

  /** Recharge la grille des creneaux du medecin pour le jour et la duree choisis. */
  chargerCreneaux(): void {
    const { medecinId, date, dureeMinutes } = this.form.getRawValue();
    if (!medecinId || !date) { this.creneaux = []; return; }
    this.chargementCreneaux = true;
    this.service.creneaux(Number(medecinId), date, Number(dureeMinutes)).subscribe({
      next: (items) => { this.creneaux = items; this.chargementCreneaux = false; },
      error: () => { this.creneaux = []; this.chargementCreneaux = false; }
    });
  }

  choisirCreneau(creneau: Creneau): void {
    if (!creneau.libre) return;
    this.form.patchValue({ heure: creneau.debut });
  }

  get creneauxLibres(): number { return this.creneaux.filter((creneau) => creneau.libre).length; }

  private chargerRendezVous(id: number): void {
    this.service.get(id).subscribe({
      next: (rendezVous) => {
        if (rendezVous.statut !== 'PLANIFIE' && rendezVous.statut !== 'CONFIRME') {
          this.error = 'Ce rendez-vous ne peut plus être modifié (seuls les rendez-vous planifiés ou confirmés le peuvent).';
          this.form.disable();
        }
        this.patientChoisi = this.patients.find((patient) => patient.id === rendezVous.patientId) ?? null;
        this.form.patchValue({
          medecinId: String(rendezVous.medecinId),
          date: rendezVous.dateHeure.slice(0, 10),
          heure: rendezVous.dateHeure.slice(11, 16),
          dureeMinutes: rendezVous.dureeMinutes ?? 30,
          motif: rendezVous.motif ?? ''
        });
        this.loading = false;
        this.chargerCreneaux();
      },
      error: () => { this.error = 'Rendez-vous introuvable.'; this.loading = false; }
    });
  }

  submit(): void {
    this.error = '';
    if (this.modePatient === 'existant' && !this.patientChoisi) { this.error = 'Recherchez et sélectionnez le patient, ou créez un nouveau patient.'; return; }
    if (this.modePatient === 'nouveau' && this.nouveauPatient.invalid) { this.nouveauPatient.markAllAsTouched(); this.error = 'Complétez la fiche du nouveau patient (nom, prénom, date de naissance, téléphone).'; return; }
    if (this.form.invalid) { this.form.markAllAsTouched(); this.error = this.form.controls.heure.invalid ? 'Choisissez un créneau libre.' : 'Complétez le médecin, la date et le motif.'; return; }
    this.saving = true;
    const valeur = this.form.getRawValue();
    const request: RendezVousRequest = {
      medecinId: Number(valeur.medecinId),
      dateHeure: `${valeur.date}T${valeur.heure}`,
      dureeMinutes: Number(valeur.dureeMinutes),
      motif: valeur.motif,
      ...(this.modePatient === 'nouveau' && !this.modeModification
        ? { nouveauPatient: this.nouveauPatient.getRawValue() }
        : { patientId: this.patientChoisi!.id })
    };
    const operation = this.rendezVousId ? this.service.update(this.rendezVousId, request) : this.service.create(request);
    operation.subscribe({
      next: (rendezVous) => {
        this.saving = false;
        if (this.modeModification) {
          this.router.navigate(['/rendez-vous'], { queryParams: { date: rendezVous.dateHeure.slice(0, 10) } });
          return;
        }
        this.cree = rendezVous;
      },
      error: (response) => {
        this.error = messageErreur(response, 'Le rendez-vous n’a pas pu être enregistré.');
        this.saving = false;
        if (response.status === 409) this.chargerCreneaux();
      }
    });
  }

  /** Nouveau rendez-vous apres une creation reussie. */
  recommencer(): void {
    this.cree = null;
    this.patientChoisi = null;
    this.modePatient = 'existant';
    this.nouveauPatient.reset();
    this.form.patchValue({ heure: '', motif: '' });
    this.patientService.list().subscribe({ next: (items) => this.patients = items });
    this.chargerCreneaux();
  }
}
