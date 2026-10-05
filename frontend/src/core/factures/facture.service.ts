import { environment } from '../../environments/environment';
import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { Facture, StatutFacture } from '../models/facture';

export interface FactureLineRequest { catalogueActeId?: number; libelle: string; typeActe: string; montant: number; }
export interface FactureCreateRequest { patientId: number; dateFacture: string; lignes: FactureLineRequest[]; }

@Injectable({ providedIn: 'root' })
export class FactureService {
  private readonly http = inject(HttpClient);
  private readonly url = `${environment.apiUrl}/factures`;

  list(statut?: StatutFacture): Observable<Facture[]> {
    const params = statut ? new HttpParams().set('statut', statut) : undefined;
    return this.http.get<Facture[]>(this.url, { params });
  }
  get(id: number): Observable<Facture> { return this.http.get<Facture>(`${this.url}/${id}`); }
  pay(id: number, montant: number, moyenPaiement: string): Observable<Facture> {
    return this.http.post<Facture>(`${this.url}/${id}/paiements`, { montant, moyenPaiement });
  }
  create(request: FactureCreateRequest): Observable<Facture> { return this.http.post<Facture>(this.url, request); }
  /** POST /api/factures/{id}/annulation : annule une facture sans paiement, motif obligatoire. */
  annuler(id: number, motif: string): Observable<Facture> { return this.http.post<Facture>(`${this.url}/${id}/annulation`, { motif }); }
}