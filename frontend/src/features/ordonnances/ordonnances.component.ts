import { Component, inject } from '@angular/core';
import { DatePipe } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { AuthService } from '../../core/auth/auth.service';
import { Ordonnance } from '../../core/models/ordonnance';
import { OrdonnanceService } from '../../core/ordonnances/ordonnance.service';

@Component({
  selector: 'app-ordonnances',
  standalone: true,
  imports: [DatePipe, FormsModule, RouterLink],
  templateUrl: './ordonnances.component.html',
  styleUrl: './ordonnances.component.css'
})
export class OrdonnancesComponent {
  private readonly service = inject(OrdonnanceService);
  private readonly role = inject(AuthService).role();
  readonly isMedecin = this.role === 'MEDECIN';
  readonly isPharmacien = this.role === 'PHARMACIEN';
  readonly peutOuvrirDossier = this.role !== 'PHARMACIEN';
  ordonnances: Ordonnance[] = [];
  selection: Ordonnance | null = null;
  recherche = '';
  statut: '' | 'delivree' | 'a-delivrer' = '';
  loading = true;
  error = '';

  constructor() { this.charger(); }

  charger(): void {
    this.loading = true;
    this.error = '';
    this.service.list().subscribe({
      next: (items) => { this.ordonnances = items; this.selection = this.selection ? items.find((item) => item.id === this.selection?.id) ?? null : null; this.loading = false; },
      error: () => { this.error = 'Les ordonnances sont momentanément indisponibles.'; this.loading = false; }
    });
  }

  get filtrees(): Ordonnance[] {
    const terme = this.recherche.trim().toLowerCase();
    return this.ordonnances.filter((item) =>
      (!terme || `${item.patientPrenom} ${item.patientNom} ${item.medecinNom} ${item.lignes.map((ligne) => ligne.medicament).join(' ')}`.toLowerCase().includes(terme))
      && (this.statut === '' || (this.statut === 'delivree') === item.delivree));
  }
  get nombreADelivrer(): number { return this.ordonnances.filter((item) => !item.delivree).length; }
  get nombreDuMois(): number {
    const mois = new Date().toISOString().slice(0, 7);
    return this.ordonnances.filter((item) => item.datePrescription.startsWith(mois)).length;
  }
  get breadcrumb(): string { return this.isMedecin ? 'MÉDECIN' : this.isPharmacien ? 'PHARMACIE' : this.role === 'DIRECTION' ? 'DIRECTION' : 'ACCUEIL'; }

  medicaments(ordonnance: Ordonnance): string { return ordonnance.lignes.map((ligne) => ligne.medicament).join(', '); }
}
