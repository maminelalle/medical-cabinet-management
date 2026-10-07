import { Injectable, NgZone, inject } from '@angular/core';
import { Observable, Subject, filter } from 'rxjs';
import { environment } from '../../environments/environment';

/** Evenement pousse par le serveur : EN_LIGNE, HORS_LIGNE, CONSULTATION, COMPTES, BATTEMENT... */
export interface EvenementTempsReel {
  type: string;
  message?: string | null;
  utilisateurId?: number | null;
  date: string;
}

/**
 * Flux temps reel (Server-Sent Events). Un seul flux par onglet, ouvert par la coquille de l'application :
 * le serveur sait ainsi qui a l'application ouverte, et previent tous les ecrans des changements de presence.
 */
@Injectable({ providedIn: 'root' })
export class TempsReelService {
  private readonly zone = inject(NgZone);
  private readonly sujet = new Subject<EvenementTempsReel>();
  private source: EventSource | null = null;
  private reprise: ReturnType<typeof setTimeout> | null = null;

  /** Tous les evenements (y compris le battement de 30 s qui fait evoluer les retards). */
  readonly evenements$: Observable<EvenementTempsReel> = this.sujet.asObservable();
  /** Evenements qui changent la presence, les sessions ou les comptes. */
  readonly changements$ = this.evenements$.pipe(filter((evenement) => evenement.type !== 'BIENVENUE'));

  connecter(): void {
    const jeton = localStorage.getItem('token');
    if (!jeton || this.source) return;
    this.source = new EventSource(`${environment.apiUrl}/evenements?jeton=${encodeURIComponent(jeton)}`);
    this.source.addEventListener('cabinet', (message) => {
      const evenement = JSON.parse((message as MessageEvent<string>).data) as EvenementTempsReel;
      this.zone.run(() => this.sujet.next(evenement));
    });
    // Coupure (serveur redemarre, reseau) : nouvelle tentative avec le jeton courant.
    this.source.onerror = () => {
      this.fermerSource();
      if (localStorage.getItem('token')) this.reprise = setTimeout(() => this.connecter(), 5000);
    };
  }

  deconnecter(): void {
    if (this.reprise) clearTimeout(this.reprise);
    this.reprise = null;
    this.fermerSource();
  }

  private fermerSource(): void {
    this.source?.close();
    this.source = null;
  }
}
