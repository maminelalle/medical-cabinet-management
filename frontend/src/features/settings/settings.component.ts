import { Component, inject } from '@angular/core';
import { RouterLink } from '@angular/router';
import { AuthService } from '../../core/auth/auth.service';

interface Acces { titre: string; detail: string; lien: string; }

const ACCES_PAR_ROLE: Record<string, Acces[]> = {
  ACCUEIL: [
    { titre: 'Rendez-vous', detail: 'Prise de rendez-vous avec création rapide du patient, créneaux libres, ticket de passage.', lien: '/rendez-vous/nouveau' },
    { titre: 'Patients et dossiers', detail: 'Créer, modifier, importer et exporter (PDF, transfert) les dossiers patients.', lien: '/patients' },
    { titre: 'Facturation', detail: 'Factures liées au rendez-vous ou à l’acte, paiement avec référence (Bankily, Masrvi, Sedad...).', lien: '/factures' },
    { titre: 'Actes programmés', detail: 'Chirurgies et traitements programmés par les médecins : organisation et facturation.', lien: '/actes' },
    { titre: 'Ordonnances', detail: 'Consulter et réimprimer les ordonnances des patients.', lien: '/ordonnances' }
  ],
  MEDECIN: [
    { titre: 'Mon planning', detail: 'Démarrer et terminer les consultations du jour.', lien: '/rendez-vous' },
    { titre: 'Dossiers de mes patients', detail: 'Historique complet, export PDF et import de dossier.', lien: '/patients' },
    { titre: 'Mes ordonnances', detail: 'Ordonnances rédigées depuis le stock, statut de délivrance et impression.', lien: '/ordonnances' },
    { titre: 'Mes actes programmés', detail: 'Chirurgies, traitements, examens : programmation et compte-rendu.', lien: '/actes' }
  ],
  PHARMACIEN: [
    { titre: 'Pharmacie interne', detail: 'Stock, fiches produits, import / export, approvisionnements, finances.', lien: '/pharmacie' },
    { titre: 'Vente des ordonnances', detail: 'Délivrance avec paiement obligatoire et référence.', lien: '/pharmacie' },
    { titre: 'Factures pharmacie', detail: 'Reçus et factures des ventes.', lien: '/factures' }
  ],
  ADMIN: [
    { titre: 'Utilisateurs', detail: 'Comptes, rôles, activation et mots de passe.', lien: '/admin/utilisateurs' },
    { titre: 'Connexions et appareils', detail: 'Sessions ouvertes, appareils, révocation.', lien: '/admin/sessions' },
    { titre: 'Journal d’activité', detail: 'Toutes les actions de tous les utilisateurs.', lien: '/admin/journal' },
    { titre: 'Coordonnées du cabinet', detail: 'En-tête des documents imprimés et PDF.', lien: '/admin/cabinet' }
  ],
  DIRECTION: [
    { titre: 'Tableau de bord', detail: 'Consultations, chiffre d’affaires, impayés et activité médecins.', lien: '/direction/dashboard' },
    { titre: 'Patients et dossiers', detail: 'Consultation, export et import des dossiers patients.', lien: '/patients' },
    { titre: 'Catalogue des actes', detail: 'Gestion du référentiel des actes facturables.', lien: '/catalogue' },
    { titre: 'Pharmacie et facturation', detail: 'Supervision du stock et des factures.', lien: '/pharmacie' }
  ]
};

@Component({
  selector: 'app-settings',
  standalone: true,
  imports: [RouterLink],
  templateUrl: './settings.component.html',
  styleUrl: './settings.component.css'
})
export class SettingsComponent {
  readonly auth = inject(AuthService);
  readonly acces = ACCES_PAR_ROLE[this.auth.role() ?? ''] ?? [];
  message = '';

  actualiserProfil(): void {
    this.auth.chargerProfil();
    this.message = 'Les informations du profil ont été actualisées.';
  }
}
