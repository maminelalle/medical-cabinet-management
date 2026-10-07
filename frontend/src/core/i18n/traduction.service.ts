import { Injectable, NgZone, inject, signal } from '@angular/core';

export type Langue = 'fr' | 'ar';

const CLE_STOCKAGE = 'cabinet.langue';
const ATTRIBUTS = ['placeholder', 'title', 'aria-label'];
/** Elements dont le contenu n'est jamais traduit (code, saisie, zones marquees translate="no"). */
const EXCLUS = 'script, style, textarea, code, pre, [translate="no"], .sans-traduction';

/**
 * Bascule francais / arabe de toute l'interface, sans modifier les gabarits : les textes affiches sont
 * traduits a la volee (dictionnaire), y compris ceux qu'Angular met a jour ensuite, et la page passe
 * en lecture de droite a gauche. Le choix est memorise sur le poste.
 */
@Injectable({ providedIn: 'root' })
export class TraductionService {
  private readonly zone = inject(NgZone);
  readonly langue = signal<Langue>(TraductionService.lireLangue());

  private dictionnaire = new Map<string, string>();
  private motif: RegExp | null = null;
  private observateur: MutationObserver | null = null;
  /** Texte francais d'origine et texte arabe pose, par noeud texte / attribut. */
  private readonly originaux = new WeakMap<Node, string>();
  private readonly poses = new WeakMap<Node, string>();
  private readonly attributsOriginaux = new WeakMap<Element, Map<string, string>>();
  private readonly attributsPoses = new WeakMap<Element, Map<string, string>>();
  private dialoguesPatches = false;

  /** Appele au demarrage : charge le dictionnaire si l'arabe a ete choisi. */
  async initialiser(): Promise<void> {
    this.appliquerDirection();
    if (this.langue() === 'ar') {
      await this.chargerDictionnaire();
      this.activer();
    }
  }

  async choisir(langue: Langue): Promise<void> {
    if (langue === this.langue()) return;
    try { localStorage.setItem(CLE_STOCKAGE, langue); } catch { /* stockage indisponible : choix limite a la session */ }
    this.langue.set(langue);
    this.appliquerDirection();
    if (langue === 'ar') {
      await this.chargerDictionnaire();
      this.activer();
    } else {
      this.desactiver();
    }
  }

  basculer(): Promise<void> { return this.choisir(this.langue() === 'ar' ? 'fr' : 'ar'); }

  /** Traduction d'un texte (utilisee aussi pour les boites confirm / prompt / alert). */
  traduire(texte: string): string {
    if (this.langue() !== 'ar' || !texte || !/[A-Za-zÀ-ÿ]/.test(texte)) return texte;
    const debut = texte.match(/^\s*/)![0];
    const fin = texte.match(/\s*$/)![0];
    const cle = TraductionService.normaliser(texte);
    const exact = this.dictionnaire.get(cle);
    if (exact !== undefined) return debut + exact + fin;
    if (!this.motif) return texte;
    // Texte compose (valeurs dynamiques) : traduction des expressions connues qu'il contient.
    const traduit = cle
      .replace(/(^|\s)à (\d{1,2}[:h]\d{2})/g, '$1على الساعة $2')
      .replace(this.motif, (expression) => this.dictionnaire.get(expression) ?? expression);
    return traduit === cle ? texte : debut + traduit + fin;
  }

  private async chargerDictionnaire(): Promise<void> {
    if (this.dictionnaire.size) return;
    const { AR } = await import('./ar');
    for (const [francais, arabe] of Object.entries(AR)) this.dictionnaire.set(TraductionService.normaliser(francais), arabe);
    // Expressions de plus de 2 caracteres, les plus longues d'abord, entre limites de mots.
    const expressions = [...this.dictionnaire.keys()].filter((cle) => cle.length > 2).sort((a, b) => b.length - a.length)
      .map((cle) => cle.replace(/[.*+?^${}()|[\]\\]/g, '\\$&'));
    this.motif = new RegExp(`(?<![\\p{L}\\p{N}])(?:${expressions.join('|')})(?![\\p{L}\\p{N}])`, 'gu');
  }

  private activer(): void {
    this.patcherDialogues();
    this.traduireArbre(document.body);
    if (this.observateur) return;
    this.zone.runOutsideAngular(() => {
      this.observateur = new MutationObserver((mutations) => this.surMutations(mutations));
      this.observateur.observe(document.body, {
        subtree: true, childList: true, characterData: true, attributes: true, attributeFilter: ATTRIBUTS
      });
    });
  }

  /** Retour au francais : chaque texte reprend sa valeur d'origine. */
  private desactiver(): void {
    this.observateur?.disconnect();
    this.observateur = null;
    const parcours = document.createTreeWalker(document.body, NodeFilter.SHOW_TEXT | NodeFilter.SHOW_ELEMENT);
    for (let noeud: Node | null = parcours.currentNode; noeud; noeud = parcours.nextNode()) {
      if (noeud.nodeType === Node.TEXT_NODE) {
        const texte = noeud as Text;
        const original = this.originaux.get(texte);
        if (original !== undefined && this.poses.get(texte) === texte.data) texte.data = original;
        this.poses.delete(texte);
      } else {
        const element = noeud as Element;
        const originaux = this.attributsOriginaux.get(element);
        const poses = this.attributsPoses.get(element);
        originaux?.forEach((valeur, nom) => { if (poses?.get(nom) === element.getAttribute(nom)) element.setAttribute(nom, valeur); });
        this.attributsPoses.delete(element);
      }
    }
  }

  private surMutations(mutations: MutationRecord[]): void {
    for (const mutation of mutations) {
      if (mutation.type === 'characterData') this.traduireTexte(mutation.target as Text);
      else if (mutation.type === 'attributes') this.traduireAttribut(mutation.target as Element, mutation.attributeName!);
      else mutation.addedNodes.forEach((noeud) => this.traduireArbre(noeud));
    }
  }

  private traduireArbre(racine: Node): void {
    if (racine.nodeType === Node.TEXT_NODE) { this.traduireTexte(racine as Text); return; }
    if (racine.nodeType !== Node.ELEMENT_NODE) return;
    const parcours = document.createTreeWalker(racine, NodeFilter.SHOW_TEXT | NodeFilter.SHOW_ELEMENT);
    for (let noeud: Node | null = parcours.currentNode; noeud; noeud = parcours.nextNode()) {
      if (noeud.nodeType === Node.TEXT_NODE) this.traduireTexte(noeud as Text);
      else for (const nom of ATTRIBUTS) if ((noeud as Element).hasAttribute(nom)) this.traduireAttribut(noeud as Element, nom);
    }
  }

  private traduireTexte(texte: Text): void {
    const valeur = texte.data;
    if (this.poses.get(texte) === valeur || !/[A-Za-zÀ-ÿ]/.test(valeur)) return;
    const parent = texte.parentElement;
    if (!parent || parent.closest(EXCLUS)) return;
    // Nouvelle valeur ecrite par Angular : c'est le nouveau texte francais de reference.
    this.originaux.set(texte, valeur);
    const traduit = this.traduire(valeur);
    if (traduit !== valeur) {
      this.poses.set(texte, traduit);
      texte.data = traduit;
    }
  }

  private traduireAttribut(element: Element, nom: string): void {
    const valeur = element.getAttribute(nom);
    if (valeur === null || element.closest(EXCLUS)) return;
    const poses = this.attributsPoses.get(element) ?? new Map<string, string>();
    if (poses.get(nom) === valeur) return;
    const originaux = this.attributsOriginaux.get(element) ?? new Map<string, string>();
    originaux.set(nom, valeur);
    this.attributsOriginaux.set(element, originaux);
    const traduit = this.traduire(valeur);
    if (traduit !== valeur) {
      poses.set(nom, traduit);
      this.attributsPoses.set(element, poses);
      element.setAttribute(nom, traduit);
    }
  }

  /** Les boites natives (confirmation, saisie d'un motif...) suivent la langue choisie. */
  private patcherDialogues(): void {
    if (this.dialoguesPatches) return;
    this.dialoguesPatches = true;
    const confirmer = window.confirm.bind(window);
    const demander = window.prompt.bind(window);
    const alerter = window.alert.bind(window);
    window.confirm = (message?: string) => confirmer(this.traduire(message ?? ''));
    window.prompt = (message?: string, defaut?: string) => demander(this.traduire(message ?? ''), defaut);
    window.alert = (message?: unknown) => alerter(typeof message === 'string' ? this.traduire(message) : message);
  }

  private appliquerDirection(): void {
    const racine = document.documentElement;
    racine.lang = this.langue();
    racine.dir = this.langue() === 'ar' ? 'rtl' : 'ltr';
  }

  private static normaliser(texte: string): string {
    return texte.replace(/[’‘]/g, "'").replace(/\s+/g, ' ').trim();
  }

  private static lireLangue(): Langue {
    try { return localStorage.getItem(CLE_STOCKAGE) === 'ar' ? 'ar' : 'fr'; } catch { return 'fr'; }
  }
}
