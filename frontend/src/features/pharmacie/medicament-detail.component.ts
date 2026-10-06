import { Component, inject } from '@angular/core';
import { DatePipe } from '@angular/common';
import { FormBuilder, FormsModule, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { AuthService } from '../../core/auth/auth.service';
import { messageErreur } from '../../core/http/erreur-api';
import { MedicamentDetail, libelleMouvement } from '../../core/models/pharmacie';
import { PharmacieService } from '../../core/pharmacie/pharmacie.service';

/** Fiche complete d'un medicament : informations, ventes, achats, mouvements, approvisionnement et inventaire. */
@Component({
  selector: 'app-medicament-detail',
  standalone: true,
  imports: [DatePipe, FormsModule, ReactiveFormsModule, RouterLink],
  templateUrl: './medicament-detail.component.html',
  styleUrl: './pharmacie.component.css'
})
export class MedicamentDetailComponent {
  private readonly service = inject(PharmacieService);
  private readonly builder = inject(FormBuilder);
  private readonly role = inject(AuthService).role();
  readonly id = Number(inject(ActivatedRoute).snapshot.paramMap.get('id'));
  readonly peutAdministrer = this.role === 'PHARMACIEN' || this.role === 'DIRECTION';
  readonly libelleMouvement = libelleMouvement;

  detail: MedicamentDetail | null = null;
  panneau: 'aucun' | 'achat' | 'inventaire' | 'modifier' = 'aucun';
  stockCompte = 0;
  commentaireInventaire = '';
  loading = true;
  saving = false;
  message = '';
  error = '';

  readonly achat = this.builder.nonNullable.group({
    quantite: [1, [Validators.required, Validators.min(1)]],
    prixAchatUnitaire: [0, [Validators.required, Validators.min(0)]],
    fournisseur: [''], reference: [''], dateExpiration: ['']
  });
  readonly fiche = this.builder.nonNullable.group({
    nom: ['', Validators.required], dosage: [''], forme: [''], famille: [''],
    seuilAlerte: [0, [Validators.required, Validators.min(0)]], prixAchat: [0, Validators.min(0)], prixVente: [0, Validators.min(0)],
    fournisseur: [''], dateExpiration: ['']
  });

  constructor() { this.charger(); }

  charger(): void {
    this.service.details(this.id).subscribe({
      next: (detail) => {
        this.detail = detail;
        const m = detail.medicament;
        this.stockCompte = m.stockActuel;
        this.achat.patchValue({ prixAchatUnitaire: m.prixAchat, fournisseur: m.fournisseur ?? '' });
        this.fiche.patchValue({ nom: m.nom, dosage: m.dosage ?? '', forme: m.forme ?? '', famille: m.famille ?? '', seuilAlerte: m.seuilAlerte,
          prixAchat: m.prixAchat, prixVente: m.prixVente, fournisseur: m.fournisseur ?? '', dateExpiration: m.dateExpiration ?? '' });
        this.loading = false;
      },
      error: () => { this.error = 'Médicament introuvable.'; this.loading = false; }
    });
  }

  ouvrir(panneau: 'achat' | 'inventaire' | 'modifier'): void { this.panneau = this.panneau === panneau ? 'aucun' : panneau; this.message = ''; this.error = ''; }

  approvisionner(): void {
    if (this.achat.invalid) { this.achat.markAllAsTouched(); return; }
    const valeur = this.achat.getRawValue();
    this.executer(this.service.approvisionner(this.id, { ...valeur, dateExpiration: valeur.dateExpiration || undefined }),
      `${valeur.quantite} unité(s) ajoutée(s) au stock.`);
  }

  corrigerStock(): void {
    this.executer(this.service.modifierStock(this.id, Number(this.stockCompte), this.commentaireInventaire.trim() || undefined), 'Inventaire corrigé.');
  }

  modifierFiche(): void {
    if (!this.detail || this.fiche.invalid) { this.fiche.markAllAsTouched(); return; }
    const valeur = this.fiche.getRawValue();
    this.executer(this.service.modifierMedicament(this.id, { ...valeur, stockActuel: this.detail.medicament.stockActuel, dateExpiration: valeur.dateExpiration || undefined }),
      'Fiche du médicament mise à jour.');
  }

  private executer(operation: ReturnType<PharmacieService['approvisionner']>, succes: string): void {
    this.saving = true;
    this.error = '';
    operation.subscribe({
      next: () => { this.message = succes; this.saving = false; this.panneau = 'aucun'; this.commentaireInventaire = ''; this.charger(); },
      error: (response) => { this.error = messageErreur(response, 'L’opération a échoué.'); this.saving = false; }
    });
  }

  format(value: number | null | undefined): string { return new Intl.NumberFormat('fr-FR').format(Number(value ?? 0)) + ' MRU'; }
  stockClasse(): string {
    const m = this.detail?.medicament;
    return !m ? '' : m.stockActuel === 0 ? 'critical' : m.stockActuel <= m.seuilAlerte ? 'warning' : 'ok';
  }
}
