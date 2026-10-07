import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';
import { Soin, SoinRequest, StatutSoin } from '../models/soin';

/** Soins au cabinet : injection, perfusion, pansement, nebulisation, prise des constantes. */
@Injectable({ providedIn: 'root' })
export class SoinService {
  private readonly http = inject(HttpClient);
  private readonly url = `${environment.apiUrl}/soins`;

  list(date?: string, statut?: StatutSoin, patientId?: number): Observable<Soin[]> {
    let params = new HttpParams();
    if (date) params = params.set('date', date);
    if (statut) params = params.set('statut', statut);
    if (patientId) params = params.set('patientId', patientId);
    return this.http.get<Soin[]>(this.url, { params });
  }
  get(id: number): Observable<Soin> { return this.http.get<Soin>(`${this.url}/${id}`); }
  create(request: SoinRequest): Observable<Soin> { return this.http.post<Soin>(this.url, request); }
  update(id: number, request: SoinRequest): Observable<Soin> { return this.http.put<Soin>(`${this.url}/${id}`, request); }
  demarrer(id: number): Observable<Soin> { return this.http.post<Soin>(`${this.url}/${id}/demarrer`, {}); }
  terminer(id: number, observations?: string): Observable<Soin> { return this.http.post<Soin>(`${this.url}/${id}/terminer`, { observations }); }
  annuler(id: number, motif: string): Observable<Soin> { return this.http.post<Soin>(`${this.url}/${id}/annulation`, { motif }); }
}
