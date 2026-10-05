import { environment } from '../../environments/environment';
import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { DossierPatient } from '../models/consultation';

export interface ConsultationCreateRequest { compteRendu: string; }
export interface PrescriptionLineRequest { medicamentId?: number; medicament: string; posologie?: string; duree?: string; }
export interface PrescriptionCreateRequest { datePrescription: string; instructions?: string; lignes: PrescriptionLineRequest[]; }
export interface ConsultationResponse { id: number; rendezVousId: number; patientId: number; compteRendu: string; }
export interface PrescriptionResponse {
  id: number;
  consultationId: number;
  datePrescription: string;
  instructions?: string;
  lignes: { id: number; medicament: string; posologie?: string; duree?: string; }[];
}

@Injectable({ providedIn: 'root' })
export class ConsultationService {
  private readonly http = inject(HttpClient);
  private readonly url = `${environment.apiUrl}`;

  /** POST /api/rendezvous/{id}/consultation : redige le compte-rendu et clot le rendez-vous. */
  creer(rendezVousId: number, request: ConsultationCreateRequest): Observable<ConsultationResponse> {
    return this.http.post<ConsultationResponse>(`${this.url}/rendezvous/${rendezVousId}/consultation`, request);
  }
  /** POST /api/consultations/{id}/prescriptions : attache l'ordonnance a la consultation. */
  ajouterPrescription(consultationId: number, request: PrescriptionCreateRequest): Observable<PrescriptionResponse> {
    return this.http.post<PrescriptionResponse>(`${this.url}/consultations/${consultationId}/prescriptions`, request);
  }
  /** GET /api/patients/{id}/historique : dossier medical du patient (medecin). */
  dossier(patientId: number): Observable<DossierPatient> {
    return this.http.get<DossierPatient>(`${this.url}/patients/${patientId}/historique`);
  }
}