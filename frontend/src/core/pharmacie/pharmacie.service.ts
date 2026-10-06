import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';
import {
  ApprovisionnementRequest, DispensationRequest, DispensationResponse, Medicament, MedicamentDetail, MedicamentRequest,
  PharmacieFinances, PrescriptionPharmacie
} from '../models/pharmacie';

@Injectable({ providedIn: 'root' })
export class PharmacieService {
  private readonly http = inject(HttpClient);
  private readonly url = `${environment.apiUrl}/pharmacie`;

  medicaments(): Observable<Medicament[]> { return this.http.get<Medicament[]>(`${this.url}/medicaments`); }
  details(id: number): Observable<MedicamentDetail> { return this.http.get<MedicamentDetail>(`${this.url}/medicaments/${id}`); }
  creerMedicament(request: MedicamentRequest): Observable<Medicament> { return this.http.post<Medicament>(`${this.url}/medicaments`, request); }
  modifierMedicament(id: number, request: MedicamentRequest): Observable<Medicament> {
    return this.http.put<Medicament>(`${this.url}/medicaments/${id}`, request);
  }
  importerMedicaments(request: MedicamentRequest[]): Observable<Medicament[]> {
    return this.http.post<Medicament[]>(`${this.url}/medicaments/import`, request);
  }
  modifierStock(id: number, stockActuel: number, commentaire?: string): Observable<Medicament> {
    return this.http.patch<Medicament>(`${this.url}/medicaments/${id}/stock`, { stockActuel, commentaire });
  }
  approvisionner(id: number, request: ApprovisionnementRequest): Observable<Medicament> {
    return this.http.post<Medicament>(`${this.url}/medicaments/${id}/approvisionnements`, request);
  }
  prescriptions(): Observable<PrescriptionPharmacie[]> { return this.http.get<PrescriptionPharmacie[]>(`${this.url}/prescriptions`); }
  dispenser(request: DispensationRequest): Observable<DispensationResponse> {
    return this.http.post<DispensationResponse>(`${this.url}/dispensations`, request);
  }
  finances(dateDebut?: string, dateFin?: string): Observable<PharmacieFinances> {
    let params = new HttpParams();
    if (dateDebut) params = params.set('dateDebut', dateDebut);
    if (dateFin) params = params.set('dateFin', dateFin);
    return this.http.get<PharmacieFinances>(`${this.url}/finances`, { params });
  }
  inventairePdfUrl(): string { return `${this.url}/medicaments/inventaire.pdf`; }
}
