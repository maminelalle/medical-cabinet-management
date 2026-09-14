import { HttpInterceptorFn } from '@angular/common/http';
import { Router } from '@angular/router';
import { catchError, throwError } from 'rxjs';
import { inject } from '@angular/core';
import { AuthService } from './auth.service';

export const jwtInterceptor: HttpInterceptorFn = (request, next) => {
  const token = localStorage.getItem('token');
  const response = next(token ? request.clone({ setHeaders: { Authorization: `Bearer ${token}` } }) : request);
  return response.pipe(catchError((error: { status: number }) => {
    if (error.status === 401) {
      inject(AuthService).logout();
      void inject(Router).navigate(['/login']);
    }
    return throwError(() => error);
  }));
};