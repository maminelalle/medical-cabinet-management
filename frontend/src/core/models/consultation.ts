import { Patient } from './patient';
import { RendezVous } from './rendez-vous';

export interface LignePrescription {
  id: number;
  medicament: string;
  posologie?: string;
  duree?: string;
}

export interface Prescription {
  id: number;
  consultationId: number;
  datePrescription: string;
  instructions?: string;
  lignes: LignePrescription[];
}

/** Une consultation telle qu'elle apparait dans le dossier medical d'un patient. */
export interface ConsultationDossier {
  id: number;
  rendezVousId: number;
  dateHeure: string;
  motif?: string;
  medecinId: number;
  medecinNom: string;
  medecinPrenom: string;
  specialite?: string;
  compteRendu: string;
  prescription: Prescription | null;
}

/** Dossier patient complet : identite, historique des consultations et prochains rendez-vous. */
export interface DossierPatient {
  patient: Patient;
  consultations: ConsultationDossier[];
  prochainsRendezVous: RendezVous[];
}