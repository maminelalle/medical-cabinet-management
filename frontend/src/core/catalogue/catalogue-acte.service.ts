import { environment } from '../../environments/environment';
import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { CatalogueActe, CatalogueActeRequest } from '../models/catalogue';

@Injectable({ providedIn: 'root' })
export class CatalogueActeService {
  private readonly http = inject(HttpClient);
  private readonly url = `${environment.apiUrl}/actes-catalogue`;

  list(): Observable<CatalogueActe[]> { return this.http.get<CatalogueActe[]>(this.url); }
  create(request: CatalogueActeRequest): Observable<CatalogueActe> { return this.http.post<CatalogueActe>(this.url, request); }
}