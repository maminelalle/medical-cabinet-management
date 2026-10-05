import { LignePrescription } from './consultation';

/** Ordonnance complete renvoyee par /api/ordonnances. */
export interface Ordonnance {
  id: number;
  consultationId: number;
  rendezVousId: number;
  dateConsultation: string;
  patientId: number;
  patientNom: string;
  patientPrenom: string;
  patientDateNaissance: string;
  medecinId: number;
  medecinNom: string;
  medecinPrenom: string;
  specialite?: string;
  numeroOrdre?: string;
  datePrescription: string;
  instructions?: string;
  delivree: boolean;
  lignes: LignePrescription[];
}
