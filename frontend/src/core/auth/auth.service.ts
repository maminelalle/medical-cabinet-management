import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, tap } from 'rxjs';

interface LoginResponse { token: string; email: string; role: string; }

@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly http = inject(HttpClient);
  private readonly api = 'http://localhost:8080/api';

  login(email: string, motDePasse: string): Observable<LoginResponse> {
    return this.http.post<LoginResponse>(`${this.api}/auth/login`, { email, motDePasse }).pipe(
      tap((response) => { localStorage.setItem('token', response.token); localStorage.setItem('role', response.role); localStorage.setItem('email', response.email); })
    );
  }

  logout(): void { localStorage.clear(); }
  isAuthenticated(): boolean { return Boolean(localStorage.getItem('token')); }
  role(): string | null { return localStorage.getItem('role'); }
}