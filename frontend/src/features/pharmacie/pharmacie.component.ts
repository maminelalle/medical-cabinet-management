import { Component, inject } from '@angular/core';
import { DatePipe } from '@angular/common';
import { HttpClient } from '@angular/common/http';
import { FormBuilder, FormsModule, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import * as XLSX from 'xlsx';
import { AuthService } from '../../core/auth/auth.service';
import { messageErreur } from '../../core/http/erreur-api';
import { telecharger } from '../../core/http/telechargement';
import { MOYENS_PAIEMENT, libelleMoyen } from '../../core/models/facture';
import {
  Medicament, MedicamentRequest, PharmacieFinances, PrescriptionPharmacie, libelleMouvement
} from '../../core/models/pharmacie';
import { PharmacieService } from '../../core/pharmacie/pharmacie.service';

interface LigneVente { medicamentId: number; quantite: number; libelle: string; posologie?: string; }
type Onglet = 'stock' | 'ordonnances' | 'finances';

/** Pharmacie interne : stock, vente des ordonnances avec paiement obligatoire, finances. */
@Component({
  selector: 'app-pharmacie',
  standalone: true,
  imports: [DatePipe, FormsModule, ReactiveFormsModule, RouterLink],
  templateUrl: './pharmacie.component.html',
  styleUrl: './pharmacie.component.css'
})
export class PharmacieComponent {
  private readonly service = inject(PharmacieService);
  private readonly http = inject(HttpClient);
  private readonly router = inject(Router);
  private readonly builder = inject(FormBuilder);
  private readonly role = inject(AuthService).role();

  readonly peutAdministrer = this.role === 'PHARMACIEN' || this.role === 'DIRECTION';
  readonly peutVendre = this.role === 'PHARMACIEN';
  readonly moyens = MOYENS_PAIEMENT;
  readonly libelleMoyen = libelleMoyen;
  readonly libelleMouvement = libelleMouvement;
  readonly form = this.builder.nonNullable.group({
    nom: ['', [Validators.required, Validators.maxLength(150)]],
    dosage: [''], forme: [''], famille: [''], stockActuel: [0, [Validators.required, Validators.min(0)]],
    seuilAlerte: [10, [Validators.required, Validators.min(0)]], prixAchat: [0, [Validators.required, Validators.min(0)]],
    prixVente: [0, [Validators.required, Validators.min(0)]], fournisseur: [''], dateExpiration: ['']
  });

  onglet: Onglet = 'stock';
  medicaments: Medicament[] = [];
  prescriptions: PrescriptionPharmacie[] = [];
  finances: PharmacieFinances | null = null;
  recherche = '';
  famille = '';
  formulaireOuvert = false;
  selectedPrescription: PrescriptionPharmacie | null = null;
  lignes: LigneVente[] = [];
  moyenPaiement = 'ESPECES';
  referencePaiement = '';
  derniereVente: { factureId: number; patient: string; total: number; moyen: string } | null = null;
  dateDebut = new Date(new Date().getFullYear(), new Date().getMonth(), 1).toLocaleDateString('sv-SE');
  dateFin = new Date().toLocaleDateString('sv-SE');
  loading = true;
  saving = false;
  message = '';
  error = '';

  constructor() { this.charger(); }

  charger(): void {
    this.loading = true;
    this.error = '';
    this.service.medicaments().subscribe({ next: (items) => { this.medicaments = items; this.loading = false; }, error: () => { this.error = 'Le stock est indisponible.'; this.loading = false; } });
    this.service.prescriptions().subscribe({ next: (items) => this.prescriptions = items, error: () => this.error = 'Les prescriptions sont indisponibles.' });
    this.chargerFinances();
  }

  changerOnglet(onglet: Onglet): void {
    this.onglet = onglet;
    this.message = '';
    this.error = '';
    if (onglet === 'finances' && !this.finances) this.chargerFinances();
  }

  // --- Stock ---------------------------------------------------------------------------------

  get familles(): string[] {
    return [...new Set(this.medicaments.map((item) => item.famille).filter((famille): famille is string => !!famille))].sort();
  }

  /** Recherche par nom, dosage ou famille ; filtre optionnel par famille. */
  get medicamentsFiltres(): Medicament[] {
    const terme = this.normaliser(this.recherche);
    return this.medicaments.filter((item) =>
      (!this.famille || item.famille === this.famille)
      && (!terme || this.normaliser(`${item.nom} ${item.dosage ?? ''} ${item.famille ?? ''} ${item.forme ?? ''}`).includes(terme)));
  }

  ouvrir(item: Medicament): void { this.router.navigate(['/pharmacie/medicaments', item.id]); }

  creerMedicament(): void {
    if (this.form.invalid) { this.form.markAllAsTouched(); return; }
    this.saving = true;
    this.service.creerMedicament(this.form.getRawValue()).subscribe({
      next: (item) => {
        this.medicaments = [...this.medicaments, item].sort((a, b) => a.nom.localeCompare(b.nom));
        this.form.reset({ nom: '', dosage: '', forme: '', famille: '', stockActuel: 0, seuilAlerte: 10, prixAchat: 0, prixVente: 0, fournisseur: '', dateExpiration: '' });
        this.message = `${item.nom} a été ajouté au stock.`;
        this.formulaireOuvert = false;
        this.saving = false;
      },
      error: (response) => { this.error = messageErreur(response, 'La création du médicament a échoué.'); this.saving = false; }
    });
  }

  /** Export Excel du stock (memes colonnes que l'import). */
  exporterExcel(): void {
    const lignes = this.medicamentsFiltres.map((item) => ({
      Nom: item.nom, Dosage: item.dosage ?? '', Forme: item.forme ?? '', Famille: item.famille ?? '',
      'Stock actuel': item.stockActuel, 'Seuil alerte': item.seuilAlerte, 'Prix achat': item.prixAchat, 'Prix vente': item.prixVente,
      Fournisseur: item.fournisseur ?? '', 'Date expiration': item.dateExpiration ?? ''
    }));
    const classeur = XLSX.utils.book_new();
    XLSX.utils.book_append_sheet(classeur, XLSX.utils.json_to_sheet(lignes), 'Stock');
    XLSX.writeFile(classeur, `stock-pharmacie-${this.dateFin}.xlsx`);
  }

  /** Inventaire PDF genere par le serveur (JasperReports). */
  exporterPdf(): void {
    this.error = '';
    telecharger(this.http, this.service.inventairePdfUrl(), `inventaire-pharmacie-${this.dateFin}.pdf`).subscribe({
      error: () => this.error = 'L’export PDF a échoué.'
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
          next: (items) => { this.message = `${items.length} médicament(s) importé(s).`; this.saving = false; this.charger(); },
          error: (response) => { this.error = messageErreur(response, 'L’import Excel a échoué.'); this.saving = false; }
        });
      } catch { this.error = 'Le fichier Excel est illisible.'; }
    };
    lecteur.readAsArrayBuffer(fichier);
    input.value = '';
  }

  private ligneExcel(ligne: Record<string, unknown>): MedicamentRequest | null {
    const valeur = (...cles: string[]): unknown => {
      const cle = Object.keys(ligne).find((item) => cles.includes(this.normaliser(item).replace(/ /g, '')));
      return cle ? ligne[cle] : '';
    };
    const nom = String(valeur('nom', 'medicament')).trim();
    if (!nom) return null;
    const nombre = (val: unknown): number => Number(String(val).replace(',', '.')) || 0;
    return { nom, dosage: String(valeur('dosage') || ''), forme: String(valeur('forme') || ''), famille: String(valeur('famille', 'classe') || ''),
      stockActuel: nombre(valeur('stockactuel', 'stock')), seuilAlerte: nombre(valeur('seuilalerte', 'seuil')),
      prixAchat: nombre(valeur('prixachat', 'achat')), prixVente: nombre(valeur('prixvente', 'vente', 'prix')),
      fournisseur: String(valeur('fournisseur') || ''), dateExpiration: String(valeur('dateexpiration', 'expiration') || '') || undefined };
  }

  // --- Vente des ordonnances -----------------------------------------------------------------

  preparer(prescription: PrescriptionPharmacie): void {
    this.selectedPrescription = prescription;
    this.derniereVente = null;
    this.moyenPaiement = 'ESPECES';
    this.referencePaiement = '';
    this.lignes = prescription.lignes.map((ligne) => {
      const medicament = this.medicaments.find((item) => item.id === ligne.medicamentId)
        ?? this.medicaments.find((item) => ligne.medicament.toLowerCase().includes(item.nom.toLowerCase()));
      return { medicamentId: medicament?.id ?? 0, quantite: 1, libelle: ligne.medicament, posologie: ligne.posologie };
    });
    this.message = '';
    this.error = '';
  }

  annulerPreparation(): void { this.selectedPrescription = null; this.lignes = []; }

  prixLigne(ligne: LigneVente): number {
    const medicament = this.medicaments.find((item) => item.id === Number(ligne.medicamentId));
    return medicament ? medicament.prixVente * (Number(ligne.quantite) || 0) : 0;
  }
  get totalVente(): number { return this.lignes.reduce((total, ligne) => total + this.prixLigne(ligne), 0); }
  get referenceObligatoire(): boolean { return this.moyenPaiement !== 'ESPECES'; }

  /** Vente : la delivrance, la facture et le paiement sont enregistres ensemble. */
  dispenser(): void {
    const prescription = this.selectedPrescription;
    if (!prescription || !this.lignes.length || this.lignes.some((ligne) => !ligne.medicamentId || ligne.quantite < 1)) { this.error = 'Sélectionnez un médicament et une quantité valide pour chaque ligne.'; return; }
    if (this.referencePaiement.trim() === '' && this.referenceObligatoire) { this.error = 'Saisissez la référence du paiement ' + libelleMoyen(this.moyenPaiement) + '.'; return; }
    this.saving = true;
    this.error = '';
    const total = this.totalVente;
    this.service.dispenser({
      prescriptionId: prescription.id,
      lignes: this.lignes.map(({ medicamentId, quantite }) => ({ medicamentId: Number(medicamentId), quantite: Number(quantite) })),
      moyenPaiement: this.moyenPaiement,
      referencePaiement: this.referencePaiement.trim() || undefined
    }).subscribe({
      next: (response) => {
        this.derniereVente = { factureId: response.factureId, patient: `${prescription.patientPrenom} ${prescription.patientNom}`, total, moyen: libelleMoyen(this.moyenPaiement) };
        this.message = `Ordonnance de ${prescription.patientPrenom} ${prescription.patientNom} délivrée et payée.`;
        this.annulerPreparation();
        this.saving = false;
        this.finances = null;
        this.charger();
      },
      error: (response) => { this.error = messageErreur(response, 'La vente a échoué.'); this.saving = false; }
    });
  }

  // --- Finances --------------------------------------------------------------------------------

  chargerFinances(): void {
    this.service.finances(this.dateDebut, this.dateFin).subscribe({
      next: (finances) => this.finances = finances,
      error: () => this.error = 'Les finances de la pharmacie sont indisponibles.'
    });
  }

  get stocksBas(): number { return this.medicaments.filter((item) => item.stockActuel <= item.seuilAlerte).length; }
  get stockTotal(): number { return this.medicaments.reduce((total, item) => total + item.stockActuel, 0); }
  prescriptionMedicaments(prescription: PrescriptionPharmacie): string { return prescription.lignes.map((ligne) => ligne.medicament).join(', '); }
  format(value: number | null | undefined): string { return new Intl.NumberFormat('fr-FR').format(Number(value ?? 0)) + ' MRU'; }
  stockClasse(item: Medicament): string { return item.stockActuel === 0 ? 'critical' : item.stockActuel <= item.seuilAlerte ? 'warning' : 'ok'; }
  private normaliser(value: string): string { return value.toLowerCase().normalize('NFD').replace(/[̀-ͯ]/g, '').trim(); }
}
