import { StatutFacture } from './facture';
import { PatientRequest } from './patient';

export type TypeSoin = 'INJECTION' | 'PERFUSION' | 'PANSEMENT' | 'NEBULISATION' | 'CONSTANTES' | 'AUTRE';
export type StatutSoin = 'EN_ATTENTE' | 'EN_COURS' | 'TERMINE' | 'ANNULE';

export interface Soin {
  id: number;
  patientId: number;
  patientNom: string;
  patientPrenom: string;
  patientTelephone?: string | null;
  type: TypeSoin;
  intitule: string;
  produit?: string | null;
  prescriptionId?: number | null;
  /** Medecin du cabinet (ordonnance) ou prescripteur externe. */
  prescripteur?: string | null;
  dateHeure: string;
  statut: StatutSoin;
  observations?: string | null;
  debut?: string | null;
  fin?: string | null;
  realisePar?: string | null;
  factureId?: number | null;
  factureStatut?: StatutFacture | null;
  createdAt: string;
}

export interface SoinRequest {
  patientId?: number;
  nouveauPatient?: PatientRequest;
  type: TypeSoin;
  intitule: string;
  produit?: string | null;
  prescriptionId?: number | null;
  prescripteurExterne?: string | null;
  dateHeure?: string | null;
  observations?: string | null;
}

/** Types de soins, avec l'intitule propose et le mot qui retrouve le tarif dans la grille de la direction. */
export const TYPES_SOIN: { code: TypeSoin; libelle: string; intitule: string; motTarif: string }[] = [
  { code: 'INJECTION', libelle: 'Injection', intitule: 'Injection intramusculaire', motTarif: 'injection' },
  { code: 'PERFUSION', libelle: 'Perfusion', intitule: 'Pose de perfusion', motTarif: 'perfusion' },
  { code: 'PANSEMENT', libelle: 'Pansement', intitule: 'Pansement', motTarif: 'pansement' },
  { code: 'NEBULISATION', libelle: 'Nébulisation', intitule: 'Nébulisation (aérosol)', motTarif: 'nebulisation' },
  { code: 'CONSTANTES', libelle: 'Prise des constantes', intitule: 'Prise de la tension et de la glycémie', motTarif: 'constantes' },
  { code: 'AUTRE', libelle: 'Autre soin', intitule: '', motTarif: '' }
];

export function libelleTypeSoin(code: TypeSoin): string {
  return TYPES_SOIN.find((type) => type.code === code)?.libelle ?? code;
}
