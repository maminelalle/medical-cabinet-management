import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { Patient, PatientRequest } from '../models/patient';

@Injectable({ providedIn: 'root' })
export class PatientService {
  private readonly http = inject(HttpClient);
  private readonly url = 'http://localhost:8080/api/patients';

  list(query = ''): Observable<Patient[]> {
    const params = query ? new HttpParams().set('q', query) : undefined;
    return this.http.get<Patient[]>(this.url, { params });
  }
  create(request: PatientRequest): Observable<Patient> { return this.http.post<Patient>(this.url, request); }
  update(id: number, request: PatientRequest): Observable<Patient> { return this.http.put<Patient>(`${this.url}/${id}`, request); }
  delete(id: number): Observable<void> { return this.http.delete<void>(`${this.url}/${id}`); }
}