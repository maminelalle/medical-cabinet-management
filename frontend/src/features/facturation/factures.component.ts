import { RouterLink } from '@angular/router';
import { Component, inject } from '@angular/core';
import { DatePipe } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute } from '@angular/router';
import { Facture, StatutFacture } from '../../core/models/facture';
import { Ordonnance } from '../../core/models/ordonnance';
import { FactureService } from '../../core/factures/facture.service';
import { OrdonnanceService } from '../../core/ordonnances/ordonnance.service';
import { AuthService } from '../../core/auth/auth.service';
import { messageErreur } from '../../core/http/erreur-api';

@Component({ selector: 'app-factures', standalone: true, imports: [DatePipe, FormsModule, RouterLink], templateUrl: './factures.component.html', styleUrl: './factures.component.css' })
export class FacturesComponent {
  private readonly service = inject(FactureService);
  private readonly ordonnanceService = inject(OrdonnanceService);
  private readonly auth = inject(AuthService);
  private readonly route = inject(ActivatedRoute);
  readonly canCreate = this.auth.role() === 'ACCUEIL';
  readonly canPay = this.auth.role() === 'ACCUEIL' || this.auth.role() === 'PHARMACIEN';
  readonly isPharmacien = this.auth.role() === 'PHARMACIEN';
  readonly peutOuvrirDossier = this.auth.role() !== 'PHARMACIEN';
  readonly statuses: StatutFacture[] = ['EN_ATTENTE', 'PARTIELLE', 'PAYEE', 'ANNULEE'];
  invoices: Facture[] = [];
  selected: Facture | null = null;
  ordonnances: Ordonnance[] = [];
  ordonnanceId: number | null = null;
  status = '';
  recherche = '';
  paymentAmount: number | null = null;
  paymentMethod = 'ESPECES';
  loading = true;
  saving = false;
  message = '';
  annulationOuverte = false;
  motifAnnulation = '';

  constructor() {
    const factureId = Number(this.route.snapshot.queryParamMap.get('id')) || null;
    this.load(factureId);
  }

  load(selectionId: number | null = null): void {
    this.loading = true;
    this.service.list(this.status as StatutFacture || undefined).subscribe({
      next: (items) => {
        this.invoices = this.isPharmacien ? items.filter((item) => item.lignes.some((ligne) => ligne.typeActe === 'PHARMACIE')) : items;
        this.invoices.sort((a, b) => b.dateFacture.localeCompare(a.dateFacture) || b.id - a.id);
        const cible = selectionId ?? this.selected?.id;
        const trouvee = cible ? this.invoices.find((item) => item.id === cible) ?? null : null;
        if (trouvee && trouvee.id !== this.selected?.id) this.select(trouvee); else this.selected = trouvee;
      },
      error: () => this.message = 'Impossible de charger les factures.',
      complete: () => this.loading = false
    });
  }

  get facturesAffichees(): Facture[] {
    const terme = this.recherche.trim().toLowerCase();
    if (!terme) return this.invoices;
    return this.invoices.filter((item) => `${item.patientPrenom} ${item.patientNom} fac-${item.id.toString().padStart(3, '0')}`.toLowerCase().includes(terme));
  }

  select(invoice: Facture): void {
    this.selected = invoice;
    this.paymentAmount = invoice.resteAPayer > 0 ? invoice.resteAPayer : null;
    this.message = '';
    this.fermerAnnulation();
    this.chargerOrdonnances(invoice);
  }

  /** Ordonnances du patient : la plus recente anterieure ou egale a la date de facture est proposee par defaut. */
  private chargerOrdonnances(invoice: Facture): void {
    this.ordonnances = [];
    this.ordonnanceId = null;
    this.ordonnanceService.list(invoice.patientId).subscribe({
      next: (items) => {
        if (this.selected?.id !== invoice.id) return;
        this.ordonnances = items;
        const avantFacture = items.find((item) => item.datePrescription <= invoice.dateFacture);
        this.ordonnanceId = (avantFacture ?? items[0])?.id ?? null;
      },
      error: () => this.ordonnances = []
    });
  }

  pay(): void {
    if (!this.selected || !this.paymentAmount || this.paymentAmount <= 0) return;
    this.saving = true;
    this.service.pay(this.selected.id, this.paymentAmount, this.paymentMethod).subscribe({
      next: (invoice) => { this.selected = invoice; this.paymentAmount = invoice.resteAPayer > 0 ? invoice.resteAPayer : null; this.message = 'Paiement enregistré avec succès. Vous pouvez imprimer le reçu.'; this.load(); },
      error: (response) => { this.message = messageErreur(response, 'Le paiement n’a pas pu être enregistré.'); this.saving = false; },
      complete: () => this.saving = false
    });
  }

  /** Une facture ne s'annule que si aucun paiement n'a ete encaisse (regle verifiee aussi par le serveur). */
  get peutAnnuler(): boolean {
    return this.canCreate && !!this.selected && this.selected.statut !== 'ANNULEE' && Number(this.selected.montantPaye) === 0;
  }

  ouvrirAnnulation(): void { this.annulationOuverte = true; this.motifAnnulation = ''; this.message = ''; }
  fermerAnnulation(): void { this.annulationOuverte = false; this.motifAnnulation = ''; }

  annuler(): void {
    const motif = this.motifAnnulation.trim();
    if (!this.selected || !motif) return;
    this.saving = true;
    this.service.annuler(this.selected.id, motif).subscribe({
      next: (invoice) => { this.selected = invoice; this.fermerAnnulation(); this.message = 'Facture annulée.'; this.saving = false; this.load(); },
      error: (response) => { this.message = messageErreur(response, 'L’annulation a échoué.'); this.saving = false; }
    });
  }

  format(value: number): string { return new Intl.NumberFormat('fr-FR').format(value) + ' MRU'; }
  label(status: string): string { return status === 'PAYEE' ? 'Payée' : status === 'PARTIELLE' ? 'Partielle' : status === 'ANNULEE' ? 'Annulée' : 'En attente'; }
  moyen(moyen?: string): string { return moyen === 'ESPECES' ? 'Espèces' : moyen === 'CARTE' ? 'Carte bancaire' : moyen === 'VIREMENT' ? 'Virement' : moyen || 'Autre'; }
}
