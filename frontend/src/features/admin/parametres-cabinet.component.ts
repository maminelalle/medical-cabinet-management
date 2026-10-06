import { Component, inject } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { AdminService } from '../../core/admin/admin.service';
import { messageErreur } from '../../core/http/erreur-api';
import { ParametresCabinet } from '../../core/models/admin';

/** Coordonnees du cabinet imprimees sur les factures, recus, ordonnances et dossiers. */
@Component({
  selector: 'app-admin-parametres-cabinet',
  standalone: true,
  imports: [FormsModule],
  template: `
    <section class="admin-page">
      <header class="page-heading">
        <div><p class="breadcrumb">ADMINISTRATION / CABINET</p><h1>Coordonnées du cabinet</h1><p class="subtitle">Imprimées en en-tête des factures, reçus, ordonnances, tickets et dossiers (écran et PDF).</p></div>
      </header>
      @if (message) { <div class="alert success"><span>✓</span><div><strong>{{ message }}</strong></div></div> }
      @if (erreur) { <div class="alert danger"><span>×</span><div><strong>Action impossible</strong>{{ erreur }}</div></div> }
      <section class="card form-card">
        <div class="form-quatre">
          <label>Nom du cabinet *<input [(ngModel)]="parametres.nom"></label>
          <label>Sous-titre<input [(ngModel)]="parametres.sousTitre" placeholder="Cabinet de groupe"></label>
          <label>Téléphone<input [(ngModel)]="parametres.telephone"></label>
          <label>Email<input type="email" [(ngModel)]="parametres.email"></label>
        </div>
        <label>Adresse<input [(ngModel)]="parametres.adresse" placeholder="Rue, quartier, ville"></label>
        <div class="apercu">
          <strong>{{ parametres.nom || 'Nom du cabinet' }}</strong>
          <small>{{ [parametres.sousTitre, parametres.adresse, parametres.telephone ? 'Tél. ' + parametres.telephone : '', parametres.email].filter(estRenseigne).join(' · ') }}</small>
        </div>
        <div class="actions-ligne"><button class="primary-button" type="button" (click)="enregistrer()" [disabled]="saving">Enregistrer</button></div>
      </section>
    </section>
  `,
  styles: [`.apercu { display: grid; gap: 4px; padding: 16px; border-bottom: 2px solid var(--text-strong); } .apercu strong { font-size: 20px; color: var(--text-strong); } .apercu small { color: var(--text-muted); }`],
  styleUrl: './admin.css'
})
export class ParametresCabinetComponent {
  private readonly service = inject(AdminService);
  parametres: ParametresCabinet = { nom: '' };
  saving = false;
  message = '';
  erreur = '';

  constructor() { this.service.parametresCabinet().subscribe({ next: (p) => this.parametres = { ...p } }); }

  estRenseigne(valeur: string | null | undefined): boolean { return !!valeur && !!valeur.trim(); }

  enregistrer(): void {
    if (!this.parametres.nom?.trim()) { this.erreur = 'Le nom du cabinet est obligatoire.'; return; }
    this.saving = true;
    this.erreur = '';
    this.service.modifierParametresCabinet(this.parametres).subscribe({
      next: (p) => { this.parametres = { ...p }; this.message = 'Coordonnées enregistrées : elles apparaissent sur tous les documents.'; this.saving = false; },
      error: (response) => { this.erreur = messageErreur(response, 'Enregistrement impossible.'); this.saving = false; }
    });
  }
}
