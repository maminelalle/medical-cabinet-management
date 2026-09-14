import { Component, inject } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router } from '@angular/router';
import { AuthService } from '../../core/auth/auth.service';

@Component({ selector: 'app-login', standalone: true, imports: [ReactiveFormsModule], templateUrl: './login.component.html', styleUrl: './login.component.css' })
export class LoginComponent {
  private readonly formBuilder = inject(FormBuilder);
  private readonly auth = inject(AuthService);
  private readonly router = inject(Router);
  readonly form = this.formBuilder.nonNullable.group({ email: ['', [Validators.required, Validators.email]], motDePasse: ['', Validators.required] });
  error = '';
  submit(): void {
    if (this.form.invalid) { this.form.markAllAsTouched(); return; }
    this.error = '';
    this.auth.login(this.form.value.email!, this.form.value.motDePasse!).subscribe({ next: () => this.router.navigate(['/patients']), error: () => this.error = 'Identifiants invalides ou serveur indisponible.' });
  }
}