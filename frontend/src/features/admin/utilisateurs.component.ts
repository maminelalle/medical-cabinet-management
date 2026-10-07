import { Component, DestroyRef, Input, inject } from '@angular/core';
import { DatePipe } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { debounceTime, filter } from 'rxjs';
import { AdminService } from '../../core/admin/admin.service';
import { AuthService } from '../../core/auth/auth.service';
import { messageErreur } from '../../core/http/erreur-api';
import { ROLES, Role, Utilisateur, UtilisateurRequest, libelleRole } from '../../core/models/admin';
import { TempsReelService } from '../../core/temps-reel/temps-reel.service';

/**
 * Gestion des comptes : creation, modification, activation / desactivation, mot de passe, suppression.
 * Utilisee par l'administrateur et par la direction (employes du cabinet, sans les comptes administrateur).
 * L'etat "en ligne" se met a jour en temps reel.
 */
@Component({
  selector: 'app-admin-utilisateurs',
  standalone: true,
  imports: [DatePipe, FormsModule, RouterLink],
  template: `
    <section class="admin-page">
      @if (!integre) {
        <header class="page-heading">
          <div><p class="breadcrumb">ADMINISTRATION / UTILISATEURS</p><h1>Utilisateurs</h1><p class="subtitle">Comptes du cabinet, rôles (permissions) et état de connexion.</p></div>
          <button class="primary-button" type="button" (click)="nouveau()">+ Nouvel utilisateur</button>
        </header>
      }
      @if (message) { <div class="alert success"><span>✓</span><div><strong>Opération réalisée</strong>{{ message }}</div></div> }
      @if (erreur) { <div class="alert danger"><span>×</span><div><strong>Action impossible</strong>{{ erreur }}</div></div> }

      @if (formulaire) {
        <section class="card form-card">
          <div class="card-heading"><div><h2>{{ editionId ? 'Modifier le compte' : 'Nouvel employé' }}</h2><p>Le rôle détermine les écrans et les actions autorisés</p></div><button class="soft-button" type="button" (click)="formulaire = false">×</button></div>
          <div class="form-quatre">
            <label>Prénom *<input [(ngModel)]="saisie.prenom"></label>
            <label>Nom *<input [(ngModel)]="saisie.nom"></label>
            <label>Email (identifiant) *<input type="email" [(ngModel)]="saisie.email"></label>
            <label>Téléphone<input [(ngModel)]="saisie.telephone"></label>
            <label>Rôle *
              <select [(ngModel)]="saisie.role" [disabled]="!!editionId && (saisie.role === 'MEDECIN' || roleInitial === 'MEDECIN')">
                @for (role of roles; track role.code) { <option [value]="role.code">{{ role.libelle }}</option> }
              </select>
            </label>
            @if (!editionId) { <label>Mot de passe initial * (8 car. min.)<input type="password" [(ngModel)]="saisie.motDePasse" autocomplete="new-password"></label> }
            @if (saisie.role === 'MEDECIN') {
              <label>Spécialité<input [(ngModel)]="saisie.specialite" placeholder="Cardiologie"></label>
              <label>N° d’ordre<input [(ngModel)]="saisie.numeroOrdre"></label>
            }
          </div>
          @if (editionId && roleInitial === 'MEDECIN') { <p class="muted">Le rôle médecin ne se retire pas : créez un nouveau compte si nécessaire.</p> }
          <div class="actions-ligne"><button class="primary-button" type="button" (click)="enregistrer()" [disabled]="saving">{{ saving ? 'Enregistrement...' : 'Enregistrer' }}</button></div>
        </section>
      }

      <article class="card table-card">
        <div class="table-heading">
          <div><h2>{{ integre ? 'Employés du cabinet' : 'Comptes' }}</h2><p>{{ filtres.length }} compte(s) · {{ enLigne }} en ligne maintenant</p></div>
          <div class="filters">
            <input type="search" [(ngModel)]="recherche" placeholder="Nom ou email">
            <select [(ngModel)]="filtreRole"><option value="">Tous les rôles</option>@for (role of roles; track role.code) { <option [value]="role.code">{{ role.libelle }}</option> }</select>
            @if (integre) { <button class="primary-button" type="button" (click)="nouveau()">+ Nouvel employé</button> }
          </div>
        </div>
        <div class="table-wrap"><table>
          <thead><tr><th>Utilisateur</th><th>Rôle</th><th>État</th><th>Dernière connexion</th><th>Sessions</th><th>Actions</th></tr></thead>
          <tbody>
            @for (u of filtres; track u.id) {
              <tr>
                <td><strong>{{ nom(u) }}</strong><small>{{ u.email }}{{ u.telephone ? ' · ' + u.telephone : '' }}</small></td>
                <td><span class="role" [class]="'role ' + u.role.toLowerCase()">{{ libelleRole(u.role) }}</span>@if (u.specialite) { <small>{{ u.specialite }}</small> }</td>
                <td>
                  @if (!u.actif) { <span class="etat desactive">Désactivé</span> }
                  @else if (u.enLigne) { <span class="etat en_ligne">En ligne</span> }
                  @else { <span class="etat">Absent</span> }
                </td>
                <td>{{ u.derniereConnexion ? (u.derniereConnexion | date:'dd/MM/yyyy HH:mm') : 'Jamais' }}<small>{{ u.connecteAujourdhui ? 'connecté aujourd’hui' : 'pas aujourd’hui' }}</small></td>
                <td>{{ u.sessionsOuvertes }}</td>
                <td>
                  <div class="actions-ligne">
                    <button class="soft-button petit" type="button" (click)="modifier(u)">✎ Modifier</button>
                    @if (u.id !== monId) {
                      <button class="soft-button petit" [class.danger]="u.actif" type="button" (click)="basculer(u)">{{ u.actif ? 'Désactiver' : 'Activer' }}</button>
                    }
                    @if (motDePasseId === u.id) {
                      <input class="petit" type="password" [(ngModel)]="nouveauMotDePasse" placeholder="Nouveau (8 car.)" autocomplete="new-password">
                      <button class="primary-button petit" type="button" (click)="reinitialiser(u)">OK</button>
                    } @else {
                      <button class="soft-button petit" type="button" (click)="motDePasseId = u.id; nouveauMotDePasse = ''">Mot de passe</button>
                    }
                    @if (u.id !== monId) {
                      @if (aSupprimer === u.id) {
                        <button class="soft-button danger petit" type="button" (click)="supprimer(u)">Confirmer la suppression</button>
                        <button class="soft-button petit" type="button" (click)="aSupprimer = null">Non</button>
                      } @else {
                        <button class="soft-button danger petit" type="button" (click)="aSupprimer = u.id">Supprimer</button>
                      }
                    }
                    @if (!integre) { <a class="soft-button petit" routerLink="/admin/journal" [queryParams]="{ utilisateurId: u.id }">Activité</a> }
                  </div>
                </td>
              </tr>
            } @empty { <tr><td colspan="6" class="empty-cell">Aucun compte.</td></tr> }
          </tbody>
        </table></div>
      </article>
    </section>
  `,
  styleUrl: './admin.css'
})
export class UtilisateursComponent {
  /** Integre dans une autre page (parametres de la direction) : pas d'en-tete de page. */
  @Input() integre = false;

  private readonly service = inject(AdminService);
  private readonly auth = inject(AuthService);
  readonly estAdmin = this.auth.role() === 'ADMIN';
  /** La direction n'attribue pas le role administrateur. */
  readonly roles = ROLES.filter((role) => this.estAdmin || role.code !== 'ADMIN');
  readonly libelleRole = libelleRole;
  utilisateurs: Utilisateur[] = [];
  recherche = '';
  filtreRole = '';
  formulaire = false;
  editionId: number | null = null;
  roleInitial: Role | null = null;
  saisie: UtilisateurRequest = this.vide();
  motDePasseId: number | null = null;
  nouveauMotDePasse = '';
  aSupprimer: number | null = null;
  saving = false;
  message = '';
  erreur = '';

  constructor() {
    this.charger();
    // Connexions, departs et modifications de comptes : la liste se met a jour sans rafraichir.
    inject(TempsReelService).changements$
      .pipe(filter((evenement) => evenement.type !== 'BATTEMENT'), debounceTime(300), takeUntilDestroyed(inject(DestroyRef)))
      .subscribe(() => this.charger());
  }

  get monId(): number | undefined { return this.auth.profil()?.id; }

  charger(): void {
    this.service.utilisateurs().subscribe({ next: (items) => this.utilisateurs = items, error: () => this.erreur = 'Liste des comptes indisponible.' });
  }

  get filtres(): Utilisateur[] {
    const terme = this.recherche.trim().toLowerCase();
    return this.utilisateurs.filter((u) => (!this.filtreRole || u.role === this.filtreRole)
      && (!terme || `${u.prenom ?? ''} ${u.nom ?? ''} ${u.email}`.toLowerCase().includes(terme)));
  }
  get enLigne(): number { return this.utilisateurs.filter((u) => u.enLigne).length; }

  nouveau(): void { this.formulaire = true; this.editionId = null; this.roleInitial = null; this.saisie = this.vide(); this.message = ''; this.erreur = ''; }

  modifier(u: Utilisateur): void {
    this.formulaire = true;
    this.editionId = u.id;
    this.roleInitial = u.role;
    this.saisie = { email: u.email, nom: u.nom ?? '', prenom: u.prenom ?? '', telephone: u.telephone ?? '', role: u.role,
      specialite: u.specialite ?? '', numeroOrdre: u.numeroOrdre ?? '' };
    this.message = '';
    this.erreur = '';
    window.scrollTo({ top: 0, behavior: 'smooth' });
  }

  enregistrer(): void {
    if (!this.saisie.nom.trim() || !this.saisie.prenom.trim() || !this.saisie.email.trim()) { this.erreur = 'Prénom, nom et email sont obligatoires.'; return; }
    this.saving = true;
    this.erreur = '';
    const operation = this.editionId ? this.service.modifierUtilisateur(this.editionId, this.saisie) : this.service.creerUtilisateur(this.saisie);
    operation.subscribe({
      next: (u) => { this.message = `Compte de ${this.nom(u)} enregistré.`; this.formulaire = false; this.saving = false; this.charger(); },
      error: (response) => { this.erreur = messageErreur(response, 'Le compte n’a pas pu être enregistré.'); this.saving = false; }
    });
  }

  basculer(u: Utilisateur): void {
    this.service.changerStatut(u.id, !u.actif).subscribe({
      next: (maj) => { this.message = maj.actif ? `${this.nom(maj)} est réactivé.` : `${this.nom(maj)} est désactivé et déconnecté de toutes ses sessions.`; this.charger(); },
      error: (response) => this.erreur = messageErreur(response, 'Changement impossible.')
    });
  }

  reinitialiser(u: Utilisateur): void {
    if (this.nouveauMotDePasse.length < 8) { this.erreur = 'Le mot de passe doit contenir au moins 8 caractères.'; return; }
    this.service.reinitialiserMotDePasse(u.id, this.nouveauMotDePasse).subscribe({
      next: () => { this.message = `Mot de passe de ${this.nom(u)} réinitialisé ; ses sessions ont été fermées.`; this.motDePasseId = null; this.charger(); },
      error: (response) => this.erreur = messageErreur(response, 'Réinitialisation impossible.')
    });
  }

  supprimer(u: Utilisateur): void {
    this.erreur = '';
    this.service.supprimerUtilisateur(u.id).subscribe({
      next: () => { this.message = `Le compte de ${this.nom(u)} est supprimé.`; this.aSupprimer = null; this.charger(); },
      error: (response) => { this.erreur = messageErreur(response, 'Suppression impossible.'); this.aSupprimer = null; }
    });
  }

  nom(u: Utilisateur): string { return [u.prenom, u.nom].filter(Boolean).join(' ') || u.email; }
  private vide(): UtilisateurRequest { return { email: '', nom: '', prenom: '', telephone: '', role: 'ACCUEIL', motDePasse: '', specialite: '', numeroOrdre: '' }; }
}
