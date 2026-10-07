import { Component, Input, inject } from '@angular/core';
import { RouterLink } from '@angular/router';
import { AdminService } from '../../core/admin/admin.service';
import { Permission } from '../../core/models/admin';

/** Matrice des permissions par role, telle qu'elle est appliquee par le serveur. */
@Component({
  selector: 'app-admin-permissions',
  standalone: true,
  imports: [RouterLink],
  template: `
    <section class="admin-page">
      @if (!integre) {
      <header class="page-heading">
        <div><p class="breadcrumb">ADMINISTRATION / PERMISSIONS</p><h1>Rôles et permissions</h1><p class="subtitle">Chaque compte reçoit un rôle ; le serveur vérifie ces droits à chaque requête.</p></div>
        <a class="primary-button" routerLink="/admin/utilisateurs">Attribuer un rôle</a>
      </header>
      }
      <section class="permission-grid">
        @for (p of permissions; track p.role) {
          <article class="card">
            <div class="card-heading"><div><span class="role" [class]="'role ' + p.role.toLowerCase()">{{ p.libelle }}</span></div></div>
            <ul>@for (acces of p.acces; track acces) { <li>{{ acces }}</li> }</ul>
          </article>
        }
      </section>
    </section>
  `,
  styleUrl: './admin.css'
})
export class PermissionsComponent {
  @Input() integre = false;
  permissions: Permission[] = [];
  constructor() { inject(AdminService).permissions().subscribe({ next: (items) => this.permissions = items }); }
}
