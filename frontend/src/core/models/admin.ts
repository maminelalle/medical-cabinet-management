export type Role = 'ACCUEIL' | 'MEDECIN' | 'PHARMACIEN' | 'DIRECTION' | 'ADMIN';

export const ROLES: { code: Role; libelle: string }[] = [
  { code: 'ACCUEIL', libelle: 'Accueil / Caisse' },
  { code: 'MEDECIN', libelle: 'Médecin' },
  { code: 'PHARMACIEN', libelle: 'Pharmacien' },
  { code: 'DIRECTION', libelle: 'Direction' },
  { code: 'ADMIN', libelle: 'Administrateur' }
];

export function libelleRole(role?: string | null): string {
  return ROLES.find((item) => item.code === role)?.libelle ?? role ?? '';
}

export interface Utilisateur {
  id: number;
  email: string;
  role: Role;
  nom?: string | null;
  prenom?: string | null;
  telephone?: string | null;
  actif: boolean;
  derniereConnexion?: string | null;
  derniereActivite?: string | null;
  enLigne: boolean;
  connecteAujourdhui: boolean;
  sessionsOuvertes: number;
  medecinId?: number | null;
  specialite?: string | null;
  numeroOrdre?: string | null;
  createdAt: string;
}

export interface UtilisateurRequest {
  email: string;
  nom: string;
  prenom: string;
  telephone?: string;
  role: Role;
  motDePasse?: string;
  specialite?: string;
  numeroOrdre?: string;
}

export interface SessionUtilisateur {
  id: number;
  utilisateurId: number;
  email: string;
  nomComplet: string;
  role: Role;
  adresseIp?: string | null;
  appareil?: string | null;
  navigateur?: string | null;
  systeme?: string | null;
  dateConnexion: string;
  derniereActivite: string;
  dateFin?: string | null;
  statut: 'EN_LIGNE' | 'INACTIVE' | 'EXPIREE' | 'DECONNEXION' | 'REVOQUEE';
}

export interface EntreeJournal {
  id: number;
  email?: string | null;
  nomComplet?: string | null;
  role?: string | null;
  action: string;
  description?: string | null;
  methode?: string | null;
  chemin?: string | null;
  statutHttp?: number | null;
  adresseIp?: string | null;
  dateAction: string;
}

export interface TableauBordAdmin {
  utilisateurs: number;
  utilisateursActifs: number;
  enLigne: number;
  connectesAujourdhui: number;
  sessionsOuvertes: number;
  actionsAujourdhui: number;
  echecsConnexionAujourdhui: number;
  presence: Utilisateur[];
  dernieresActions: EntreeJournal[];
}

export interface Permission { role: Role; libelle: string; acces: string[]; }

export type StatutPresence = 'ABSENT' | 'DISPONIBLE' | 'EN_CONSULTATION' | 'EN_RETARD';

export interface PresenceMedecin {
  medecinId: number;
  nom: string;
  prenom: string;
  specialite?: string | null;
  connecte: boolean;
  derniereActivite?: string | null;
  statut: StatutPresence;
  patientEnCours?: string | null;
  debutConsultation?: string | null;
  prochainRendezVous?: string | null;
  prochainPatient?: string | null;
  retardMinutes: number;
  rendezVousRestants: number;
  rendezVousTermines: number;
}

export interface ParametresCabinet {
  nom: string;
  sousTitre?: string | null;
  adresse?: string | null;
  telephone?: string | null;
  email?: string | null;
}
