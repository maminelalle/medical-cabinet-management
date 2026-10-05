import { Component, inject } from '@angular/core';
import { FormsModule, FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { AuthService } from '../../core/auth/auth.service';
import { Patient, PatientRequest } from '../../core/models/patient';
import { PatientService } from '../../core/patients/patient.service';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { lireFichierDossier } from '../../core/patients/dossier-fichier';
import { messageErreur } from '../../core/http/erreur-api';

@Component({ selector: 'app-patients', standalone: true, imports: [FormsModule, ReactiveFormsModule, RouterLink], templateUrl: './patients.component.html', styleUrl: './patients.component.css' })
export class PatientsComponent {
  private readonly service = inject(PatientService);
  private readonly auth = inject(AuthService);
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly formBuilder = inject(FormBuilder);
  readonly isMedecin = this.auth.role() === 'MEDECIN';
  readonly isAccueil = this.auth.role() === 'ACCUEIL';
  readonly isDirection = this.auth.role() === 'DIRECTION';
  readonly form = this.formBuilder.nonNullable.group({ nom: ['', Validators.required], prenom: ['', Validators.required], dateNaissance: ['', Validators.required], telephone: [''], email: ['', Validators.email], adresse: [''] });
  patients: Patient[] = [];
  query = '';
  loading = false;
  message = '';
  editingId: number | null = null;
  formOpen = false;
  importEnCours = false;
  constructor() {
    this.query = this.route.snapshot.queryParamMap.get('q') ?? '';
    if (this.isAccueil && this.route.snapshot.queryParamMap.has('nouveau')) this.startCreate();
    this.load();
  }
  load(): void { this.loading = true; this.service.list(this.query).subscribe({ next: (items) => this.patients = items, error: () => this.message = 'Impossible de charger les patients.', complete: () => this.loading = false }); }
  save(): void {
    if (this.form.invalid) { this.form.markAllAsTouched(); return; }
    const request = this.form.getRawValue() as PatientRequest;
    const operation = this.editingId === null ? this.service.create(request) : this.service.update(this.editingId, request);
    operation.subscribe({ next: (patient) => { this.patients = this.editingId === null ? [patient, ...this.patients] : this.patients.map((item) => item.id === patient.id ? patient : item); this.cancelEdit(); this.message = 'Fiche patient enregistrée.'; }, error: (response) => this.message = messageErreur(response, 'L’enregistrement a échoué.') });
  }
  startCreate(): void { this.editingId = null; this.form.reset(); this.formOpen = true; }
  edit(patient: Patient): void { this.editingId = patient.id; this.formOpen = true; this.form.patchValue({ nom: patient.nom, prenom: patient.prenom, dateNaissance: patient.dateNaissance, telephone: patient.telephone ?? '', email: patient.email ?? '', adresse: patient.adresse ?? '' }); }
  cancelEdit(): void { this.editingId = null; this.formOpen = false; this.form.reset(); }
  /** Importe un dossier patient exporte (JSON) puis ouvre le dossier obtenu. */
  async importerDossier(evenement: Event): Promise<void> {
    const champ = evenement.target as HTMLInputElement;
    const fichier = champ.files?.[0];
    champ.value = '';
    if (!fichier) return;
    try {
      const contenu = await lireFichierDossier(fichier);
      this.importEnCours = true;
      this.message = '';
      this.service.importerDossier(contenu).subscribe({
        next: (bilan) => { this.importEnCours = false; this.router.navigate(['/dossier', bilan.patientId]); },
        error: () => { this.importEnCours = false; this.message = 'L’import du dossier a échoué. Vérifiez le contenu du fichier.'; }
      });
    } catch (erreur) {
      this.message = (erreur as Error).message;
    }
  }
  remove(patient: Patient): void { if (!confirm(`Supprimer la fiche de ${patient.prenom} ${patient.nom} ?`)) return; this.service.delete(patient.id).subscribe({ next: () => { this.patients = this.patients.filter((item) => item.id !== patient.id); this.message = 'Fiche patient supprimée.'; }, error: (response) => this.message = messageErreur(response, 'La suppression a échoué.') }); }
}