export type StatutFacture = 'EN_ATTENTE' | 'PARTIELLE' | 'PAYEE' | 'ANNULEE';

export interface LigneFacture {
  id: number;
  catalogueActeId?: number;
  libelle: string;
  typeActe: string;
  montant: number;
}

export interface Paiement {
  id: number;
  montant: number;
  datePaiement: string;
  moyenPaiement?: string;
}

export interface Facture {
  id: number;
  patientId: number;
  patientNom: string;
  patientPrenom: string;
  dateFacture: string;
  montantTotal: number;
  montantPaye: number;
  resteAPayer: number;
  statut: StatutFacture;
  lignes: LigneFacture[];
  paiements: Paiement[];
  motifAnnulation?: string | null;
  dateAnnulation?: string | null;
}