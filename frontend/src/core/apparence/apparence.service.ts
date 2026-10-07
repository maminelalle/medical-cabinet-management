import { HttpClient } from '@angular/common/http';
import { Injectable, inject, signal } from '@angular/core';
import { Observable, firstValueFrom, tap } from 'rxjs';
import { environment } from '../../environments/environment';

/** Apparence de l'interface reglee par l'administrateur. */
export interface Apparence {
  nomInterface: string;
  sousTitre?: string | null;
  couleurPrincipale: string;
  couleurAccent: string;
  couleurBouton: string;
  /** Logo en data URL (PNG, JPEG, WEBP ou SVG), ou null pour le logo par defaut. */
  logo?: string | null;
}

export const APPARENCE_PAR_DEFAUT: Apparence = {
  nomInterface: 'Cabinets Médicaux',
  sousTitre: 'Cabinet de groupe',
  couleurPrincipale: '#2563eb',
  couleurAccent: '#10b981',
  couleurBouton: '#111a2e',
  logo: null
};

const CLE_CACHE = 'cabinet.apparence';

/**
 * Applique a toute l'application le nom, le logo et les couleurs choisis par l'administrateur :
 * variables CSS de la charte (marque, accent, boutons et leurs nuances), titre de l'onglet et icone.
 * La derniere apparence connue est gardee sur le poste pour s'afficher des le chargement.
 */
@Injectable({ providedIn: 'root' })
export class ApparenceService {
  private readonly http = inject(HttpClient);
  private readonly url = `${environment.apiUrl}/parametres-cabinet/apparence`;
  /** Apparence enregistree. */
  readonly apparence = signal<Apparence>(ApparenceService.lireCache());
  /** Apparence affichee : l'enregistree, ou l'apercu en cours de l'administrateur. */
  readonly affichee = signal<Apparence>(this.apparence());
  /** Faux si le serveur ne propose pas encore l'apparence (backend a redemarrer apres mise a jour). */
  readonly serveurAJour = signal(true);

  /** Appele au demarrage : applique le cache tout de suite, puis la version du serveur. */
  async initialiser(): Promise<void> {
    this.appliquer(this.apparence());
    try {
      this.definir(await firstValueFrom(this.http.get<Apparence>(this.url)));
      this.serveurAJour.set(true);
    } catch {
      // Serveur injoignable ou pas encore mis a jour : l'apparence en cache reste appliquee.
      this.serveurAJour.set(false);
    }
  }

  enregistrer(apparence: Apparence): Observable<Apparence> {
    return this.http.put<Apparence>(this.url, apparence).pipe(tap((enregistree) => this.definir(enregistree)));
  }

  /** Apercu immediat (avant enregistrement) ; {@code definir} le rend definitif. */
  appliquer(apparence: Apparence): void {
    this.affichee.set({ ...apparence });
    const style = document.documentElement.style;
    const principale = ApparenceService.valide(apparence.couleurPrincipale, APPARENCE_PAR_DEFAUT.couleurPrincipale);
    const accent = ApparenceService.valide(apparence.couleurAccent, APPARENCE_PAR_DEFAUT.couleurAccent);
    const bouton = ApparenceService.valide(apparence.couleurBouton, APPARENCE_PAR_DEFAUT.couleurBouton);
    style.setProperty('--brand', principale);
    style.setProperty('--brand-dark', ApparenceService.melanger(principale, '#000000', 0.18));
    style.setProperty('--brand-soft', ApparenceService.melanger(principale, '#ffffff', 0.92));
    style.setProperty('--accent', accent);
    style.setProperty('--accent-soft', ApparenceService.melanger(accent, '#ffffff', 0.92));
    style.setProperty('--accent-text', ApparenceService.melanger(accent, '#000000', 0.35));
    style.setProperty('--ink', bouton);
    style.setProperty('--ink-hover', ApparenceService.melanger(bouton, '#ffffff', 0.12));
    document.title = apparence.nomInterface || APPARENCE_PAR_DEFAUT.nomInterface;
    this.appliquerIcone(apparence.logo);
  }

  private definir(apparence: Apparence): void {
    this.apparence.set(apparence);
    this.appliquer(apparence);
    try { localStorage.setItem(CLE_CACHE, JSON.stringify(apparence)); } catch { /* stockage plein ou indisponible */ }
  }

  private appliquerIcone(logo?: string | null): void {
    let lien = document.querySelector<HTMLLinkElement>('link[rel="icon"]');
    if (!logo) { lien?.remove(); return; }
    if (!lien) {
      lien = document.createElement('link');
      lien.rel = 'icon';
      document.head.appendChild(lien);
    }
    lien.href = logo;
  }

  private static lireCache(): Apparence {
    try {
      const brut = localStorage.getItem(CLE_CACHE);
      return brut ? { ...APPARENCE_PAR_DEFAUT, ...JSON.parse(brut) } : APPARENCE_PAR_DEFAUT;
    } catch { return APPARENCE_PAR_DEFAUT; }
  }

  private static valide(couleur: string | undefined, defaut: string): string {
    return couleur && /^#[0-9a-fA-F]{6}$/.test(couleur) ? couleur : defaut;
  }

  /** Melange deux couleurs #RRGGBB : {@code part} = proportion de la seconde (0 a 1). */
  static melanger(couleur: string, autre: string, part: number): string {
    const canal = (hex: string, i: number) => parseInt(hex.slice(1 + i * 2, 3 + i * 2), 16);
    return '#' + [0, 1, 2].map((i) => Math.round(canal(couleur, i) * (1 - part) + canal(autre, i) * part)
      .toString(16).padStart(2, '0')).join('');
  }
}
