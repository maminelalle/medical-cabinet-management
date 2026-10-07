export interface CatalogueActe {
  id: number;
  libelle: string;
  type: string;
  montantDefaut: number;
  actif: boolean;
  /** Renseignee : tarif de consultation de cette specialite (pre-rempli a la facturation du rendez-vous). */
  specialite?: string | null;
  description?: string | null;
  /** Deja facture : se desactive au lieu d etre supprime. */
  utilise?: boolean;
}

export interface CatalogueActeRequest {
  libelle: string;
  type: string;
  montantDefaut: number;
  specialite?: string | null;
  description?: string | null;
  actif?: boolean;
}

/** Regle du controle gratuit : apres une consultation payee, controle(s) gratuit(s) avec le meme medecin. */
export interface RegleControleGratuit {
  actif: boolean;
  jours: number;
  nombre: number;
}

export const TYPES_TARIF = ['CONSULTATION', 'SOINS', 'HOSPITALISATION', 'CHIRURGIE', 'LABORATOIRE', 'IMAGERIE', 'PHARMACIE'];
