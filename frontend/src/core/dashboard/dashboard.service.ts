import { environment } from '../../environments/environment';
import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { ChiffreAffaires, DashboardConsultations, Impayes, MedecinActivite } from '../models/dashboard';

@Injectable({ providedIn: 'root' })
export class DashboardService {
  private readonly http = inject(HttpClient);
  private readonly url = `${environment.apiUrl}/dashboard`;

  consultations(dateDebut?: string, dateFin?: string): Observable<DashboardConsultations> {
    return this.http.get<DashboardConsultations>(`${this.url}/consultations`, { params: this.parametres(dateDebut, dateFin) });
  }
  chiffreAffaires(dateDebut?: string, dateFin?: string): Observable<ChiffreAffaires> {
    return this.http.get<ChiffreAffaires>(`${this.url}/chiffre-affaires`, { params: this.parametres(dateDebut, dateFin) });
  }
  impayes(): Observable<Impayes> {
    return this.http.get<Impayes>(`${this.url}/impayes`);
  }
  activiteMedecins(dateDebut?: string, dateFin?: string): Observable<MedecinActivite[]> {
    return this.http.get<MedecinActivite[]>(`${this.url}/activite-medecins`, { params: this.parametres(dateDebut, dateFin) });
  }

  private parametres(dateDebut?: string, dateFin?: string): HttpParams {
    let params = new HttpParams();
    if (dateDebut) params = params.set('dateDebut', dateDebut);
    if (dateFin) params = params.set('dateFin', dateFin);
    return params;
  }
}