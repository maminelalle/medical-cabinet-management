import { environment } from '../../environments/environment';
import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { CatalogueActe, CatalogueActeRequest, RegleControleGratuit } from '../models/catalogue';

@Injectable({ providedIn: 'root' })
export class CatalogueActeService {
  private readonly http = inject(HttpClient);
  private readonly url = `${environment.apiUrl}/actes-catalogue`;
  private readonly urlRegle = `${environment.apiUrl}/parametres-cabinet/controle-gratuit`;

  /** Tarifs actifs (facturation) ; {@code tous} = grille complete de la direction. */
  list(tous = false): Observable<CatalogueActe[]> { return this.http.get<CatalogueActe[]>(this.url, { params: { tous } }); }
  create(request: CatalogueActeRequest): Observable<CatalogueActe> { return this.http.post<CatalogueActe>(this.url, request); }
  update(id: number, request: CatalogueActeRequest): Observable<CatalogueActe> { return this.http.put<CatalogueActe>(`${this.url}/${id}`, request); }
  delete(id: number): Observable<void> { return this.http.delete<void>(`${this.url}/${id}`); }

  regleControle(): Observable<RegleControleGratuit> { return this.http.get<RegleControleGratuit>(this.urlRegle); }
  modifierRegleControle(regle: RegleControleGratuit): Observable<RegleControleGratuit> {
    return this.http.put<RegleControleGratuit>(this.urlRegle, regle);
  }

  /** Tarif de consultation du medecin : celui de sa specialite, sinon le tarif general (premier sans specialite). */
  static tarifConsultation(tarifs: CatalogueActe[], specialite?: string | null): CatalogueActe | undefined {
    const consultations = tarifs.filter((tarif) => tarif.type === 'CONSULTATION' && tarif.actif);
    const normaliser = (texte?: string | null) => (texte ?? '').normalize('NFD').replace(/[\u0300-\u036f]/g, '').trim().toLowerCase();
    return consultations.find((tarif) => specialite && normaliser(tarif.specialite) === normaliser(specialite))
      ?? consultations.find((tarif) => !tarif.specialite);
  }
}
