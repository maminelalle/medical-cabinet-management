import { RouterLink } from '@angular/router';
import { Component, inject } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Facture, StatutFacture } from '../../core/models/facture';
import { FactureService } from '../../core/factures/facture.service';
import { AuthService } from '../../core/auth/auth.service';

@Component({ selector: 'app-factures', standalone: true, imports: [FormsModule, RouterLink], templateUrl: './factures.component.html', styleUrl: './factures.component.css' })
export class FacturesComponent {
  private readonly service = inject(FactureService);
  private readonly auth = inject(AuthService);
  readonly canCreate = this.auth.role() === 'ACCUEIL';
  readonly statuses: StatutFacture[] = ['EN_ATTENTE', 'PARTIELLE', 'PAYEE', 'ANNULEE'];
  invoices: Facture[] = [];
  selected: Facture | null = null;
  status = '';
  paymentAmount: number | null = null;
  paymentMethod = 'ESPECES';
  loading = true;
  saving = false;
  message = '';

  constructor() { this.load(); }
  load(): void { this.loading = true; this.service.list(this.status as StatutFacture || undefined).subscribe({ next: (items) => { this.invoices = items; if (this.selected) this.selected = items.find((item) => item.id === this.selected?.id) ?? null; }, error: () => this.message = 'Impossible de charger les factures.', complete: () => this.loading = false }); }
  select(invoice: Facture): void { this.selected = invoice; this.paymentAmount = invoice.resteAPayer > 0 ? invoice.resteAPayer : null; this.message = ''; }
  pay(): void { if (!this.selected || !this.paymentAmount || this.paymentAmount <= 0) return; this.saving = true; this.service.pay(this.selected.id, this.paymentAmount, this.paymentMethod).subscribe({ next: (invoice) => { this.selected = invoice; this.paymentAmount = invoice.resteAPayer > 0 ? invoice.resteAPayer : null; this.message = 'Paiement enregistré avec succès.'; this.load(); }, error: () => this.message = 'Le paiement n’a pas pu être enregistré.', complete: () => this.saving = false }); }
  format(value: number): string { return new Intl.NumberFormat('fr-FR').format(value) + ' MRU'; }
  label(status: string): string { return status === 'PAYEE' ? 'Payée' : status === 'PARTIELLE' ? 'Partielle' : status === 'ANNULEE' ? 'Annulée' : 'En attente'; }
}