import { Component, inject } from '@angular/core';
import { DecimalPipe } from '@angular/common';
import { HttpClient } from '@angular/common/http';
import { FormArray, FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { forkJoin } from 'rxjs';
import { Patient } from '../../core/models/patient';
import { FactureService, FactureCreateRequest } from '../../core/factures/facture.service';

interface CatalogueActe { id: number; libelle: string; type: string; montantDefaut: number; }

@Component({ selector: 'app-nouvelle-facture', standalone: true, imports: [DecimalPipe, ReactiveFormsModule, RouterLink], templateUrl: './nouvelle-facture.component.html', styleUrl: './nouvelle-facture.component.css' })
export class NouvelleFactureComponent {
  private readonly builder = inject(FormBuilder);
  private readonly http = inject(HttpClient);
  private readonly factureService = inject(FactureService);
  private readonly router = inject(Router);
  patients: Patient[] = [];
  acts: CatalogueActe[] = [];
  loading = true;
  saving = false;
  error = '';
  readonly form = this.builder.group({
    patientId: ['', Validators.required],
    dateFacture: [new Date().toISOString().slice(0, 10), Validators.required],
    lignes: this.builder.array([this.newLine()])
  });

  constructor() {
    forkJoin({ patients: this.http.get<Patient[]>('http://localhost:8080/api/patients'), acts: this.http.get<CatalogueActe[]>('http://localhost:8080/api/actes-catalogue') }).subscribe({ next: (data) => { this.patients = data.patients; this.acts = data.acts; }, error: () => this.error = 'Impossible de charger les patients et le catalogue des actes.', complete: () => this.loading = false });
  }
  get lines(): FormArray { return this.form.controls.lignes; }
  get total(): number { return this.lines.controls.reduce((sum, line) => sum + Number(line.get('montant')?.value || 0), 0); }
  newLine() { return this.builder.group({ catalogueActeId: [''], libelle: ['', Validators.required], typeActe: ['', Validators.required], montant: [0, [Validators.required, Validators.min(0)]] }); }
  addLine(): void { this.lines.push(this.newLine()); }
  removeLine(index: number): void { if (this.lines.length > 1) this.lines.removeAt(index); }
  chooseAct(index: number): void { const line = this.lines.at(index); const act = this.acts.find((item) => item.id === Number(line.get('catalogueActeId')?.value)); if (act) line.patchValue({ libelle: act.libelle, typeActe: act.type, montant: act.montantDefaut }); }
  submit(): void { if (this.form.invalid) { this.form.markAllAsTouched(); return; } this.saving = true; this.error = ''; const value = this.form.getRawValue(); const request: FactureCreateRequest = { patientId: Number(value.patientId), dateFacture: value.dateFacture!, lignes: value.lignes.map((line) => ({ catalogueActeId: line.catalogueActeId ? Number(line.catalogueActeId) : undefined, libelle: line.libelle!, typeActe: line.typeActe!, montant: Number(line.montant) })) }; this.factureService.create(request).subscribe({ next: () => this.router.navigate(['/factures']), error: () => { this.error = 'La facture n’a pas pu être créée.'; this.saving = false; } }); }
}
