import { Component, inject } from '@angular/core';
import { FormsModule, FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Patient, PatientRequest } from '../../core/models/patient';
import { PatientService } from '../../core/patients/patient.service';

@Component({ selector: 'app-patients', standalone: true, imports: [FormsModule, ReactiveFormsModule], templateUrl: './patients.component.html', styleUrl: './patients.component.css' })
export class PatientsComponent {
  private readonly service = inject(PatientService);
  private readonly formBuilder = inject(FormBuilder);
  readonly form = this.formBuilder.nonNullable.group({ nom: ['', Validators.required], prenom: ['', Validators.required], dateNaissance: ['', Validators.required], telephone: [''], email: ['', Validators.email], adresse: [''] });
  patients: Patient[] = [];
  query = '';
  loading = false;
  message = '';
  editingId: number | null = null;
  constructor() { this.load(); }
  load(): void { this.loading = true; this.service.list(this.query).subscribe({ next: (items) => this.patients = items, error: () => this.message = 'Impossible de charger les patients.', complete: () => this.loading = false }); }
  save(): void {
    if (this.form.invalid) { this.form.markAllAsTouched(); return; }
    const request = this.form.getRawValue() as PatientRequest;
    const operation = this.editingId === null ? this.service.create(request) : this.service.update(this.editingId, request);
    operation.subscribe({ next: (patient) => { this.patients = this.editingId === null ? [patient, ...this.patients] : this.patients.map((item) => item.id === patient.id ? patient : item); this.cancelEdit(); this.message = 'Fiche patient enregistree.'; }, error: () => this.message = 'L enregistrement a echoue.' });
  }
  edit(patient: Patient): void { this.editingId = patient.id; this.form.patchValue({ nom: patient.nom, prenom: patient.prenom, dateNaissance: patient.dateNaissance, telephone: patient.telephone ?? '', email: patient.email ?? '', adresse: patient.adresse ?? '' }); }
  cancelEdit(): void { this.editingId = null; this.form.reset(); }
  remove(patient: Patient): void { if (!confirm(`Supprimer la fiche de ${patient.prenom} ${patient.nom} ?`)) return; this.service.delete(patient.id).subscribe({ next: () => { this.patients = this.patients.filter((item) => item.id !== patient.id); this.message = 'Fiche patient supprimee.'; }, error: () => this.message = 'La suppression a echoue.' }); }
}