import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';
import { Creneau, RendezVous, RendezVousRequest, StatutRendezVous } from '../models/rendez-vous';

export interface Medecin { id: number; nom: string; prenom: string; specialite?: string; }

@Injectable({ providedIn: 'root' })
export class RendezVousService {
  private readonly http = inject(HttpClient);
  private readonly url = `${environment.apiUrl}/rendezvous`;

  list(date?: string, medecinId?: number, statut?: StatutRendezVous): Observable<RendezVous[]> {
    let params = new HttpParams();
    if (date) params = params.set('date', date);
    if (medecinId) params = params.set('medecinId', medecinId);
    if (statut) params = params.set('statut', statut);
    return this.http.get<RendezVous[]>(this.url, { params });
  }
  get(id: number): Observable<RendezVous> { return this.http.get<RendezVous>(`${this.url}/${id}`); }
  /** Rendez-vous d'une periode (utilise par le calendrier du tableau de bord). */
  listPeriode(dateDebut: string, dateFin: string, medecinId?: number): Observable<RendezVous[]> {
    let params = new HttpParams().set('dateDebut', dateDebut).set('dateFin', dateFin);
    if (medecinId) params = params.set('medecinId', medecinId);
    return this.http.get<RendezVous[]>(this.url, { params });
  }
  /** Grille des creneaux d'un medecin pour une journee : libres, passes ou deja reserves. */
  creneaux(medecinId: number, date: string, dureeMinutes: number): Observable<Creneau[]> {
    const params = new HttpParams().set('medecinId', medecinId).set('date', date).set('dureeMinutes', dureeMinutes);
    return this.http.get<Creneau[]>(`${this.url}/creneaux`, { params });
  }
  doctors(): Observable<Medecin[]> { return this.http.get<Medecin[]>(`${environment.apiUrl}/medecins`); }
  /** Cree le rendez-vous ; avec {@code nouveauPatient}, la fiche patient est creee en meme temps. */
  create(request: RendezVousRequest): Observable<RendezVous> { return this.http.post<RendezVous>(this.url, request); }
  /** PUT /api/rendezvous/{id} : modifie ou reprogramme un rendez-vous planifie ou confirme. */
  update(id: number, request: RendezVousRequest): Observable<RendezVous> { return this.http.put<RendezVous>(`${this.url}/${id}`, request); }
  updateStatut(id: number, statut: StatutRendezVous): Observable<RendezVous> { return this.http.patch<RendezVous>(`${this.url}/${id}/statut`, { statut }); }
  /** Suppression definitive (rendez-vous sans consultation ni facture). */
  delete(id: number): Observable<void> { return this.http.delete<void>(`${this.url}/${id}`); }
}
