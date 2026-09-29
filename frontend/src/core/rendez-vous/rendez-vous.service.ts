import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { RendezVous, StatutRendezVous } from '../models/rendez-vous';

export interface Medecin { id: number; nom: string; prenom: string; specialite?: string; }
export interface RendezVousCreateRequest { patientId: number; medecinId: number; dateHeure: string; motif: string; }

@Injectable({ providedIn: 'root' })
export class RendezVousService {
  private readonly http = inject(HttpClient);
  private readonly url = 'http://localhost:8080/api/rendezvous';

  list(date?: string, medecinId?: number, statut?: StatutRendezVous): Observable<RendezVous[]> {
    let params = new HttpParams();
    if (date) params = params.set('date', date);
    if (medecinId) params = params.set('medecinId', medecinId);
    if (statut) params = params.set('statut', statut);
    return this.http.get<RendezVous[]>(this.url, { params });
  }
  get(id: number): Observable<RendezVous> { return this.http.get<RendezVous>(`${this.url}/${id}`); }
  doctors(): Observable<Medecin[]> { return this.http.get<Medecin[]>('http://localhost:8080/api/medecins'); }
  create(request: RendezVousCreateRequest): Observable<RendezVous> { return this.http.post<RendezVous>(this.url, request); }
  updateStatut(id: number, statut: StatutRendezVous): Observable<RendezVous> { return this.http.patch<RendezVous>(`${this.url}/${id}/statut`, { statut }); }
}