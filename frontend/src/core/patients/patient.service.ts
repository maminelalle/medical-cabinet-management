import { environment } from '../../environments/environment';
import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { DossierExport, DossierImportBilan } from '../models/consultation';
import { Patient, PatientRequest } from '../models/patient';

@Injectable({ providedIn: 'root' })
export class PatientService {
  private readonly http = inject(HttpClient);
  private readonly url = `${environment.apiUrl}/patients`;

  list(query = ''): Observable<Patient[]> {
    const params = query ? new HttpParams().set('q', query) : undefined;
    return this.http.get<Patient[]>(this.url, { params });
  }
  get(id: number): Observable<Patient> { return this.http.get<Patient>(`${this.url}/${id}`); }
  create(request: PatientRequest): Observable<Patient> { return this.http.post<Patient>(this.url, request); }
  update(id: number, request: PatientRequest): Observable<Patient> { return this.http.put<Patient>(`${this.url}/${id}`, request); }
  delete(id: number): Observable<void> { return this.http.delete<void>(`${this.url}/${id}`); }
  /** POST /api/patients/import : cree le patient ou fusionne l'historique du dossier importe. */
  importerDossier(dossier: Partial<DossierExport>): Observable<DossierImportBilan> {
    return this.http.post<DossierImportBilan>(`${this.url}/import`, dossier);
  }
}
