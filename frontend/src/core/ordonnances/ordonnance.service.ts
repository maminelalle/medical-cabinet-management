import { environment } from '../../environments/environment';
import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { Ordonnance } from '../models/ordonnance';

@Injectable({ providedIn: 'root' })
export class OrdonnanceService {
  private readonly http = inject(HttpClient);
  private readonly url = `${environment.apiUrl}/ordonnances`;

  list(patientId?: number): Observable<Ordonnance[]> {
    const params = patientId ? new HttpParams().set('patientId', patientId) : undefined;
    return this.http.get<Ordonnance[]>(this.url, { params });
  }
  get(id: number): Observable<Ordonnance> { return this.http.get<Ordonnance>(`${this.url}/${id}`); }
}
