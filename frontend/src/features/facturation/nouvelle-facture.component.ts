import { Component, inject } from '@angular/core';
import { DatePipe, DecimalPipe } from '@angular/common';
import { FormArray, FormBuilder, FormsModule, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { forkJoin } from 'rxjs';
import { ActeService } from '../../core/actes/acte.service';
import { CatalogueActeService } from '../../core/catalogue/catalogue-acte.service';
import { FactureCreateRequest, FactureService } from '../../core/factures/facture.service';
import { messageErreur } from '../../core/http/erreur-api';
import { ActeProgramme, TYPES_ACTE } from '../../core/models/acte';
import { CatalogueActe } from '../../core/models/catalogue';
import { MOYENS_PAIEMENT } from '../../core/models/facture';
import { Patient } from '../../core/models/patient';
import { RendezVous } from '../../core/models/rendez-vous';
import { PatientService } from '../../core/patients/patient.service';
import { Medecin, RendezVousService } from '../../core/rendez-vous/rendez-vous.service';
import { Soin, TYPES_SOIN } from '../../core/models/soin';
import { SoinService } from '../../core/soins/soin.service';

/**
 * Facture multi-actes. Ouverte depuis un rendez-vous ou un acte programme, elle leur est rattachee
 * (patient fixe, acte pre-rempli). Le patient paie tout de suite ou plus tard.
 */
@Component({ selector: 'app-nouvelle-facture', standalone: true, imports: [DatePipe, DecimalPipe, FormsModule, ReactiveFormsModule, RouterLink], templateUrl: './nouvelle-facture.component.html', styleUrl: './nouvelle-facture.component.css' })
export class NouvelleFactureComponent {
  private readonly builder = inject(FormBuilder);
  private readonly patientService = inject(PatientService);
  private readonly catalogueService = inject(CatalogueActeService);
  private readonly factureService = inject(FactureService);
  private readonly rendezVousService = inject(RendezVousService);
  private readonly acteService = inject(ActeService);
  private readonly soinService = inject(SoinService);
  private readonly router = inject(Router);
  private readonly parametres = inject(ActivatedRoute).snapshot.queryParamMap;
  readonly moyens = MOYENS_PAIEMENT;

  patients: Patient[] = [];
  acts: CatalogueActe[] = [];
  rendezVous: RendezVous | null = null;
  acte: ActeProgramme | null = null;
  soin: Soin | null = null;
  medecins: Medecin[] = [];
  encaisserMaintenant = true;
  moyenPaiement = 'ESPECES';
  referencePaiement = '';
  montantPaiement: number | null = null;
  loading = true;
  saving = false;
  error = '';

  readonly form = this.builder.group({
    patientId: ['', Validators.required],
    dateFacture: [new Date().toISOString().slice(0, 10), Validators.required],
    lignes: this.builder.array([this.newLine()])
  });

  constructor() {
    forkJoin({ patients: this.patientService.list(), acts: this.catalogueService.list(), medecins: this.rendezVousService.doctors() }).subscribe({
      next: (data) => {
        this.patients = data.patients;
        this.acts = data.acts.filter((act) => act.actif !== false);
        this.medecins = data.medecins;
        this.chargerOrigine();
      },
      error: () => { this.error = 'Impossible de charger les patients et le catalogue des actes.'; this.loading = false; }
    });
  }

  /** Rendez-vous ou acte d'origine : patient impose et premiere ligne pre-remplie. */
  private chargerOrigine(): void {
    const rendezVousId = Number(this.parametres.get('rendezVousId'));
    const acteId = Number(this.parametres.get('acteProgrammeId'));
    const soinId = Number(this.parametres.get('soinId'));
    const patientId = this.parametres.get('patientId');
    if (rendezVousId) {
      this.rendezVousService.get(rendezVousId).subscribe({
        next: (rendezVous) => {
          this.rendezVous = rendezVous;
          this.verrouillerPatient(rendezVous.patientId);
          if (rendezVous.factureId) this.error = `Ce rendez-vous a déjà la facture FAC-${String(rendezVous.factureId).padStart(3, '0')}.`;
          if (rendezVous.controleGratuit) {
            // Controle couvert par la consultation payee (regle de la direction) : rien a encaisser.
            this.preremplir(undefined, 'Consultation de contrôle (gratuite)', 'CONSULTATION');
            this.encaisserMaintenant = false;
          } else {
            const specialite = this.medecins.find((medecin) => medecin.id === rendezVous.medecinId)?.specialite;
            this.preremplir(CatalogueActeService.tarifConsultation(this.acts, specialite), rendezVous.motif ? `Consultation · ${rendezVous.motif}` : 'Consultation', 'CONSULTATION');
          }
          this.loading = false;
        },
        error: () => { this.error = 'Rendez-vous introuvable.'; this.loading = false; }
      });
    } else if (acteId) {
      this.acteService.get(acteId).subscribe({
        next: (acte) => {
          this.acte = acte;
          this.verrouillerPatient(acte.patientId);
          if (acte.factureId) this.error = `Cet acte a déjà la facture FAC-${String(acte.factureId).padStart(3, '0')}.`;
          const type = TYPES_ACTE.find((item) => item.code === acte.type)?.typeFacturation ?? 'AUTRE';
          this.preremplir(this.acts.find((act) => act.type === type), acte.intitule, type);
          this.loading = false;
        },
        error: () => { this.error = 'Acte programmé introuvable.'; this.loading = false; }
      });
    } else if (soinId) {
      this.soinService.get(soinId).subscribe({
        next: (soin) => {
          this.soin = soin;
          this.verrouillerPatient(soin.patientId);
          if (soin.factureId) this.error = `Ce soin a déjà la facture FAC-${String(soin.factureId).padStart(3, '0')}.`;
          const mot = TYPES_SOIN.find((type) => type.code === soin.type)?.motTarif ?? '';
          const sansAccent = (texte: string) => texte.normalize('NFD').replace(/[\u0300-\u036f]/g, '').toLowerCase();
          const tarif = this.acts.find((act) => act.type === 'SOINS' && mot && sansAccent(act.libelle).includes(mot));
          this.preremplir(tarif, soin.intitule, 'SOINS');
          if (tarif) this.lines.at(0).patchValue({ libelle: `${tarif.libelle}${soin.produit ? ' · ' + soin.produit : ''}`.slice(0, 150) });
          this.loading = false;
        },
        error: () => { this.error = 'Soin introuvable.'; this.loading = false; }
      });
    } else {
      if (patientId) this.form.patchValue({ patientId });
      this.loading = false;
    }
  }

  private verrouillerPatient(patientId: number): void {
    this.form.patchValue({ patientId: String(patientId) });
    this.form.controls.patientId.disable();
  }

  private preremplir(act: CatalogueActe | undefined, libelle: string, type: string): void {
    this.lines.at(0).patchValue({
      catalogueActeId: act ? String(act.id) : '',
      libelle: act ? act.libelle : libelle,
      typeActe: act ? act.type : type,
      montant: act ? act.montantDefaut : 0
    });
  }

  get lines(): FormArray { return this.form.controls.lignes; }
  get total(): number { return this.lines.controls.reduce((sum, line) => sum + Number(line.get('montant')?.value || 0), 0); }
  get patientOrigine(): string {
    const source = this.rendezVous ?? this.acte ?? this.soin;
    return source ? `${source.patientPrenom} ${source.patientNom}` : '';
  }
  get referenceObligatoire(): boolean { return this.moyenPaiement !== 'ESPECES'; }

  newLine() { return this.builder.group({ catalogueActeId: [''], libelle: ['', Validators.required], typeActe: ['', Validators.required], montant: [0, [Validators.required, Validators.min(0)]] }); }
  addLine(): void { this.lines.push(this.newLine()); }
  removeLine(index: number): void { if (this.lines.length > 1) this.lines.removeAt(index); }
  chooseAct(index: number): void {
    const line = this.lines.at(index);
    const act = this.acts.find((item) => item.id === Number(line.get('catalogueActeId')?.value));
    if (act) line.patchValue({ libelle: act.libelle, typeActe: act.type, montant: act.montantDefaut });
  }

  submit(): void {
    if (this.form.invalid) { this.form.markAllAsTouched(); this.error = 'Complétez le patient et chaque acte.'; return; }
    const montant = this.montantPaiement ?? this.total;
    // Facture a 0 MRU (controle gratuit) : soldee a la creation, aucun paiement a saisir.
    const encaisser = this.encaisserMaintenant && this.total > 0;
    if (encaisser) {
      if (montant <= 0 || montant > this.total) { this.error = 'Le montant encaissé doit être compris entre 1 et le total de la facture.'; return; }
      if (this.referencePaiement.trim() === '' && this.referenceObligatoire) { this.error = 'Saisissez la référence de la transaction (Bankily, Masrvi, Sedad, carte...).'; return; }
    }
    this.saving = true;
    this.error = '';
    const value = this.form.getRawValue();
    const request: FactureCreateRequest = {
      patientId: Number(value.patientId),
      dateFacture: value.dateFacture!,
      rendezVousId: this.rendezVous?.id ?? null,
      acteProgrammeId: this.acte?.id ?? null,
      soinId: this.soin?.id ?? null,
      lignes: value.lignes.map((line) => ({ catalogueActeId: line.catalogueActeId ? Number(line.catalogueActeId) : undefined, libelle: line.libelle!, typeActe: line.typeActe!, montant: Number(line.montant) })),
      paiement: encaisser ? { montant, moyenPaiement: this.moyenPaiement, reference: this.referencePaiement.trim() || undefined } : null
    };
    this.factureService.create(request).subscribe({
      next: (facture) => this.router.navigate(['/factures'], { queryParams: { id: facture.id } }),
      error: (response) => { this.error = messageErreur(response, 'La facture n’a pas pu être créée.'); this.saving = false; }
    });
  }
}
