export type StatutFacture = 'EN_ATTENTE' | 'PARTIELLE' | 'PAYEE' | 'ANNULEE';
export type OrigineFacture = 'CONSULTATION' | 'ACTE' | 'PHARMACIE' | 'LIBRE';

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
  reference?: string | null;
  enregistrePar?: string | null;
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
  createdAt?: string;
  creePar?: string | null;
  motifAnnulation?: string | null;
  dateAnnulation?: string | null;
  origine: OrigineFacture;
  rendezVousId?: number | null;
  rendezVousDateHeure?: string | null;
  rendezVousMedecin?: string | null;
  acteProgrammeId?: number | null;
  acteIntitule?: string | null;
  dispensationId?: number | null;
}

export interface PaiementRequest {
  montant: number;
  moyenPaiement: string;
  reference?: string;
}

/** Moyens de paiement acceptes ; tous sauf les especes exigent une reference de transaction. */
export const MOYENS_PAIEMENT: { code: string; libelle: string }[] = [
  { code: 'ESPECES', libelle: 'Espèces' },
  { code: 'BANKILY', libelle: 'Bankily' },
  { code: 'MASRVI', libelle: 'Masrvi' },
  { code: 'SEDAD', libelle: 'Sedad' },
  { code: 'CARTE', libelle: 'Carte bancaire' },
  { code: 'VIREMENT', libelle: 'Virement' },
  { code: 'CHEQUE', libelle: 'Chèque' }
];

export function libelleMoyen(code?: string | null): string {
  return MOYENS_PAIEMENT.find((moyen) => moyen.code === code)?.libelle ?? code ?? 'Autre';
}

export function libelleOrigine(facture: Facture): string {
  switch (facture.origine) {
    case 'CONSULTATION': return 'Rendez-vous' + (facture.rendezVousMedecin ? ' · ' + facture.rendezVousMedecin : '');
    case 'ACTE': return 'Acte · ' + (facture.acteIntitule ?? '');
    case 'PHARMACIE': return 'Pharmacie';
    default: return 'Facture libre';
  }
}
