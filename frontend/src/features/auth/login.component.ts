import { Component, inject } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router } from '@angular/router';
import { AuthService } from '../../core/auth/auth.service';

const CLE_EMAIL_MEMORISE = 'cabinet.emailMemoire';

@Component({ selector: 'app-login', standalone: true, imports: [ReactiveFormsModule], templateUrl: './login.component.html', styleUrl: './login.component.css' })
export class LoginComponent {
  private readonly formBuilder = inject(FormBuilder);
  private readonly auth = inject(AuthService);
  private readonly router = inject(Router);
  readonly form = this.formBuilder.nonNullable.group({ email: ['', [Validators.required, Validators.email]], motDePasse: ['', Validators.required], remember: [true] });
  error = '';
  info = '';

  constructor() {
    const emailMemorise = localStorage.getItem(CLE_EMAIL_MEMORISE);
    if (emailMemorise) {
      this.form.patchValue({ email: emailMemorise, remember: true });
    }
  }

  submit(): void {
    if (this.form.invalid) { this.form.markAllAsTouched(); return; }
    this.error = '';
    this.info = '';
    const { email, motDePasse, remember } = this.form.getRawValue();
    this.auth.login(email, motDePasse).subscribe({
      next: () => {
        if (remember) {
          localStorage.setItem(CLE_EMAIL_MEMORISE, email);
        } else {
          localStorage.removeItem(CLE_EMAIL_MEMORISE);
        }
        this.router.navigate([this.destination()]);
      },
      error: (response) => {
        this.error = response.status === 0
          ? 'Le serveur est indisponible. Vérifiez que Spring Boot est lancé sur le port 8080.'
          : response.status === 401
            ? 'Email ou mot de passe incorrect.'
            : 'La connexion est momentanément indisponible.';
      }
    });
  }

  /** Message d'aide a la place de l'ancien lien mort « mot de passe oublié ». */
  motDePasseOublie(): void {
    this.info = 'Contactez la direction du cabinet pour réinitialiser votre mot de passe.';
  }

  private destination(): string {
    const role = this.auth.role();
    return role === 'DIRECTION' ? '/direction/dashboard' : role === 'MEDECIN' ? '/medecin/dashboard' : role === 'PHARMACIEN' ? '/pharmacie' : '/accueil';
  }
}