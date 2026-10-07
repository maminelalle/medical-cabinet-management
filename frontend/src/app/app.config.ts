import { ApplicationConfig, inject, provideAppInitializer } from '@angular/core';
import { provideHttpClient, withInterceptors } from '@angular/common/http';
import { provideRouter } from '@angular/router';
import { routes } from './app.routes';
import { jwtInterceptor } from '../core/auth/jwt.interceptor';
import { TraductionService } from '../core/i18n/traduction.service';
import { ApparenceService } from '../core/apparence/apparence.service';

export const appConfig: ApplicationConfig = {
  providers: [
    provideRouter(routes),
    provideHttpClient(withInterceptors([jwtInterceptor])),
    // Langue choisie (francais / arabe) appliquee avant le premier affichage.
    provideAppInitializer(() => inject(TraductionService).initialiser()),
    // Nom, logo et couleurs choisis par l'administrateur.
    provideAppInitializer(() => inject(ApparenceService).initialiser())
  ]
};