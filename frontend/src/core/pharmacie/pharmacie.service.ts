import { environment } from '../../environments/environment';
import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { DispensationRequest, DispensationResponse, Medicament, MedicamentRequest, PrescriptionPharmacie } from '../models/pharmacie';

@Injectable({ providedIn: 'root' })
export class PharmacieService {
  private readonly http = inject(HttpClient);
  private readonly url = `${environment.apiUrl}/pharmacie`;

  medicaments(): Observable<Medicament[]> {
    return this.http.get<Medicament[]>(`${this.url}/medicaments`);
  }
  creerMedicament(request: MedicamentRequest): Observable<Medicament> {
    return this.http.post<Medicament>(`${this.url}/medicaments`, request);
  }
  importerMedicaments(request: MedicamentRequest[]): Observable<Medicament[]> {
    return this.http.post<Medicament[]>(`${this.url}/medicaments/import`, request);
  }
  modifierStock(id: number, stockActuel: number): Observable<Medicament> {
    return this.http.patch<Medicament>(`${this.url}/medicaments/${id}/stock`, { stockActuel });
  }
  prescriptions(): Observable<PrescriptionPharmacie[]> {
    return this.http.get<PrescriptionPharmacie[]>(`${this.url}/prescriptions`);
  }
  dispenser(request: DispensationRequest): Observable<DispensationResponse> {
    return this.http.post<DispensationResponse>(`${this.url}/dispensations`, request);
  }
}
