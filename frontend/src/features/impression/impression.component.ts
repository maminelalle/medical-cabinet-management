import { Component, inject } from '@angular/core';
import { DatePipe, DecimalPipe, Location } from '@angular/common';
import { ActivatedRoute } from '@angular/router';
import { Facture } from '../../core/models/facture';
import { RendezVous } from '../../core/models/rendez-vous';
import { DossierPatient } from '../../core/models/consultation';
import { Ordonnance } from '../../core/models/ordonnance';
import { FactureService } from '../../core/factures/facture.service';
import { RendezVousService } from '../../core/rendez-vous/rendez-vous.service';
import { ConsultationService } from '../../core/consultations/consultation.service';
import { OrdonnanceService } from '../../core/ordonnances/ordonnance.service';
import { environment } from '../../environments/environment';

type TypeDocument = 'facture' | 'recu' | 'rendez-vous' | 'ordonnance' | 'dossier';

@Component({
  selector: 'app-impression',
  standalone: true,
  imports: [DatePipe, DecimalPipe],
  templateUrl: './impression.component.html',
  styleUrl: './impression.component.css'
})
export class ImpressionComponent {
  private readonly route = inject(ActivatedRoute);
  private readonly location = inject(Location);
  private readonly factureService = inject(FactureService);
  private readonly rendezVousService = inject(RendezVousService);
  private readonly consultationService = inject(ConsultationService);
  private readonly ordonnanceService = inject(OrdonnanceService);

  readonly type = this.route.snapshot.data['type'] as TypeDocument;
  readonly id = Number(this.route.snapshot.paramMap.get('id'));
  readonly today = new Date();
  readonly cabinet = environment.cabinet;
  facture: Facture | null = null;
  rendezVous: RendezVous | null = null;
  ordonnance: Ordonnance | null = null;
  dossier: DossierPatient | null = null;
  loading = true;
  error = '';

  constructor() { this.charger(); }

  private charger(): void {
    const fin = () => { this.loading = false; };
    const echec = (message: string) => () => { this.error = message; this.loading = false; };
    switch (this.type) {
      case 'facture':
      case 'recu':
        this.factureService.get(this.id).subscribe({ next: (item) => { this.facture = item; fin(); }, error: echec('Facture introuvable.') });
        break;
      case 'ordonnance':
        this.ordonnanceService.get(this.id).subscribe({ next: (item) => { this.ordonnance = item; fin(); }, error: echec('Ordonnance introuvable ou non autorisée.') });
        break;
      case 'dossier':
        this.consultationService.dossier(this.id).subscribe({ next: (item) => { this.dossier = item; fin(); }, error: echec('Dossier patient introuvable ou non autorisé.') });
        break;
      default:
        this.rendezVousService.get(this.id).subscribe({ next: (item) => { this.rendezVous = item; fin(); }, error: echec('Rendez-vous introuvable.') });
    }
  }

  imprimer(): void { window.print(); }
  fermer(): void { this.location.back(); }

  get titre(): string {
    switch (this.type) {
      case 'facture': return 'Facture';
      case 'recu': return 'Reçu de paiement';
      case 'ordonnance': return 'Ordonnance médicale';
      case 'dossier': return 'Dossier patient';
      default: return 'Reçu de rendez-vous';
    }
  }

  reference(prefixe: string, id: number): string { return `${prefixe}-${id.toString().padStart(4, '0')}`; }

  age(dateNaissance?: string): number | null {
    if (!dateNaissance) return null;
    return Math.floor((Date.now() - new Date(dateNaissance).getTime()) / 31557600000);
  }

  statutFacture(statut: string): string {
    return statut === 'PAYEE' ? 'Payée' : statut === 'PARTIELLE' ? 'Partiellement payée' : statut === 'ANNULEE' ? 'Annulée' : 'En attente de paiement';
  }
  statutRendezVous(statut: string): string { return statut.replace('_', ' ').toLowerCase().replace(/^\w/, (lettre) => lettre.toUpperCase()); }
  moyen(moyen?: string): string {
    return moyen === 'ESPECES' ? 'Espèces' : moyen === 'CARTE' ? 'Carte bancaire' : moyen === 'VIREMENT' ? 'Virement' : moyen || 'Non précisé';
  }
}
