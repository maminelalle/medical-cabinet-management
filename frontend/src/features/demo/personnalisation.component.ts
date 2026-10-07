import { Component, OnDestroy, inject } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { APPARENCE_PAR_DEFAUT, Apparence, ApparenceService } from '../../core/apparence/apparence.service';
import { AuthService } from '../../core/auth/auth.service';
import { messageErreur } from '../../core/http/erreur-api';

const TAILLE_MAX_LOGO = 300 * 1024;

/**
 * Personnalisation de l'interface par l'administrateur : nom, sous-titre, logo et couleurs.
 * Les changements s'affichent en apercu immediatement et s'appliquent a tous les postes une fois enregistres.
 */
@Component({
  selector: 'app-personnalisation',
  standalone: true,
  imports: [FormsModule],
  template: `
    <section class="card personnalisation">
      <div class="entete">
        <div>
          <h2>Personnalisation de l'interface</h2>
          <p>Nom, logo et couleurs de l'application, appliqués à tous les écrans et à la page de connexion.</p>
        </div>
        @if (!peutModifier) { <span class="badge neutral">Réservé à l'administrateur et à la direction</span> }
      </div>
      @if (!serveurAJour()) {
        <div class="alert warning"><span>!</span><div><strong>Serveur à redémarrer</strong>Le backend ne propose pas encore la personnalisation (migration V16). Arrêtez puis relancez le backend : l'aperçu fonctionne déjà, mais l'enregistrement échouera tant qu'il n'est pas redémarré.</div></div>
      }

      <div class="grille">
        <fieldset [disabled]="!peutModifier || enregistrement">
          <label>Nom de l'interface *
            <input [(ngModel)]="modele.nomInterface" (ngModelChange)="apercu()" maxlength="80" placeholder="Cabinets Médicaux">
          </label>
          <label>Sous-titre
            <input [(ngModel)]="modele.sousTitre" (ngModelChange)="apercu()" maxlength="150" placeholder="Cabinet de groupe">
          </label>

          <div class="couleurs">
            @for (champ of champsCouleur; track champ.cle) {
              <label class="couleur">
                <span>{{ champ.libelle }}</span>
                <span class="saisie-couleur">
                  <input type="color" [(ngModel)]="modele[champ.cle]" (ngModelChange)="apercu()" [attr.aria-label]="champ.libelle">
                  <input class="hex" [(ngModel)]="modele[champ.cle]" (ngModelChange)="apercu()" maxlength="7" pattern="#[0-9a-fA-F]{6}">
                </span>
                <small>{{ champ.aide }}</small>
              </label>
            }
          </div>

          <div class="logo">
            <span class="apercu-logo">
              @if (modele.logo) { <img [src]="modele.logo" alt="Logo"> } @else { <b>+</b> }
            </span>
            <div>
              <strong>Logo</strong>
              <small>PNG, JPEG, WEBP ou SVG, 300 Ko au plus. Affiché dans la barre latérale, la page de connexion et l'onglet du navigateur.</small>
              <div class="actions-logo">
                <label class="soft-button fichier">Choisir une image
                  <input type="file" accept="image/png,image/jpeg,image/webp,image/svg+xml" (change)="choisirLogo($event)">
                </label>
                @if (modele.logo) { <button type="button" class="link-button" (click)="modele.logo = null; apercu()">Retirer le logo</button> }
              </div>
            </div>
          </div>
        </fieldset>

        <aside class="previsualisation" aria-label="Aperçu">
          <small>Aperçu</small>
          <div class="mini-barre">
            <span class="mini-marque">@if (modele.logo) { <img [src]="modele.logo" alt=""> } @else { + }</span>
            <span><small>{{ modele.sousTitre }}</small><strong>{{ modele.nomInterface || 'Nom de l’interface' }}</strong></span>
          </div>
          <div class="mini-elements">
            <span class="mini-lien">Élément actif du menu</span>
            <button type="button" class="primary-button">Bouton principal</button>
            <span class="badge success">Payée</span>
            <a href="javascript:void(0)">Lien</a>
          </div>
        </aside>
      </div>

      @if (message) { <p class="message" [class.erreur]="erreur">{{ message }}</p> }
      @if (peutModifier) {
        <div class="form-actions">
          <button type="button" class="soft-button" (click)="reinitialiser()" [disabled]="enregistrement">Couleurs et nom par défaut</button>
          <button type="button" class="soft-button" (click)="annuler()" [disabled]="enregistrement">Annuler les changements</button>
          <button type="button" class="primary-button" (click)="enregistrer()" [disabled]="enregistrement">
            {{ enregistrement ? 'Enregistrement...' : 'Enregistrer et appliquer' }}
          </button>
        </div>
      }
    </section>
  `,
  styles: [`
    .personnalisation { display: grid; gap: 18px; }
    .entete { display: flex; justify-content: space-between; gap: 12px; align-items: start; }
    .entete h2 { margin: 0 0 4px; }
    .entete p { margin: 0; color: var(--text-muted); }
    .grille { display: grid; grid-template-columns: minmax(0, 1.5fr) minmax(260px, 1fr); gap: 20px; align-items: start; }
    fieldset { display: grid; gap: 14px; margin: 0; padding: 0; border: 0; min-width: 0; }
    .couleurs { display: grid; grid-template-columns: repeat(3, minmax(0, 1fr)); gap: 12px; }
    .couleur { display: grid; gap: 6px; }
    .couleur small, .logo small { color: var(--text-muted); font-size: var(--fs-xs); }
    .saisie-couleur { display: flex; gap: 6px; align-items: center; }
    .saisie-couleur input[type='color'] { width: 44px; height: 38px; padding: 2px; border: 1px solid var(--border); border-radius: var(--radius-sm); background: var(--bg-surface); cursor: pointer; }
    .saisie-couleur .hex { flex: 1; min-width: 0; font-family: ui-monospace, monospace; direction: ltr; }
    .logo { display: flex; gap: 14px; align-items: center; padding: 12px; border: 1px dashed var(--border-strong); border-radius: var(--radius-md); }
    .logo > div { display: grid; gap: 6px; }
    .apercu-logo, .mini-marque { display: grid; place-items: center; flex: none; width: 56px; height: 56px; border-radius: var(--radius-md); background: var(--brand); color: #fff; font-size: 26px; overflow: hidden; }
    .apercu-logo img, .mini-marque img { width: 100%; height: 100%; object-fit: contain; background: #fff; }
    .actions-logo { display: flex; gap: 10px; align-items: center; flex-wrap: wrap; }
    .fichier { position: relative; overflow: hidden; cursor: pointer; }
    .fichier input { position: absolute; inset: 0; opacity: 0; cursor: pointer; }
    fieldset:disabled .fichier, fieldset:disabled input, fieldset:disabled .apercu-logo { opacity: .55; cursor: not-allowed; }
    fieldset:disabled .fichier input { cursor: not-allowed; }
    .previsualisation { display: grid; gap: 12px; padding: 16px; border-radius: var(--radius-md); background: var(--bg-app); border: 1px solid var(--border); }
    .previsualisation > small { color: var(--text-soft); font-weight: 800; text-transform: uppercase; font-size: var(--fs-label); }
    .mini-barre { display: flex; gap: 10px; align-items: center; padding: 12px; border-radius: var(--radius-md); background: var(--bg-surface); }
    .mini-marque { width: 38px; height: 38px; font-size: 20px; }
    .mini-barre small, .mini-barre strong { display: block; }
    .mini-barre small { color: var(--text-soft); font-size: var(--fs-xs); }
    .mini-elements { display: grid; gap: 10px; justify-items: start; }
    .mini-lien { padding: 8px 12px; border-radius: var(--radius-sm); background: var(--brand-soft); color: var(--brand-dark); font-weight: 700; }
    .link-button { border: 0; background: none; color: var(--danger, #c0392b); font: inherit; font-weight: 700; cursor: pointer; }
    .message.erreur { color: var(--danger, #c0392b); }
    .form-actions { display: flex; flex-wrap: wrap; gap: 10px; justify-content: flex-end; }
    @media (max-width: 1100px) { .grille, .couleurs { grid-template-columns: minmax(0, 1fr); } }
  `]
})
export class PersonnalisationComponent implements OnDestroy {
  private readonly service = inject(ApparenceService);
  /** L'administrateur et la direction reglent l'apparence ; les autres roles la consultent. */
  readonly peutModifier = ['ADMIN', 'DIRECTION'].includes(inject(AuthService).role() ?? '');
  readonly serveurAJour = this.service.serveurAJour;
  readonly champsCouleur: { cle: 'couleurPrincipale' | 'couleurAccent' | 'couleurBouton'; libelle: string; aide: string }[] = [
    { cle: 'couleurPrincipale', libelle: 'Couleur principale', aide: 'Liens, menu actif, logo, éléments de marque' },
    { cle: 'couleurAccent', libelle: 'Couleur d’accent', aide: 'Succès, statuts payés, présence' },
    { cle: 'couleurBouton', libelle: 'Couleur des boutons', aide: 'Boutons principaux' }
  ];
  modele: Apparence = { ...this.service.apparence() };
  enregistrement = false;
  message = '';
  erreur = false;

  /** Apercu en direct dans toute l'application (annule si l'on quitte sans enregistrer). */
  apercu(): void {
    if (this.champsCouleur.every((champ) => /^#[0-9a-fA-F]{6}$/.test(this.modele[champ.cle]))) this.service.appliquer(this.modele);
  }

  choisirLogo(evenement: Event): void {
    const champ = evenement.target as HTMLInputElement;
    const fichier = champ.files?.[0];
    champ.value = '';
    if (!fichier) return;
    if (!/^image\/(png|jpeg|webp|svg\+xml)$/.test(fichier.type)) { this.afficher('Choisissez une image PNG, JPEG, WEBP ou SVG.', true); return; }
    if (fichier.size > TAILLE_MAX_LOGO) { this.afficher('Le logo dépasse 300 Ko : réduisez l’image.', true); return; }
    const lecteur = new FileReader();
    lecteur.onload = () => { this.modele.logo = String(lecteur.result); this.apercu(); this.afficher('', false); };
    lecteur.readAsDataURL(fichier);
  }

  reinitialiser(): void {
    this.modele = { ...APPARENCE_PAR_DEFAUT, logo: this.modele.logo };
    this.apercu();
  }

  annuler(): void {
    this.modele = { ...this.service.apparence() };
    this.service.appliquer(this.modele);
    this.afficher('', false);
  }

  enregistrer(): void {
    if (!this.modele.nomInterface?.trim()) { this.afficher('Le nom de l’interface est obligatoire.', true); return; }
    if (!this.champsCouleur.every((champ) => /^#[0-9a-fA-F]{6}$/.test(this.modele[champ.cle]))) {
      this.afficher('Chaque couleur doit être au format #RRGGBB.', true);
      return;
    }
    this.enregistrement = true;
    this.service.enregistrer({ ...this.modele, nomInterface: this.modele.nomInterface.trim() }).subscribe({
      next: (apparence) => {
        this.modele = { ...apparence };
        this.enregistrement = false;
        this.afficher('Apparence enregistrée : elle s’applique à tous les utilisateurs à leur prochain chargement.', false);
      },
      error: (erreur) => {
        this.enregistrement = false;
        const ancienServeur = erreur?.status === 403 || erreur?.status === 404 || erreur?.status === 405;
        this.afficher(ancienServeur
          ? 'Enregistrement refusé par le serveur : redémarrez le backend pour appliquer la mise à jour (migration V16), puis réessayez.'
          : messageErreur(erreur, 'L’enregistrement a échoué.'), true);
      }
    });
  }

  /** Apercu non enregistre : on revient a l'apparence en vigueur en quittant la page. */
  ngOnDestroy(): void { this.service.appliquer(this.service.apparence()); }

  private afficher(texte: string, erreur: boolean): void { this.message = texte; this.erreur = erreur; }
}
