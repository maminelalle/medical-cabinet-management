import { ActeProgramme } from './acte';
import { Facture } from './facture';
import { Patient } from './patient';
import { RendezVous } from './rendez-vous';

export interface LignePrescription {
  id: number;
  medicamentId?: number;
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
  /** Null lorsque le compte-rendu est couvert par le secret medical (role accueil). */
  compteRendu: string | null;
  prescription: Prescription | null;
  ordonnanceDelivree: boolean;
}

/** Dossier patient complet : identite, consultations, ordonnances, rendez-vous et factures. */
export interface DossierPatient {
  patient: Patient;
  consultations: ConsultationDossier[];
  prochainsRendezVous: RendezVous[];
  rendezVous: RendezVous[];
  factures: Facture[];
  actes: ActeProgramme[];
  compteRenduMasque: boolean;
}

/** Fichier d'export d'un dossier patient (reimportable). */
export interface DossierExport extends DossierPatient {
  format: string;
  version: number;
  exporteLe: string;
  exportePar: string;
}

export interface DossierImportBilan {
  patientId: number;
  patientCree: boolean;
  consultationsImportees: number;
  ordonnancesImportees: number;
  rendezVousImportes: number;
  facturesImportees: number;
  elementsIgnores: number;
}
