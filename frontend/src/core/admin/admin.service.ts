import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';
import {
  EntreeJournal, ParametresCabinet, Permission, PresenceMedecin, SessionUtilisateur, TableauBordAdmin, Utilisateur,
  UtilisateurRequest
} from '../models/admin';

@Injectable({ providedIn: 'root' })
export class AdminService {
  private readonly http = inject(HttpClient);
  private readonly url = `${environment.apiUrl}/admin`;

  tableauDeBord(): Observable<TableauBordAdmin> { return this.http.get<TableauBordAdmin>(`${this.url}/tableau-de-bord`); }
  utilisateurs(): Observable<Utilisateur[]> { return this.http.get<Utilisateur[]>(`${this.url}/utilisateurs`); }
  creerUtilisateur(request: UtilisateurRequest): Observable<Utilisateur> { return this.http.post<Utilisateur>(`${this.url}/utilisateurs`, request); }
  modifierUtilisateur(id: number, request: UtilisateurRequest): Observable<Utilisateur> {
    return this.http.put<Utilisateur>(`${this.url}/utilisateurs/${id}`, request);
  }
  /** Suppression d'un compte sans historique (sinon le serveur demande de le desactiver). */
  supprimerUtilisateur(id: number): Observable<void> { return this.http.delete<void>(`${this.url}/utilisateurs/${id}`); }
  changerStatut(id: number, actif: boolean): Observable<Utilisateur> {
    return this.http.patch<Utilisateur>(`${this.url}/utilisateurs/${id}/statut`, { actif });
  }
  reinitialiserMotDePasse(id: number, motDePasse: string): Observable<void> {
    return this.http.post<void>(`${this.url}/utilisateurs/${id}/mot-de-passe`, { motDePasse });
  }
  sessions(periode: 'actives' | 'jour' | 'semaine'): Observable<SessionUtilisateur[]> {
    return this.http.get<SessionUtilisateur[]>(`${this.url}/sessions`, { params: new HttpParams().set('periode', periode) });
  }
  revoquerSession(id: number): Observable<void> { return this.http.delete<void>(`${this.url}/sessions/${id}`); }
  journal(utilisateurId?: number, jours = 7): Observable<EntreeJournal[]> {
    let params = new HttpParams().set('jours', jours).set('limite', 500);
    if (utilisateurId) params = params.set('utilisateurId', utilisateurId);
    return this.http.get<EntreeJournal[]>(`${this.url}/journal`, { params });
  }
  permissions(): Observable<Permission[]> { return this.http.get<Permission[]>(`${this.url}/permissions`); }

  /** Presence des medecins (accueil, direction, administration). */
  presenceMedecins(): Observable<PresenceMedecin[]> { return this.http.get<PresenceMedecin[]>(`${environment.apiUrl}/medecins/presence`); }
  parametresCabinet(): Observable<ParametresCabinet> { return this.http.get<ParametresCabinet>(`${environment.apiUrl}/parametres-cabinet`); }
  modifierParametresCabinet(request: ParametresCabinet): Observable<ParametresCabinet> {
    return this.http.put<ParametresCabinet>(`${environment.apiUrl}/parametres-cabinet`, request);
  }
}
