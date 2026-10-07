import { Component, DestroyRef, inject } from '@angular/core';
import { DatePipe } from '@angular/common';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FormBuilder, FormsModule, ReactiveFormsModule, Validators } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { filter } from 'rxjs';
import { AuthService } from '../../core/auth/auth.service';
import { CatalogueActeService } from '../../core/catalogue/catalogue-acte.service';
import { messageErreur } from '../../core/http/erreur-api';
import { CatalogueActe } from '../../core/models/catalogue';
import { Patient } from '../../core/models/patient';
import { Soin, SoinRequest, StatutSoin, TYPES_SOIN, TypeSoin, libelleTypeSoin } from '../../core/models/soin';
import { PatientService } from '../../core/patients/patient.service';
import { SoinService } from '../../core/soins/soin.service';
import { TempsReelService } from '../../core/temps-reel/temps-reel.service';

/**
 * Soins au cabinet : le patient vient pour une injection, une perfusion, un pansement... souvent avec
 * l'ordonnance d'un autre medecin et son propre produit. L'accueil enregistre, l'equipe realise, l'accueil facture.
 */
@Component({
  selector: 'app-soins',
  standalone: true,
  imports: [DatePipe, FormsModule, ReactiveFormsModule, RouterLink],
  template: `
    <section class="soins-page">
      <header class="page-heading">
        <div>
          <p class="breadcrumb">PARCOURS PATIENT / SOINS</p>
          <h1>Soins et injections</h1>
          <p class="subtitle">Injection, perfusion, pansement, nébulisation ou prise des constantes : enregistrement, réalisation puis facturation.</p>
        </div>
        <label class="jour">Jour <input type="date" [(ngModel)]="jour" (change)="charger()"></label>
      </header>

      <section class="stats">
        <article class="card stat"><span>En attente</span><strong>{{ compter('EN_ATTENTE') }}</strong></article>
        <article class="card stat"><span>En cours</span><strong>{{ compter('EN_COURS') }}</strong></article>
        <article class="card stat"><span>Terminés</span><strong>{{ compter('TERMINE') }}</strong></article>
        <article class="card stat"><span>À facturer</span><strong>{{ aFacturer }}</strong></article>
      </section>

      <section class="soins-layout" [class.lecture]="!peutGerer">
        <article class="card liste">
          <div class="table-heading">
            <div><h2>Soins du {{ jour | date:'EEEE d MMMM':'':'fr-FR' }}</h2><p>{{ soins.length }} soin(s)</p></div>
            <button class="soft-button" type="button" (click)="charger()">↻ Actualiser</button>
          </div>
          @if (message) { <p class="message" [class.erreur]="erreur">{{ message }}</p> }
          @if (chargement) {
            <div class="empty-state">Chargement des soins...</div>
          } @else {
            @for (soin of soins; track soin.id) {
              <div class="soin" [class]="'soin statut-' + soin.statut">
                <div class="soin-entete">
                  <span class="heure">{{ soin.dateHeure | date:'HH:mm' }}</span>
                  <div>
                    <strong>{{ soin.patientPrenom }} {{ soin.patientNom }}</strong>
                    <small>{{ libelleType(soin.type) }} · {{ soin.intitule }}</small>
                  </div>
                  <span class="badge" [class]="'badge ' + classeStatut(soin.statut)">{{ libelleStatut(soin.statut) }}</span>
                </div>
                <div class="soin-details">
                  @if (soin.produit) { <span>Produit : {{ soin.produit }}</span> }
                  @if (soin.prescripteur) { <span>Prescrit par {{ soin.prescripteur }}</span> }
                  @if (soin.realisePar) { <span>Réalisé par {{ soin.realisePar }}</span> }
                  @if (soin.observations) { <span class="observations">{{ soin.observations }}</span> }
                  @if (soin.factureId) {
                    <span>Facture FAC-{{ pad(soin.factureId) }} · {{ soin.factureStatut === 'PAYEE' ? 'payée' : 'à payer' }}</span>
                  }
                </div>
                <div class="soin-actions">
                  @if (peutGerer && soin.statut === 'EN_ATTENTE') {
                    <button class="primary-button petit" type="button" (click)="demarrer(soin)">Démarrer</button>
                  }
                  @if (peutGerer && soin.statut === 'EN_COURS') {
                    <button class="primary-button petit" type="button" (click)="terminer(soin)">Terminer</button>
                  }
                  @if (peutFacturer && !soin.factureId && soin.statut !== 'ANNULE') {
                    <a class="soft-button petit" routerLink="/factures/nouveau" [queryParams]="{ soinId: soin.id }">Facturer</a>
                  }
                  @if (soin.factureId) {
                    <a class="soft-button petit" routerLink="/factures" [queryParams]="{ id: soin.factureId }">Voir la facture</a>
                  }
                  @if (peutGerer && (soin.statut === 'EN_ATTENTE' || soin.statut === 'EN_COURS') && !soin.factureId) {
                    <button class="link-button danger" type="button" (click)="annuler(soin)">Annuler</button>
                  }
                </div>
              </div>
            } @empty {
              <div class="empty-state">Aucun soin ce jour.</div>
            }
          }
        </article>

        @if (peutGerer) {
          <form class="card formulaire" [formGroup]="form" (ngSubmit)="enregistrer()">
            <div class="section-heading"><div><h2>Nouveau soin</h2><p>Le patient est présent : le soin est daté de maintenant</p></div></div>

            <div class="bascule">
              <button type="button" [class.active]="modePatient === 'existant'" (click)="modePatient = 'existant'">Patient existant</button>
              <button type="button" [class.active]="modePatient === 'nouveau'" (click)="modePatient = 'nouveau'">+ Nouveau patient</button>
            </div>
            @if (modePatient === 'existant') {
              @if (patientChoisi) {
                <div class="patient-choisi">
                  <strong>{{ patientChoisi.prenom }} {{ patientChoisi.nom }}</strong>
                  <small>{{ patientChoisi.telephone || '—' }}</small>
                  <button type="button" class="link-button" (click)="patientChoisi = null">Changer</button>
                </div>
              } @else {
                <label>Rechercher le patient<input [(ngModel)]="recherche" [ngModelOptions]="{ standalone: true }" placeholder="Nom, prénom ou téléphone"></label>
                @for (patient of resultats; track patient.id) {
                  <button type="button" class="resultat" (click)="patientChoisi = patient; recherche = ''">{{ patient.prenom }} {{ patient.nom }} · {{ patient.telephone || '—' }}</button>
                }
              }
            } @else {
              <div class="grille" [formGroup]="nouveauPatient">
                <label>Prénom *<input formControlName="prenom"></label>
                <label>Nom *<input formControlName="nom"></label>
                <label>Date de naissance *<input type="date" formControlName="dateNaissance"></label>
                <label>Téléphone *<input formControlName="telephone" placeholder="22 00 00 00"></label>
              </div>
            }

            <label>Type de soin *
              <select formControlName="type" (change)="typeChange()">
                @for (type of types; track type.code) { <option [value]="type.code">{{ type.libelle }}</option> }
              </select>
            </label>
            <label>Intitulé *<input formControlName="intitule" placeholder="Injection intramusculaire"></label>
            <label>Produit administré<input formControlName="produit" placeholder="Ceftriaxone 1 g (apporté par le patient)"></label>
            <label>Prescripteur<input formControlName="prescripteurExterne" placeholder="Dr. Ould Ely, clinique externe"></label>
            <label>Observations<textarea rows="2" formControlName="observations"></textarea></label>
            @if (tarif) { <p class="tarif">Tarif de la direction : <strong>{{ tarif.libelle }} · {{ format(tarif.montantDefaut) }}</strong></p> }
            <div class="form-actions">
              <button class="primary-button" type="submit" [disabled]="enregistrement">{{ enregistrement ? 'Enregistrement...' : 'Enregistrer le soin' }}</button>
            </div>
          </form>
        }
      </section>
    </section>
  `,
  styles: [`
    .soins-page { display: grid; gap: 18px; }
    .page-heading { display: flex; justify-content: space-between; gap: 16px; flex-wrap: wrap; align-items: end; }
    .jour { display: grid; gap: 4px; font-size: var(--fs-sm); font-weight: 700; }
    .stats { display: grid; grid-template-columns: repeat(4, minmax(0, 1fr)); gap: 12px; }
    .stat { display: grid; gap: 4px; }
    .stat span { color: var(--text-muted); font-size: var(--fs-sm); }
    .stat strong { font-size: 1.6rem; }
    .soins-layout { display: grid; grid-template-columns: minmax(0, 1.6fr) minmax(300px, 1fr); gap: 18px; align-items: start; }
    .soins-layout.lecture { grid-template-columns: minmax(0, 1fr); }
    .liste { display: grid; gap: 10px; }
    .table-heading { display: flex; justify-content: space-between; gap: 12px; }
    .soin { display: grid; gap: 8px; padding: 12px 14px; border: 1px solid var(--border); border-inline-start: 4px solid var(--border); border-radius: var(--radius-md); }
    .soin.statut-EN_ATTENTE { border-inline-start-color: #f59e0b; }
    .soin.statut-EN_COURS { border-inline-start-color: #2563eb; }
    .soin.statut-TERMINE { border-inline-start-color: #16a34a; }
    .soin.statut-ANNULE { opacity: .6; }
    .soin-entete { display: grid; grid-template-columns: auto minmax(0, 1fr) auto; gap: 12px; align-items: center; }
    .soin-entete small { display: block; color: var(--text-muted); }
    .heure { font-weight: 800; font-variant-numeric: tabular-nums; }
    .soin-details { display: flex; flex-wrap: wrap; gap: 6px 14px; color: var(--text-muted); font-size: var(--fs-sm); }
    .observations { width: 100%; white-space: pre-line; color: var(--text-strong); }
    .soin-actions { display: flex; flex-wrap: wrap; gap: 8px; align-items: center; }
    .petit { padding: 6px 12px; font-size: var(--fs-sm); }
    .formulaire { display: grid; gap: 12px; }
    .bascule { display: flex; gap: 6px; }
    .bascule button { flex: 1; padding: 8px; border: 1px solid var(--border); border-radius: var(--radius-sm); background: var(--bg-surface); font: inherit; font-weight: 700; cursor: pointer; }
    .bascule button.active { background: var(--brand-soft); color: var(--brand-dark); border-color: transparent; }
    .grille { display: grid; grid-template-columns: 1fr 1fr; gap: 10px; }
    .patient-choisi { display: flex; gap: 10px; align-items: center; padding: 10px; border-radius: var(--radius-sm); background: var(--brand-soft); }
    .patient-choisi small { color: var(--text-muted); }
    .resultat { text-align: start; padding: 8px 10px; border: 1px solid var(--border); border-radius: var(--radius-sm); background: var(--bg-surface); font: inherit; cursor: pointer; }
    .tarif { margin: 0; color: var(--text-muted); }
    .link-button { border: 0; background: none; color: var(--brand-dark); font: inherit; font-weight: 700; cursor: pointer; }
    .link-button.danger, .message.erreur { color: var(--danger, #c0392b); }
    @media (max-width: 1100px) { .soins-layout, .stats { grid-template-columns: minmax(0, 1fr); } }
  `]
})
export class SoinsComponent {
  private readonly service = inject(SoinService);
  private readonly builder = inject(FormBuilder);
  private readonly role = inject(AuthService).role();

  readonly types = TYPES_SOIN;
  readonly peutGerer = this.role === 'ACCUEIL' || this.role === 'MEDECIN';
  readonly peutFacturer = this.role === 'ACCUEIL';
  readonly form = this.builder.nonNullable.group({
    type: ['INJECTION' as TypeSoin, Validators.required],
    intitule: ['Injection intramusculaire', [Validators.required, Validators.maxLength(200)]],
    produit: ['', Validators.maxLength(255)],
    prescripteurExterne: ['', Validators.maxLength(200)],
    observations: ['']
  });
  readonly nouveauPatient = this.builder.nonNullable.group({
    nom: ['', Validators.required], prenom: ['', Validators.required],
    dateNaissance: ['', Validators.required], telephone: ['', Validators.required]
  });

  jour = new Date().toISOString().slice(0, 10);
  soins: Soin[] = [];
  patients: Patient[] = [];
  tarifs: CatalogueActe[] = [];
  patientChoisi: Patient | null = null;
  modePatient: 'existant' | 'nouveau' = 'existant';
  recherche = '';
  chargement = true;
  enregistrement = false;
  message = '';
  erreur = false;

  constructor() {
    this.charger();
    if (this.peutGerer) inject(PatientService).list().subscribe({ next: (patients) => { this.patients = patients; } });
    inject(CatalogueActeService).list().subscribe({ next: (tarifs) => { this.tarifs = tarifs; } });
    // Un soin demarre ou termine ailleurs (autre poste) apparait sans recharger la page.
    inject(TempsReelService).changements$
      .pipe(filter((evenement) => evenement.type === 'SOINS'), takeUntilDestroyed(inject(DestroyRef)))
      .subscribe(() => this.charger(false));
  }

  get resultats(): Patient[] {
    const terme = this.recherche.trim().toLowerCase();
    if (terme.length < 2) return [];
    return this.patients.filter((patient) =>
      `${patient.prenom} ${patient.nom} ${patient.nom} ${patient.prenom} ${patient.telephone ?? ''}`.toLowerCase().includes(terme)).slice(0, 6);
  }

  /** Tarif fixe par la direction pour le type de soin choisi. */
  get tarif(): CatalogueActe | undefined {
    const mot = TYPES_SOIN.find((type) => type.code === this.form.controls.type.value)?.motTarif;
    if (!mot) return undefined;
    return this.tarifs.find((tarif) => tarif.type === 'SOINS' && tarif.libelle.normalize('NFD').replace(/[̀-ͯ]/g, '').toLowerCase().includes(mot));
  }

  get aFacturer(): number { return this.soins.filter((soin) => !soin.factureId && soin.statut !== 'ANNULE').length; }
  compter(statut: StatutSoin): number { return this.soins.filter((soin) => soin.statut === statut).length; }

  charger(indicateur = true): void {
    if (indicateur) this.chargement = true;
    this.service.list(this.jour).subscribe({
      next: (soins) => { this.soins = [...soins].sort((a, b) => a.dateHeure.localeCompare(b.dateHeure)); this.chargement = false; },
      error: (erreur) => { this.afficher(messageErreur(erreur, 'Les soins sont indisponibles.'), true); this.chargement = false; }
    });
  }

  typeChange(): void {
    const type = TYPES_SOIN.find((item) => item.code === this.form.controls.type.value);
    this.form.patchValue({ intitule: type?.intitule ?? '' });
  }

  enregistrer(): void {
    if (this.modePatient === 'existant' && !this.patientChoisi) { this.afficher('Recherchez et sélectionnez le patient, ou créez-le.', true); return; }
    if (this.modePatient === 'nouveau' && this.nouveauPatient.invalid) { this.nouveauPatient.markAllAsTouched(); this.afficher('Complétez la fiche du nouveau patient.', true); return; }
    if (this.form.invalid) { this.form.markAllAsTouched(); return; }
    const valeur = this.form.getRawValue();
    const requete: SoinRequest = {
      ...valeur,
      ...(this.modePatient === 'nouveau'
        ? { nouveauPatient: { ...this.nouveauPatient.getRawValue(), email: '', adresse: '' } }
        : { patientId: this.patientChoisi!.id })
    };
    this.enregistrement = true;
    this.service.create(requete).subscribe({
      next: (soin) => {
        this.afficher(`Soin enregistré pour ${soin.patientPrenom} ${soin.patientNom}.`, false);
        this.form.reset({ type: 'INJECTION', intitule: 'Injection intramusculaire', produit: '', prescripteurExterne: '', observations: '' });
        this.nouveauPatient.reset();
        this.patientChoisi = null;
        this.modePatient = 'existant';
        this.enregistrement = false;
        this.jour = soin.dateHeure.slice(0, 10);
        this.charger();
      },
      error: (erreur) => { this.afficher(messageErreur(erreur, 'Le soin n’a pas pu être enregistré.'), true); this.enregistrement = false; }
    });
  }

  demarrer(soin: Soin): void { this.service.demarrer(soin.id).subscribe({ next: () => this.charger(false), error: (e) => this.afficher(messageErreur(e, 'Action impossible.'), true) }); }

  terminer(soin: Soin): void {
    const observations = prompt('Observations (réaction, tension, suite à donner...) :', soin.observations ?? '');
    if (observations === null) return;
    this.service.terminer(soin.id, observations).subscribe({ next: () => this.charger(false), error: (e) => this.afficher(messageErreur(e, 'Action impossible.'), true) });
  }

  annuler(soin: Soin): void {
    const motif = prompt('Motif de l’annulation :');
    if (!motif?.trim()) return;
    this.service.annuler(soin.id, motif.trim()).subscribe({ next: () => this.charger(false), error: (e) => this.afficher(messageErreur(e, 'Annulation impossible.'), true) });
  }

  libelleType(type: TypeSoin): string { return libelleTypeSoin(type); }
  libelleStatut(statut: StatutSoin): string {
    return { EN_ATTENTE: 'En attente', EN_COURS: 'En cours', TERMINE: 'Terminé', ANNULE: 'Annulé' }[statut];
  }
  classeStatut(statut: StatutSoin): string {
    return { EN_ATTENTE: 'warning', EN_COURS: 'info', TERMINE: 'success', ANNULE: 'neutral' }[statut];
  }
  pad(id: number): string { return String(id).padStart(3, '0'); }
  format(valeur: number): string { return new Intl.NumberFormat('fr-FR').format(Number(valeur)) + ' MRU'; }
  private afficher(texte: string, erreur: boolean): void { this.message = texte; this.erreur = erreur; }
}
