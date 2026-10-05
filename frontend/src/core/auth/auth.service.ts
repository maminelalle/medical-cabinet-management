import { environment } from '../../environments/environment';
import { Injectable, computed, inject, signal } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, tap } from 'rxjs';

interface LoginResponse { token: string; email: string; role: string; }

/** Identite reelle de l'utilisateur connecte, renvoyee par GET /api/profil. */
export interface Profil {
  id: number;
  email: string;
  role: string;
  nom?: string | null;
  prenom?: string | null;
  specialite?: string | null;
}

@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly http = inject(HttpClient);
  private readonly api = `${environment.apiUrl}`;
  private readonly profilInterne = signal<Profil | null>(null);

  readonly profil = this.profilInterne.asReadonly();
  readonly emailAffiche = computed(() => this.profilInterne()?.email ?? this.email() ?? '');
  readonly prenom = computed(() => this.profilInterne()?.prenom ?? '');
  readonly nomComplet = computed(() => {
    const profil = this.profilInterne();
    const nomEtPrenom = [profil?.prenom, profil?.nom].filter(Boolean).join(' ');
    if (nomEtPrenom) return nomEtPrenom;
    const partieLocale = this.emailAffiche().split('@')[0].replace(/[._-]+/g, ' ').trim();
    return partieLocale ? partieLocale.replace(/\b\w/g, (lettre) => lettre.toUpperCase()) : 'Utilisateur';
  });
  readonly initiales = computed(() => {
    const mots = this.nomComplet().split(/\s+/).filter(Boolean);
    return (mots.length > 1 ? mots[0][0] + mots[1][0] : mots[0]?.slice(0, 2) ?? 'US').toUpperCase();
  });
  readonly libelleRole = computed(() => {
    const profil = this.profilInterne();
    const role = profil?.role ?? this.role();
    if (role === 'MEDECIN') return profil?.specialite ? `MÉDECIN · ${profil.specialite}` : 'MÉDECIN';
    if (role === 'PHARMACIEN') return 'PHARMACIEN';
    return role === 'DIRECTION' ? 'DIRECTION' : 'ACCUEIL / CAISSE';
  });

  constructor() { if (this.isAuthenticated()) this.chargerProfil(); }

  login(email: string, motDePasse: string): Observable<LoginResponse> {
    return this.http.post<LoginResponse>(`${this.api}/auth/login`, { email, motDePasse }).pipe(
      tap((response) => {
        localStorage.setItem('token', response.token);
        localStorage.setItem('role', response.role);
        localStorage.setItem('email', response.email);
        this.chargerProfil();
      })
    );
  }

  /** Charge le nom, le prenom et la specialite reels de l'utilisateur connecte. */
  chargerProfil(): void {
    this.http.get<Profil>(`${this.api}/profil`).subscribe({
      next: (profil) => this.profilInterne.set(profil),
      error: () => this.profilInterne.set(null)
    });
  }

  logout(): void {
    localStorage.removeItem('token');
    localStorage.removeItem('role');
    localStorage.removeItem('email');
    this.profilInterne.set(null);
  }
  isAuthenticated(): boolean { return Boolean(localStorage.getItem('token')); }
  role(): string | null { return localStorage.getItem('role'); }
  email(): string | null { return localStorage.getItem('email'); }
}