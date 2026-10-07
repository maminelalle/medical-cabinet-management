import { Component, inject } from '@angular/core';
import { FormBuilder, FormsModule, ReactiveFormsModule, Validators } from '@angular/forms';
import { CatalogueActeService } from '../../core/catalogue/catalogue-acte.service';
import { messageErreur } from '../../core/http/erreur-api';
import { CatalogueActe, RegleControleGratuit, TYPES_TARIF } from '../../core/models/catalogue';
import { Medecin, RendezVousService } from '../../core/rendez-vous/rendez-vous.service';

/**
 * Tarifs du cabinet (direction) : tarif de consultation par specialite, soins (injection, perfusion...),
 * actes et examens ; et regle de la consultation de controle gratuite.
 */
@Component({
  selector: 'app-tarifs',
  standalone: true,
  imports: [ReactiveFormsModule, FormsModule],
  template: `
    <section class="tarifs-page">
      <header class="page-heading">
        <div>
          <p class="breadcrumb">DIRECTION / TARIFS</p>
          <h1>Tarifs du cabinet</h1>
          <p class="subtitle">Consultations par spécialité, soins, actes et examens. Ces montants sont proposés automatiquement à l'accueil lors de la facturation.</p>
        </div>
      </header>

      <article class="card regle">
        <div class="regle-texte">
          <h2>Consultation de contrôle gratuite</h2>
          <p>
            Après une consultation payée, le patient revient voir <strong>le même médecin</strong> sans payer
            pendant le délai choisi. L'accueil est prévenu automatiquement lors de la prise de rendez-vous.
          </p>
        </div>
        @if (regle) {
          <div class="regle-champs">
            <label class="interrupteur">
              <input type="checkbox" [(ngModel)]="regle.actif" name="actif">
              <span>{{ regle.actif ? 'Activée' : 'Désactivée' }}</span>
            </label>
            <label>Délai (jours)
              <input type="number" min="1" max="365" [(ngModel)]="regle.jours" name="jours" [disabled]="!regle.actif">
            </label>
            <label>Contrôles gratuits par consultation
              <input type="number" min="1" max="10" [(ngModel)]="regle.nombre" name="nombre" [disabled]="!regle.actif">
            </label>
            <button class="primary-button" type="button" (click)="enregistrerRegle()" [disabled]="enregistrementRegle">
              {{ enregistrementRegle ? 'Enregistrement...' : 'Enregistrer la règle' }}
            </button>
          </div>
          @if (messageRegle) { <p class="message">{{ messageRegle }}</p> }
        }
      </article>

      <section class="tarifs-layout">
        <article class="card table-card">
          <div class="table-heading">
            <div>
              <h2>Grille tarifaire</h2>
              <p>{{ actifs }} tarif(s) actif(s) sur {{ tarifs.length }}</p>
            </div>
            <div class="filtres">
              <select [(ngModel)]="filtreType" name="filtreType" aria-label="Filtrer par type">
                <option value="">Tous les types</option>
                @for (type of types; track type) { <option [value]="type">{{ typeLabel(type) }}</option> }
              </select>
              <button class="soft-button" type="button" (click)="charger()">↻ Actualiser</button>
            </div>
          </div>
          @if (message) { <p class="message" [class.erreur]="erreur">{{ message }}</p> }
          @if (loading) {
            <div class="empty-state">Chargement des tarifs...</div>
          } @else {
            <div class="table-wrap">
              <table>
                <thead><tr><th>Tarif</th><th>Type</th><th>Spécialité</th><th>Montant</th><th>Statut</th><th></th></tr></thead>
                <tbody>
                  @for (tarif of tarifsFiltres; track tarif.id) {
                    <tr [class.inactif]="!tarif.actif" [class.selection]="enEdition?.id === tarif.id">
                      <td><strong>{{ tarif.libelle }}</strong>@if (tarif.description) { <small>{{ tarif.description }}</small> }</td>
                      <td><span class="badge neutral">{{ typeLabel(tarif.type) }}</span></td>
                      <td>{{ tarif.specialite || '—' }}</td>
                      <td class="amount">{{ format(tarif.montantDefaut) }}</td>
                      <td><span class="badge" [class.success]="tarif.actif" [class.neutral]="!tarif.actif">{{ tarif.actif ? 'Actif' : 'Inactif' }}</span></td>
                      <td class="actions">
                        <button type="button" class="link-button" (click)="editer(tarif)">Modifier</button>
                        <button type="button" class="link-button" (click)="basculer(tarif)">{{ tarif.actif ? 'Désactiver' : 'Activer' }}</button>
                        @if (!tarif.utilise) { <button type="button" class="link-button danger" (click)="supprimer(tarif)">Supprimer</button> }
                      </td>
                    </tr>
                  } @empty {
                    <tr><td colspan="6" class="empty-cell">Aucun tarif.</td></tr>
                  }
                </tbody>
              </table>
            </div>
          }
        </article>

        <form class="card" [formGroup]="form" (ngSubmit)="enregistrer()">
          <div class="section-heading">
            <div>
              <h2>{{ enEdition ? 'Modifier le tarif' : 'Nouveau tarif' }}</h2>
              <p>{{ enEdition ? enEdition.libelle : 'Consultation, soin, acte ou examen' }}</p>
            </div>
          </div>
          <label>Libellé *<input formControlName="libelle" placeholder="Consultation de cardiologie"></label>
          <label>Type *
            <select formControlName="type">
              @for (type of types; track type) { <option [value]="type">{{ typeLabel(type) }}</option> }
            </select>
          </label>
          @if (form.controls.type.value === 'CONSULTATION') {
            <label>Spécialité du médecin
              <select formControlName="specialite">
                <option value="">Tarif général (toutes spécialités)</option>
                @for (specialite of specialites; track specialite) { <option [value]="specialite">{{ specialite }}</option> }
              </select>
            </label>
          }
          <label>Montant (MRU) *<input type="number" min="0" step="100" formControlName="montantDefaut"></label>
          <label>Description<input formControlName="description" placeholder="Précision affichée à l'accueil"></label>
          <label class="interrupteur"><input type="checkbox" formControlName="actif"><span>Proposé à la facturation</span></label>
          <div class="form-actions">
            @if (enEdition) { <button class="soft-button" type="button" (click)="annulerEdition()">Annuler</button> }
            <button class="primary-button" type="submit" [disabled]="saving">{{ saving ? 'Enregistrement...' : enEdition ? 'Enregistrer' : 'Ajouter le tarif' }}</button>
          </div>
        </form>
      </section>
    </section>
  `,
  styles: [`
    .tarifs-page { display: grid; gap: 20px; }
    .regle { display: grid; grid-template-columns: minmax(0, 1fr) auto; gap: 18px; align-items: center; }
    .regle h2 { margin: 0 0 6px; }
    .regle p { margin: 0; color: var(--text-muted); }
    .regle-champs { display: flex; flex-wrap: wrap; gap: 12px; align-items: end; }
    .regle-champs label:not(.interrupteur) { display: grid; gap: 4px; font-size: var(--fs-sm); max-width: 170px; }
    .regle .message { grid-column: 1 / -1; }
    .interrupteur { display: flex; gap: 8px; align-items: center; font-weight: 700; }
    .interrupteur input { width: 18px; height: 18px; }
    .tarifs-layout { display: grid; grid-template-columns: minmax(0, 1.8fr) minmax(280px, 1fr); gap: 18px; align-items: start; }
    .tarifs-page form.card { display: grid; gap: 14px; }
    .table-card { padding: 0; overflow: hidden; }
    .table-card .table-heading { padding: 22px 22px 0; display: flex; justify-content: space-between; gap: 12px; flex-wrap: wrap; }
    .table-card .message, .table-card .empty-state { margin: 12px 22px; }
    .table-card .table-wrap { margin-top: 14px; }
    .filtres { display: flex; gap: 8px; }
    td small { display: block; color: var(--text-muted); }
    td.amount { font-weight: 800; white-space: nowrap; text-align: end; }
    td.actions { white-space: nowrap; text-align: end; }
    tr.inactif td { opacity: .55; }
    tr.selection td { background: var(--brand-soft); }
    .link-button { border: 0; background: none; color: var(--brand-dark); font: inherit; font-size: var(--fs-sm); font-weight: 700; cursor: pointer; padding: 4px 6px; }
    .link-button.danger { color: var(--danger, #c0392b); }
    .message.erreur { color: var(--danger, #c0392b); }
    @media (max-width: 1180px) { .tarifs-layout, .regle { grid-template-columns: minmax(0, 1fr); } }
  `]
})
export class TarifsComponent {
  private readonly service = inject(CatalogueActeService);
  private readonly builder = inject(FormBuilder);

  readonly types = TYPES_TARIF;
  readonly form = this.builder.nonNullable.group({
    libelle: ['', [Validators.required, Validators.maxLength(150)]],
    type: ['CONSULTATION', Validators.required],
    specialite: [''],
    montantDefaut: [0, [Validators.required, Validators.min(0)]],
    description: ['', Validators.maxLength(255)],
    actif: [true]
  });
  tarifs: CatalogueActe[] = [];
  specialites: string[] = [];
  regle: RegleControleGratuit | null = null;
  enEdition: CatalogueActe | null = null;
  filtreType = '';
  loading = true;
  saving = false;
  enregistrementRegle = false;
  message = '';
  messageRegle = '';
  erreur = false;

  constructor() {
    this.charger();
    this.service.regleControle().subscribe({ next: (regle) => { this.regle = regle; } });
    inject(RendezVousService).doctors().subscribe({
      next: (medecins: Medecin[]) => {
        this.specialites = [...new Set(medecins.map((medecin) => medecin.specialite).filter((s): s is string => !!s))].sort();
      }
    });
  }

  get tarifsFiltres(): CatalogueActe[] { return this.tarifs.filter((tarif) => !this.filtreType || tarif.type === this.filtreType); }
  get actifs(): number { return this.tarifs.filter((tarif) => tarif.actif).length; }

  charger(): void {
    this.loading = true;
    this.service.list(true).subscribe({
      next: (tarifs) => { this.tarifs = tarifs; this.loading = false; },
      error: (erreur) => { this.afficher(messageErreur(erreur, 'Les tarifs sont indisponibles.'), true); this.loading = false; }
    });
  }

  editer(tarif: CatalogueActe): void {
    this.enEdition = tarif;
    this.form.reset({
      libelle: tarif.libelle, type: tarif.type, specialite: tarif.specialite ?? '', montantDefaut: Number(tarif.montantDefaut),
      description: tarif.description ?? '', actif: tarif.actif
    });
  }

  annulerEdition(): void {
    this.enEdition = null;
    this.form.reset({ libelle: '', type: 'CONSULTATION', specialite: '', montantDefaut: 0, description: '', actif: true });
  }

  enregistrer(): void {
    if (this.form.invalid) { this.form.markAllAsTouched(); return; }
    const valeur = this.form.getRawValue();
    const requete = { ...valeur, specialite: valeur.type === 'CONSULTATION' ? valeur.specialite || null : null };
    this.saving = true;
    const appel = this.enEdition ? this.service.update(this.enEdition.id, requete) : this.service.create(requete);
    appel.subscribe({
      next: (tarif) => {
        this.afficher(`Tarif « ${tarif.libelle} » enregistré : ${this.format(tarif.montantDefaut)}.`, false);
        this.annulerEdition();
        this.saving = false;
        this.charger();
      },
      error: (erreur) => { this.afficher(messageErreur(erreur, 'L’enregistrement du tarif a échoué.'), true); this.saving = false; }
    });
  }

  basculer(tarif: CatalogueActe): void {
    this.service.update(tarif.id, { ...tarif, actif: !tarif.actif }).subscribe({
      next: () => { this.afficher(`« ${tarif.libelle} » ${tarif.actif ? 'désactivé' : 'activé'}.`, false); this.charger(); },
      error: (erreur) => this.afficher(messageErreur(erreur, 'Modification impossible.'), true)
    });
  }

  supprimer(tarif: CatalogueActe): void {
    if (!confirm(`Supprimer le tarif « ${tarif.libelle} » ?`)) return;
    this.service.delete(tarif.id).subscribe({
      next: () => { this.afficher(`Tarif « ${tarif.libelle} » supprimé.`, false); this.charger(); },
      error: (erreur) => this.afficher(messageErreur(erreur, 'Suppression impossible.'), true)
    });
  }

  enregistrerRegle(): void {
    if (!this.regle) return;
    this.enregistrementRegle = true;
    this.service.modifierRegleControle(this.regle).subscribe({
      next: (regle) => {
        this.regle = regle;
        this.messageRegle = regle.actif
          ? `Règle enregistrée : ${regle.nombre} contrôle(s) gratuit(s) pendant ${regle.jours} jours après une consultation payée.`
          : 'Règle enregistrée : le contrôle gratuit est désactivé.';
        this.enregistrementRegle = false;
      },
      error: (erreur) => { this.messageRegle = messageErreur(erreur, 'Enregistrement impossible.'); this.enregistrementRegle = false; }
    });
  }

  format(valeur: number): string { return new Intl.NumberFormat('fr-FR').format(Number(valeur)) + ' MRU'; }

  typeLabel(type: string): string { return type.charAt(0) + type.slice(1).toLowerCase(); }

  private afficher(texte: string, erreur: boolean): void { this.message = texte; this.erreur = erreur; }
}
