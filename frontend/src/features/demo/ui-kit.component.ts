import { Component } from '@angular/core';

interface Swatch { label: string; token: string; }
interface Kpi { label: string; value: string; caption: string; icon: string; tone: string; accent: string; }
interface InvoiceRow { id: string; patient: string; initials: string; date: string; amount: string; status: string; badge: string; }

@Component({
  selector: 'app-ui-kit',
  standalone: true,
  templateUrl: './ui-kit.component.html',
  styleUrl: './ui-kit.component.css'
})
export class UiKitComponent {
  readonly swatches: Swatch[] = [
    { label: 'Fond application', token: '--bg-app' },
    { label: 'Surface', token: '--bg-surface' },
    { label: 'Fond discret', token: '--bg-subtle' },
    { label: 'Bordure', token: '--border' },
    { label: 'Texte principal', token: '--text-strong' },
    { label: 'Texte secondaire', token: '--text-muted' },
    { label: 'Marque', token: '--brand' },
    { label: 'Marque sombre', token: '--brand-dark' },
    { label: 'Succès', token: '--accent' },
    { label: 'Alerte', token: '--warn' },
    { label: 'Danger', token: '--danger' },
    { label: 'Information', token: '--info' },
    { label: 'Violet', token: '--violet' },
    { label: 'Bouton principal', token: '--ink' }
  ];

  readonly kpis: Kpi[] = [
    { label: 'Rendez-vous aujourd\'hui', value: '12', caption: '+2 par rapport à hier', icon: '□', tone: 'teal', accent: 'accent' },
    { label: 'Patients enregistrés', value: '350', caption: '+5 ce mois', icon: '○', tone: 'blue', accent: 'brand' },
    { label: 'Factures en attente', value: '04', caption: 'À relancer cette semaine', icon: '▣', tone: 'orange', accent: 'warn' },
    { label: 'Chiffre d\'affaires', value: '125 000', caption: 'MRU encaissés aujourd\'hui', icon: '₨', tone: 'violet', accent: 'violet' }
  ];

  readonly badges: string[][] = [
    ['planifie', 'Planifié'],
    ['confirme', 'Confirmé'],
    ['en_cours', 'En cours'],
    ['termine', 'Terminé'],
    ['annule', 'Annulé'],
    ['en_attente', 'En attente'],
    ['partielle', 'Partielle'],
    ['payee', 'Payée'],
    ['annulee', 'Annulée']
  ];

  readonly invoices: InvoiceRow[] = [
    { id: 'FAC-142', patient: 'Lalle Ould Mohamed', initials: 'LO', date: '15/09/2026', amount: '12 500 MRU', status: 'Payée', badge: 'payee' },
    { id: 'FAC-141', patient: 'Mohamed Ould Sidi', initials: 'MO', date: '15/09/2026', amount: '8 000 MRU', status: 'Partielle', badge: 'partielle' },
    { id: 'FAC-140', patient: 'Fatimetou Mint Ahmedou', initials: 'FM', date: '14/09/2026', amount: '25 000 MRU', status: 'En attente', badge: 'en_attente' },
    { id: 'FAC-139', patient: 'Brahim Ould Ely', initials: 'BO', date: '13/09/2026', amount: '6 500 MRU', status: 'Annulée', badge: 'annulee' }
  ];

  readonly navPreview: string[][] = [
    ['Tableau de bord', 'active', ''],
    ['Patients', '', ''],
    ['Rendez-vous', '', ''],
    ['Facturation', '', ''],
    ['Catalogue des actes', '', 'Bientôt']
  ];
}