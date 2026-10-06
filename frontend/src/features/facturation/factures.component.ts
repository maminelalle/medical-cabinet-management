import { Component, inject } from '@angular/core';
import { DatePipe } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { AuthService } from '../../core/auth/auth.service';
import { FactureService } from '../../core/factures/facture.service';
import { messageErreur } from '../../core/http/erreur-api';
import { Facture, MOYENS_PAIEMENT, libelleMoyen, libelleOrigine } from '../../core/models/facture';
import { Ordonnance } from '../../core/models/ordonnance';
import { OrdonnanceService } from '../../core/ordonnances/ordonnance.service';

type Onglet = 'a-payer' | 'payees' | 'annulees';

/**
 * Facturation : les factures a payer d'un cote, les factures payees de l'autre (plus les annulees),
 * le detail avec ses paiements, l'encaissement avec reference et les impressions.
 */
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
  readonly moyens = MOYENS_PAIEMENT;
  readonly libelleMoyen = libelleMoyen;
  readonly libelleOrigine = libelleOrigine;

  invoices: Facture[] = [];
  onglet: Onglet = 'a-payer';
  selected: Facture | null = null;
  ordonnances: Ordonnance[] = [];
  ordonnanceId: number | null = null;
  paymentAmount: number | null = null;
  paymentMethod = 'ESPECES';
  paymentReference = '';
  loading = true;
  saving = false;
  message = '';
  erreur = '';
  annulationOuverte = false;
  motifAnnulation = '';

  constructor() {
    const factureId = Number(this.route.snapshot.queryParamMap.get('id')) || null;
    this.load(factureId);
  }

  load(selectionId: number | null = null): void {
    this.loading = true;
    this.service.list().subscribe({
      next: (items) => {
        this.invoices = this.isPharmacien ? items.filter((item) => item.origine === 'PHARMACIE' || item.lignes.some((ligne) => ligne.typeActe === 'PHARMACIE')) : items;
        const cible = selectionId ?? this.selected?.id;
        const trouvee = cible ? this.invoices.find((item) => item.id === cible) ?? null : null;
        if (trouvee) {
          this.onglet = this.ongletDe(trouvee);
          if (trouvee.id !== this.selected?.id) this.select(trouvee); else this.selected = trouvee;
        } else {
          this.selected = null;
        }
        this.loading = false;
      },
      error: () => { this.erreur = 'Impossible de charger les factures.'; this.loading = false; }
    });
  }

  private ongletDe(facture: Facture): Onglet {
    return facture.statut === 'ANNULEE' ? 'annulees' : facture.statut === 'PAYEE' ? 'payees' : 'a-payer';
  }

  get aPayer(): Facture[] { return this.invoices.filter((item) => item.statut === 'EN_ATTENTE' || item.statut === 'PARTIELLE'); }
  get payees(): Facture[] { return this.invoices.filter((item) => item.statut === 'PAYEE'); }
  get annulees(): Facture[] { return this.invoices.filter((item) => item.statut === 'ANNULEE'); }
  get facturesAffichees(): Facture[] { return this.onglet === 'a-payer' ? this.aPayer : this.onglet === 'payees' ? this.payees : this.annulees; }
  get totalAPayer(): number { return this.aPayer.reduce((total, item) => total + Number(item.resteAPayer), 0); }
  get totalPaye(): number { return this.payees.reduce((total, item) => total + Number(item.montantTotal), 0); }

  changerOnglet(onglet: Onglet): void { this.onglet = onglet; this.selected = null; this.message = ''; this.erreur = ''; }

  select(invoice: Facture): void {
    this.selected = invoice;
    this.paymentAmount = invoice.resteAPayer > 0 ? invoice.resteAPayer : null;
    this.paymentMethod = 'ESPECES';
    this.paymentReference = '';
    this.message = '';
    this.erreur = '';
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

  get referenceObligatoire(): boolean { return this.paymentMethod !== 'ESPECES'; }

  pay(): void {
    if (!this.selected || !this.paymentAmount || this.paymentAmount <= 0) { this.erreur = 'Saisissez le montant encaissé.'; return; }
    if (this.referenceObligatoire && !this.paymentReference.trim()) { this.erreur = 'La référence de la transaction est obligatoire pour ' + libelleMoyen(this.paymentMethod) + '.'; return; }
    this.saving = true;
    this.erreur = '';
    this.service.pay(this.selected.id, { montant: this.paymentAmount, moyenPaiement: this.paymentMethod, reference: this.paymentReference.trim() || undefined }).subscribe({
      next: (invoice) => {
        this.saving = false;
        this.message = `Paiement de ${this.format(this.paymentAmount ?? 0)} enregistré (${libelleMoyen(this.paymentMethod)}). Vous pouvez imprimer le reçu.`;
        this.selected = invoice;
        this.load(invoice.id);
      },
      error: (response) => { this.erreur = messageErreur(response, 'Le paiement n’a pas pu être enregistré.'); this.saving = false; }
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
      next: (invoice) => { this.fermerAnnulation(); this.message = 'Facture annulée.'; this.saving = false; this.load(invoice.id); },
      error: (response) => { this.erreur = messageErreur(response, 'L’annulation a échoué.'); this.saving = false; }
    });
  }

  format(value: number): string { return new Intl.NumberFormat('fr-FR').format(value) + ' MRU'; }
  label(status: string): string { return status === 'PAYEE' ? 'Payée' : status === 'PARTIELLE' ? 'Partielle' : status === 'ANNULEE' ? 'Annulée' : 'En attente'; }
}
