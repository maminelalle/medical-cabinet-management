import { Component, inject } from '@angular/core';
import { TraductionService } from '../../core/i18n/traduction.service';

/** Bouton de bascule francais / arabe (barre du haut et page de connexion). */
@Component({
  selector: 'app-choix-langue',
  standalone: true,
  template: `
    <button type="button" class="choix-langue sans-traduction" (click)="traduction.basculer()"
            [attr.aria-label]="traduction.langue() === 'ar' ? 'Passer en français' : 'التبديل إلى العربية'"
            [attr.lang]="traduction.langue() === 'ar' ? 'fr' : 'ar'">
      <svg viewBox="0 0 24 24" aria-hidden="true"><circle cx="12" cy="12" r="9" /><path d="M3 12h18M12 3c2.5 2.6 3.7 5.6 3.7 9s-1.2 6.4-3.7 9c-2.5-2.6-3.7-5.6-3.7-9S9.5 5.6 12 3Z" /></svg>
      {{ traduction.langue() === 'ar' ? 'Français' : 'العربية' }}
    </button>
  `,
  styles: [`
    .choix-langue { display: inline-flex; align-items: center; gap: 6px; padding: 8px 12px; border: 1px solid var(--border); border-radius: 999px; background: var(--bg-surface); color: var(--text-strong); font: inherit; font-size: var(--fs-sm); font-weight: 700; cursor: pointer; white-space: nowrap; }
    .choix-langue:hover { border-color: var(--brand); color: var(--brand-dark); }
    .choix-langue svg { width: 16px; height: 16px; fill: none; stroke: currentColor; stroke-width: 1.8; }
    .choix-langue[lang='ar'] { font-family: 'Cairo', var(--font-sans); }
  `]
})
export class ChoixLangueComponent {
  readonly traduction = inject(TraductionService);
}
