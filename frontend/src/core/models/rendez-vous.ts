import { StatutFacture } from './facture';
import { PatientRequest } from './patient';

export type StatutRendezVous = 'PLANIFIE' | 'CONFIRME' | 'EN_COURS' | 'TERMINE' | 'ANNULE' | 'ABSENT';

export interface RendezVous {
  id: number;
  patientId: number;
  patientNom: string;
  patientPrenom: string;
  patientTelephone?: string;
  medecinId: number;
  medecinNom: string;
  medecinPrenom: string;
  dateHeure: string;
  dureeMinutes: number;
  motif?: string;
  numeroFile: number;
  statut: StatutRendezVous;
  debutConsultation?: string | null;
  finConsultation?: string | null;
  /** Consultation payee dont ce rendez-vous est le controle. */
  rendezVousOrigineId?: number | null;
  /** Controle couvert par la consultation payee : rien a facturer. */
  controleGratuit?: boolean;
  /** Facture active (non annulee) du rendez-vous. */
  factureId?: number | null;
  factureStatut?: StatutFacture | null;
  createdAt: string;
}

/** Prise de rendez-vous : un patient existant (patientId) ou un nouveau patient cree en meme temps. */
export interface RendezVousRequest {
  patientId?: number;
  nouveauPatient?: PatientRequest;
  medecinId: number;
  dateHeure: string;
  dureeMinutes: number;
  motif: string;
  /** Controle gratuit : consultation payee qui l'ouvre (proposee par /controle-gratuit). */
  rendezVousOrigineId?: number | null;
}

/** Droit a un controle gratuit pour ce patient, ce medecin et cette date. */
export interface ControleGratuit {
  eligible: boolean;
  rendezVousOrigineId?: number | null;
  dateOrigine?: string | null;
  dateLimite?: string | null;
  restants: number;
  message: string;
}

export interface Creneau {
  debut: string;
  fin: string;
  libre: boolean;
  passe: boolean;
  occupePar?: string | null;
}
