import { Facture } from './facture';

export interface MedecinActivite {
  medecinId: number;
  nom: string;
  prenom: string;
  specialite?: string;
  rendezVous: number;
  consultations: number;
}

export interface DashboardConsultations {
  dateDebut?: string;
  dateFin?: string;
  consultations: number;
  rendezVous: number;
  rendezVousTermines: number;
  parMedecin: MedecinActivite[];
}

export interface ChiffreAffairesType {
  typeActe: string;
  montant: number;
  nombreActes: number;
}

export interface ChiffreAffaires {
  dateDebut?: string;
  dateFin?: string;
  totalFacture: number;
  totalEncaisse: number;
  totalRestant: number;
  parTypeActe: ChiffreAffairesType[];
}

export interface Impayes {
  nombreFactures: number;
  nombreImpayees: number;
  montantFacture: number;
  montantRestant: number;
  tauxImpayees: number;
  factures: Facture[];
}