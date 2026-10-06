import { Component, inject } from '@angular/core';
import { Router } from '@angular/router';
import { AuthService } from '../../core/auth/auth.service';

@Component({ selector: 'app-default-redirect', standalone: true, template: '' })
export class DefaultRedirectComponent {
  private readonly router = inject(Router);
  private readonly auth = inject(AuthService);
  constructor() {
    const destination = this.auth.role() === 'ADMIN' ? '/admin' : this.auth.role() === 'DIRECTION' ? '/direction/dashboard' : this.auth.role() === 'MEDECIN' ? '/medecin/dashboard' : this.auth.role() === 'PHARMACIEN' ? '/pharmacie' : '/accueil';
    this.router.navigateByUrl(destination);
  }
}