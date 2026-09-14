import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { AuthService } from './auth.service';

export const roleGuard: CanActivateFn = (route) => {
  const roles = route.data['roles'] as string[] | undefined;
  const allowed = !roles || roles.includes(inject(AuthService).role() ?? '');
  return allowed ? true : inject(Router).createUrlTree(['/login']);
};