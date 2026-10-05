import { Component, inject } from '@angular/core';
import { RouterLink } from '@angular/router';
import { AuthService } from '../../core/auth/auth.service';

interface Acces { titre: string; detail: string; lien: string; }

const ACCES_PAR_ROLE: Record<string, Acces[]> = {
  ACCUEIL: [
    { titre: 'Patients et dossiers', detail: 'Créer, modifier, importer et exporter les dossiers patients.', lien: '/patients' },
    { titre: 'Rendez-vous', detail: 'Planifier, confirmer, annuler et imprimer le ticket de passage.', lien: '/rendez-vous' },
    { titre: 'Facturation', detail: 'Créer les factures, encaisser, imprimer facture, reçu et ordonnance.', lien: '/factures' },
    { titre: 'Ordonnances', detail: 'Consulter et réimprimer les ordonnances des patients.', lien: '/ordonnances' }
  ],
  MEDECIN: [
    { titre: 'Mon planning', detail: 'Rendez-vous du jour, changement de statut, consultation.', lien: '/rendez-vous' },
    { titre: 'Dossiers de mes patients', detail: 'Historique complet, export et import de dossier.', lien: '/patients' },
    { titre: 'Mes ordonnances', detail: 'Ordonnances rédigées, statut de délivrance et impression.', lien: '/ordonnances' },
    { titre: 'Facturation', detail: 'Consultation des factures en lecture seule.', lien: '/factures' }
  ],
  PHARMACIEN: [
    { titre: 'Pharmacie interne', detail: 'Stock, import Excel, dispensation des ordonnances.', lien: '/pharmacie' },
    { titre: 'Ordonnances', detail: 'Historique des ordonnances délivrées et à délivrer.', lien: '/ordonnances' },
    { titre: 'Factures pharmacie', detail: 'Encaissement et impression des reçus pharmacie.', lien: '/factures' }
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
