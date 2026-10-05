import { Component, inject } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { AuthService } from '../../core/auth/auth.service';
import { CatalogueActeService } from '../../core/catalogue/catalogue-acte.service';
import { CatalogueActe } from '../../core/models/catalogue';

@Component({
  selector: 'app-catalogue',
  standalone: true,
  imports: [ReactiveFormsModule],
  templateUrl: './catalogue.component.html',
  styleUrl: './catalogue.component.css'
})
export class CatalogueComponent {
  private readonly service = inject(CatalogueActeService);
  private readonly auth = inject(AuthService);
  private readonly builder = inject(FormBuilder);

  readonly peutCreer = this.auth.role() === 'DIRECTION';
  readonly types = ['CONSULTATION', 'HOSPITALISATION', 'CHIRURGIE', 'LABORATOIRE', 'IMAGERIE', 'PHARMACIE', 'SOINS'];
  readonly form = this.builder.nonNullable.group({
    libelle: ['', [Validators.required, Validators.maxLength(150)]],
    type: ['CONSULTATION', Validators.required],
    montantDefaut: [0, [Validators.required, Validators.min(0)]]
  });
  actes: CatalogueActe[] = [];
  loading = true;
  saving = false;
  message = '';

  constructor() { this.charger(); }

  charger(): void {
    this.loading = true;
    this.service.list().subscribe({
      next: (actes) => { this.actes = actes; },
      error: () => { this.message = 'Le catalogue des actes est indisponible.'; },
      complete: () => { this.loading = false; }
    });
  }

  creer(): void {
    if (this.form.invalid) { this.form.markAllAsTouched(); return; }
    this.saving = true;
    this.message = '';
    this.service.create(this.form.getRawValue()).subscribe({
      next: (acte) => {
        this.actes = [...this.actes, acte];
        this.form.reset({ libelle: '', type: 'CONSULTATION', montantDefaut: 0 });
        this.message = `Acte « ${acte.libelle} » ajouté au catalogue.`;
      },
      error: () => { this.message = 'L’ajout de l’acte a échoué.'; },
      complete: () => { this.saving = false; }
    });
  }

  get nombreTypes(): number { return new Set(this.actes.map((acte) => acte.type)).size; }
  get montantMoyen(): number {
    if (!this.actes.length) return 0;
    return this.actes.reduce((total, acte) => total + Number(acte.montantDefaut), 0) / this.actes.length;
  }

  format(valeur: number): string { return new Intl.NumberFormat('fr-FR').format(valeur) + ' MRU'; }

  typeLabel(type: string): string {
    return type.replace('_', ' ').toLowerCase().replace(/^\w/, (lettre) => lettre.toUpperCase());
  }

  classeType(type: string): string {
    switch (type) {
      case 'CONSULTATION': return 'planifie';
      case 'HOSPITALISATION': return 'violet';
      case 'CHIRURGIE': return 'en_cours';
      case 'LABORATOIRE': return 'info';
      case 'IMAGERIE': return 'success';
      case 'PHARMACIE': return 'violet';
      default: return 'neutral';
    }
  }
}