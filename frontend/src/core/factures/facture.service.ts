import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { Facture, StatutFacture } from '../models/facture';

export interface FactureLineRequest { catalogueActeId?: number; libelle: string; typeActe: string; montant: number; }
export interface FactureCreateRequest { patientId: number; dateFacture: string; lignes: FactureLineRequest[]; }

@Injectable({ providedIn: 'root' })
export class FactureService {
  private readonly http = inject(HttpClient);
  private readonly url = 'http://localhost:8080/api/factures';

  list(statut?: StatutFacture): Observable<Facture[]> {
    const params = statut ? new HttpParams().set('statut', statut) : undefined;
    return this.http.get<Facture[]>(this.url, { params });
  }
  pay(id: number, montant: number, moyenPaiement: string): Observable<Facture> {
    return this.http.post<Facture>(`${this.url}/${id}/paiements`, { montant, moyenPaiement });
  }
  create(request: FactureCreateRequest): Observable<Facture> { return this.http.post<Facture>(this.url, request); }
}