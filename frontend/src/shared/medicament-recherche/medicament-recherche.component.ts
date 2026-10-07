import { Component, EventEmitter, Input, Output } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Medicament } from '../../core/models/pharmacie';

export interface ChoixMedicament { medicamentId: number | null; medicament: string; }

/**
 * Recherche d'un medicament par nom, dosage ou famille, avec la disponibilite en stock.
 * Un produit absent du stock peut etre prescrit en saisie libre.
 */
@Component({
  selector: 'app-medicament-recherche',
  standalone: true,
  imports: [FormsModule],
  template: `
    <div class="recherche" (focusout)="fermerPlusTard()">
      <input [(ngModel)]="texte" (focus)="ouvert = true" (input)="ouvert = true; choixLibre()" [placeholder]="placeholder"
             autocomplete="off" [attr.aria-label]="placeholder">
      @if (selection) {
        <span class="etat" [class]="'etat ' + classeStock(selection)">{{ selection.stockActuel > 0 ? selection.stockActuel + ' en stock' : 'Rupture' }}</span>
      } @else if (texte.trim()) {
        <span class="etat libre">Hors stock</span>
      }
      @if (ouvert && texte.trim().length >= 1) {
        <div class="resultats" role="listbox">
          @for (groupe of groupes; track groupe.famille) {
            <p class="famille">{{ groupe.famille }}</p>
            @for (item of groupe.items; track item.id) {
              <button type="button" (mousedown)="choisir(item)">
                <span><strong>{{ item.nom }} {{ item.dosage || '' }}</strong><small>{{ item.forme || '' }}</small></span>
                <span class="etat" [class]="'etat ' + classeStock(item)">{{ item.stockActuel > 0 ? item.stockActuel + ' dispo.' : 'Rupture' }}</span>
              </button>
            }
          } @empty {
            <p class="vide">Aucun produit du stock ne correspond. Le nom saisi sera prescrit tel quel.</p>
          }
        </div>
      }
    </div>
  `,
  styles: [`
    .recherche { position: relative; }
    .recherche input { padding-inline-end: 96px; }
    .recherche > .etat { position: absolute; top: 50%; inset-inline-end: 8px; transform: translateY(-50%); }
    .etat { padding: 3px 8px; border-radius: var(--radius-pill); font-size: 11px; font-weight: 800; white-space: nowrap; }
    .etat.ok { background: var(--accent-soft); color: var(--accent-text); }
    .etat.bas { background: var(--warn-soft); color: var(--warn-text); }
    .etat.rupture { background: var(--danger-soft); color: var(--danger-text); }
    .etat.libre { background: var(--bg-hover); color: var(--text-muted); }
    .resultats { position: absolute; z-index: 30; top: calc(100% + 4px); inset-inline-start: 0; inset-inline-end: 0; max-height: 300px; overflow-y: auto; padding: 6px; border: 1px solid var(--border); border-radius: var(--radius-md); background: var(--bg-surface); box-shadow: var(--shadow-pop); }
    .famille { margin: 6px 8px 4px; color: var(--text-soft); font-size: 10px; font-weight: 800; letter-spacing: .06em; text-transform: uppercase; }
    .resultats button { display: flex; align-items: center; justify-content: space-between; gap: 10px; width: 100%; padding: 8px 10px; border: 0; border-radius: var(--radius-sm); background: transparent; font: inherit; text-align: start; cursor: pointer; }
    .resultats button:hover { background: var(--brand-soft); }
    .resultats strong { display: block; color: var(--text-strong); font-size: var(--fs-sm); }
    .resultats small { color: var(--text-soft); font-size: var(--fs-label); }
    .vide { margin: 6px 8px; color: var(--text-muted); font-size: var(--fs-xs); }
  `]
})
export class MedicamentRechercheComponent {
  @Input() medicaments: Medicament[] = [];
  @Input() placeholder = 'Nom, dosage ou famille du médicament...';
  @Input() set valeur(choix: ChoixMedicament | null) {
    this.selection = choix?.medicamentId ? this.medicaments.find((item) => item.id === choix.medicamentId) ?? null : null;
    this.texte = choix?.medicament ?? '';
  }
  @Output() readonly choix = new EventEmitter<ChoixMedicament>();

  texte = '';
  ouvert = false;
  selection: Medicament | null = null;

  /** Resultats regroupes par famille therapeutique. */
  get groupes(): { famille: string; items: Medicament[] }[] {
    const terme = this.normaliser(this.texte);
    const trouves = this.medicaments.filter((item) =>
      this.normaliser(`${item.nom} ${item.dosage ?? ''} ${item.famille ?? ''} ${item.forme ?? ''}`).includes(terme)).slice(0, 25);
    const groupes = new Map<string, Medicament[]>();
    trouves.forEach((item) => {
      const famille = item.famille || 'Sans famille';
      groupes.set(famille, [...(groupes.get(famille) ?? []), item]);
    });
    return [...groupes.entries()].map(([famille, items]) => ({ famille, items }));
  }

  choisir(item: Medicament): void {
    this.selection = item;
    this.texte = `${item.nom}${item.dosage ? ' ' + item.dosage : ''}`;
    this.ouvert = false;
    this.choix.emit({ medicamentId: item.id, medicament: this.texte });
  }

  /** Saisie libre : le medicament n'est pas relie au stock. */
  choixLibre(): void {
    this.selection = null;
    this.choix.emit({ medicamentId: null, medicament: this.texte.trim() });
  }

  fermerPlusTard(): void { setTimeout(() => this.ouvert = false, 150); }

  classeStock(item: Medicament): string {
    return item.stockActuel === 0 ? 'rupture' : item.stockActuel <= item.seuilAlerte ? 'bas' : 'ok';
  }

  private normaliser(valeur: string): string { return valeur.toLowerCase().normalize('NFD').replace(/[̀-ͯ]/g, '').trim(); }
}
