import { StatutFacture } from './facture';

export type TypeActe = 'CHIRURGIE' | 'TRAITEMENT' | 'EXAMEN' | 'HOSPITALISATION' | 'SOINS' | 'AUTRE';
export type StatutActe = 'PLANIFIE' | 'REALISE' | 'ANNULE';
export type ResultatActe = 'REUSSI' | 'PARTIEL' | 'ECHEC';

export interface ActeProgramme {
  id: number;
  patientId: number;
  patientNom: string;
  patientPrenom: string;
  patientDateNaissance: string;
  medecinId: number;
  medecinNom: string;
  medecinPrenom: string;
  specialite?: string;
  consultationId?: number | null;
  type: TypeActe;
  intitule: string;
  details?: string | null;
  dateHeure: string;
  dureeMinutes: number;
  lieu?: string | null;
  statut: StatutActe;
  resultat?: ResultatActe | null;
  /** Null pour l'accueil et l'administration (secret medical). */
  compteRendu?: string | null;
  compteRenduMasque: boolean;
  dateRealisation?: string | null;
  motifAnnulation?: string | null;
  factureId?: number | null;
  factureStatut?: StatutFacture | null;
  createdAt: string;
}

export interface ActeProgrammeRequest {
  patientId: number;
  consultationId?: number | null;
  type: TypeActe;
  intitule: string;
  details?: string;
  dateHeure: string;
  dureeMinutes: number;
  lieu?: string;
}

export const TYPES_ACTE: { code: TypeActe; libelle: string; typeFacturation: string }[] = [
  { code: 'CHIRURGIE', libelle: 'Chirurgie', typeFacturation: 'CHIRURGIE' },
  { code: 'TRAITEMENT', libelle: 'Traitement', typeFacturation: 'AUTRE' },
  { code: 'EXAMEN', libelle: 'Examen', typeFacturation: 'AUTRE' },
  { code: 'HOSPITALISATION', libelle: 'Hospitalisation', typeFacturation: 'HOSPITALISATION' },
  { code: 'SOINS', libelle: 'Soins', typeFacturation: 'AUTRE' },
  { code: 'AUTRE', libelle: 'Autre', typeFacturation: 'AUTRE' }
];

export function libelleTypeActe(type: TypeActe): string {
  return TYPES_ACTE.find((item) => item.code === type)?.libelle ?? type;
}

export function libelleResultat(resultat?: ResultatActe | null): string {
  return resultat === 'REUSSI' ? 'Réussi' : resultat === 'PARTIEL' ? 'Partiel' : resultat === 'ECHEC' ? 'Échec' : '—';
}

export function libelleStatutActe(statut: StatutActe): string {
  return statut === 'PLANIFIE' ? 'Planifié' : statut === 'REALISE' ? 'Réalisé' : 'Annulé';
}
