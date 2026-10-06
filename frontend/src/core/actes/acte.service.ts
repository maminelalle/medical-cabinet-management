import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';
import { ActeProgramme, ActeProgrammeRequest, ResultatActe, StatutActe } from '../models/acte';

@Injectable({ providedIn: 'root' })
export class ActeService {
  private readonly http = inject(HttpClient);
  private readonly url = `${environment.apiUrl}/actes`;

  list(patientId?: number, statut?: StatutActe): Observable<ActeProgramme[]> {
    let params = new HttpParams();
    if (patientId) params = params.set('patientId', patientId);
    if (statut) params = params.set('statut', statut);
    return this.http.get<ActeProgramme[]>(this.url, { params });
  }
  get(id: number): Observable<ActeProgramme> { return this.http.get<ActeProgramme>(`${this.url}/${id}`); }
  programmer(request: ActeProgrammeRequest): Observable<ActeProgramme> { return this.http.post<ActeProgramme>(this.url, request); }
  modifier(id: number, request: ActeProgrammeRequest): Observable<ActeProgramme> { return this.http.put<ActeProgramme>(`${this.url}/${id}`, request); }
  realiser(id: number, resultat: ResultatActe, compteRendu: string, dateRealisation?: string): Observable<ActeProgramme> {
    return this.http.post<ActeProgramme>(`${this.url}/${id}/realisation`, { resultat, compteRendu, dateRealisation: dateRealisation || null });
  }
  annuler(id: number, motif: string): Observable<ActeProgramme> { return this.http.post<ActeProgramme>(`${this.url}/${id}/annulation`, { motif }); }
}
