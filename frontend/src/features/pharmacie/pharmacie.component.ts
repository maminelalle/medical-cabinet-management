import { Component, inject } from '@angular/core';
import { DatePipe } from '@angular/common';
import { RouterLink } from '@angular/router';
import { FormsModule, ReactiveFormsModule, FormBuilder, Validators } from '@angular/forms';
import { AuthService } from '../../core/auth/auth.service';
import { PharmacieService } from '../../core/pharmacie/pharmacie.service';
import { Medicament, PrescriptionPharmacie } from '../../core/models/pharmacie';
import { MedicamentRequest } from '../../core/models/pharmacie';
import { Facture } from '../../core/models/facture';
import { FactureService } from '../../core/factures/facture.service';
import * as XLSX from 'xlsx';

interface LigneDispensation { medicamentId: number; quantite: number; libelle: string; }

@Component({
  selector: 'app-pharmacie',
  standalone: true,
  imports: [DatePipe, FormsModule, ReactiveFormsModule, RouterLink],
  templateUrl: './pharmacie.component.html',
  styleUrl: './pharmacie.component.css'
})
export class PharmacieComponent {
  private readonly service = inject(PharmacieService);
  private readonly auth = inject(AuthService);
  private readonly builder = inject(FormBuilder);
  private readonly factureService = inject(FactureService);

  readonly peutAdministrer = this.auth.role() === 'PHARMACIEN' || this.auth.role() === 'DIRECTION';
  readonly form = this.builder.nonNullable.group({
    nom: ['', [Validators.required, Validators.maxLength(150)]],
    dosage: [''], forme: [''], stockActuel: [0, [Validators.required, Validators.min(0)]],
    seuilAlerte: [10, [Validators.required, Validators.min(0)]], prixAchat: [0, [Validators.required, Validators.min(0)]],
    prixVente: [0, [Validators.required, Validators.min(0)]], fournisseur: [''], dateExpiration: ['']
  });
  medicaments: Medicament[] = [];
  prescriptions: PrescriptionPharmacie[] = [];
  selectedPrescription: PrescriptionPharmacie | null = null;
  lignes: LigneDispensation[] = [];
  loading = true;
  saving = false;
  message = '';
  error = '';
  facture: Facture | null = null;

  constructor() { this.charger(); }

  charger(): void {
    this.loading = true;
    this.error = '';
    this.service.medicaments().subscribe({ next: (items) => this.medicaments = items, error: () => this.error = 'Le stock est indisponible.', complete: () => this.loading = false });
    this.service.prescriptions().subscribe({ next: (items) => this.prescriptions = items, error: () => this.error = 'Les prescriptions sont indisponibles.' });
  }

  creerMedicament(): void {
    if (this.form.invalid) { this.form.markAllAsTouched(); return; }
    this.saving = true;
    this.service.creerMedicament(this.form.getRawValue()).subscribe({
      next: (item) => { this.medicaments = [...this.medicaments, item].sort((a, b) => a.nom.localeCompare(b.nom)); this.form.reset({ nom: '', dosage: '', forme: '', stockActuel: 0, seuilAlerte: 10, prixAchat: 0, prixVente: 0, fournisseur: '', dateExpiration: '' }); this.message = `${item.nom} a été ajouté au stock.`; },
      error: () => this.error = 'La création du médicament a échoué.', complete: () => this.saving = false
    });
  }

  importerExcel(event: Event): void {
    const input = event.target as HTMLInputElement;
    const fichier = input.files?.[0];
    if (!fichier) return;
    const lecteur = new FileReader();
    lecteur.onload = () => {
      try {
        const classeur = XLSX.read(lecteur.result, { type: 'array' });
        const lignes = XLSX.utils.sheet_to_json<Record<string, unknown>>(classeur.Sheets[classeur.SheetNames[0]], { defval: '' });
        const demandes = lignes.map((ligne) => this.ligneExcel(ligne)).filter((ligne): ligne is MedicamentRequest => ligne !== null);
        if (!demandes.length) { this.error = 'Aucune ligne médicament valide dans le fichier Excel.'; return; }
        this.saving = true;
        this.service.importerMedicaments(demandes).subscribe({
          next: (items) => { this.message = `${items.length} médicament(s) importé(s).`; this.charger(); },
          error: () => this.error = 'L’import Excel a échoué.',
          complete: () => this.saving = false
        });
      } catch { this.error = 'Le fichier Excel est illisible.'; }
    };
    lecteur.readAsArrayBuffer(fichier);
    input.value = '';
  }

  private ligneExcel(ligne: Record<string, unknown>): MedicamentRequest | null {
    const valeur = (...cles: string[]): unknown => {
      const cle = Object.keys(ligne).find((item) => cles.includes(this.normaliser(item)));
      return cle ? ligne[cle] : '';
    };
    const nom = String(valeur('nom', 'medicament')).trim();
    if (!nom) return null;
    const nombre = (val: unknown): number => Number(String(val).replace(',', '.')) || 0;
    return { nom, dosage: String(valeur('dosage') || ''), forme: String(valeur('forme') || ''), stockActuel: nombre(valeur('stockactuel', 'stock')),
      seuilAlerte: nombre(valeur('seuilalerte', 'seuil')), prixAchat: nombre(valeur('prixachat', 'achat')), prixVente: nombre(valeur('prixvente', 'vente', 'prix')),
      fournisseur: String(valeur('fournisseur') || ''), dateExpiration: String(valeur('dateexpiration', 'expiration') || '') };
  }

  private normaliser(value: string): string { return value.toLowerCase().normalize('NFD').replace(/[\u0300-\u036f]/g, '').replace(/[^a-z0-9]/g, ''); }

  mettreAJourStock(item: Medicament): void {
    this.service.modifierStock(item.id, item.stockActuel).subscribe({ next: (updated) => { item.stockActuel = updated.stockActuel; this.message = `Stock de ${item.nom} mis à jour.`; }, error: () => this.error = 'La mise à jour du stock a échoué.' });
  }

  preparer(prescription: PrescriptionPharmacie): void {
    this.selectedPrescription = prescription;
    this.lignes = prescription.lignes.map((ligne) => {
      const medicament = this.medicaments.find((item) => item.id === ligne.medicamentId)
        ?? this.medicaments.find((item) => ligne.medicament.toLowerCase().includes(item.nom.toLowerCase()));
      return { medicamentId: medicament?.id ?? this.medicaments[0]?.id ?? 0, quantite: 1, libelle: ligne.medicament };
    });
    this.message = '';
    this.error = '';
  }

  annulerPreparation(): void { this.selectedPrescription = null; this.lignes = []; }

  dispenser(): void {
    if (!this.selectedPrescription || !this.lignes.length || this.lignes.some((ligne) => !ligne.medicamentId || ligne.quantite < 1)) { this.error = 'Sélectionnez un médicament et une quantité valide pour chaque ligne.'; return; }
    this.saving = true;
    this.service.dispenser({ prescriptionId: this.selectedPrescription.id, lignes: this.lignes.map(({ medicamentId, quantite }) => ({ medicamentId, quantite })) }).subscribe({
      next: (response) => { this.message = `Ordonnance de ${this.selectedPrescription?.patientPrenom} ${this.selectedPrescription?.patientNom} dispensée et facture créée.`; this.factureService.get(response.factureId).subscribe({ next: (facture) => this.facture = facture }); this.annulerPreparation(); this.charger(); },
      error: (response) => this.error = response.status === 409 ? 'Stock insuffisant ou ordonnance déjà dispensée.' : 'La dispensation a échoué.',
      complete: () => this.saving = false
    });
  }

  get stocksBas(): number { return this.medicaments.filter((item) => item.stockActuel <= item.seuilAlerte).length; }
  get stockTotal(): number { return this.medicaments.reduce((total, item) => total + item.stockActuel, 0); }
  prescriptionMedicaments(prescription: PrescriptionPharmacie): string { return prescription.lignes.map((ligne) => ligne.medicament).join(', '); }
  format(value: number): string { return new Intl.NumberFormat('fr-FR').format(value) + ' MRU'; }
  stockClasse(item: Medicament): string { return item.stockActuel === 0 ? 'critical' : item.stockActuel <= item.seuilAlerte ? 'warning' : 'ok'; }
}
