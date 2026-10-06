export interface Medicament {
  id: number;
  nom: string;
  dosage?: string;
  forme?: string;
  famille?: string | null;
  stockActuel: number;
  seuilAlerte: number;
  prixAchat: number;
  prixVente: number;
  fournisseur?: string;
  dateExpiration?: string;
  actif: boolean;
}

export interface LignePrescriptionPharmacie {
  id: number;
  medicamentId?: number;
  medicament: string;
  posologie?: string;
  duree?: string;
}

export interface PrescriptionPharmacie {
  id: number;
  patientId: number;
  patientNom: string;
  patientPrenom: string;
  datePrescription: string;
  instructions?: string;
  lignes: LignePrescriptionPharmacie[];
}

export interface MedicamentRequest {
  nom: string;
  dosage?: string;
  forme?: string;
  famille?: string;
  stockActuel: number;
  seuilAlerte: number;
  prixAchat: number;
  prixVente: number;
  fournisseur?: string;
  dateExpiration?: string;
}

export interface DispensationLineRequest {
  medicamentId: number;
  quantite: number;
}

/** Vente d'une ordonnance : paiement obligatoire (reference hors especes). */
export interface DispensationRequest {
  prescriptionId: number;
  lignes: DispensationLineRequest[];
  moyenPaiement: string;
  referencePaiement?: string;
}

export interface DispensationResponse {
  id: number;
  prescriptionId: number;
  patientId: number;
  factureId: number;
  dateDispensation: string;
  medicaments: Medicament[];
}

export type TypeMouvement = 'ENTREE_INITIALE' | 'ACHAT' | 'VENTE' | 'AJUSTEMENT';

export interface MouvementStock {
  id: number;
  medicamentId: number;
  medicament: string;
  type: TypeMouvement;
  quantite: number;
  stockApres: number;
  prixUnitaire?: number | null;
  montant?: number | null;
  fournisseur?: string | null;
  reference?: string | null;
  commentaire?: string | null;
  dispensationId?: number | null;
  factureId?: number | null;
  patient?: string | null;
  utilisateur?: string | null;
  dateMouvement: string;
}

export interface MedicamentDetail {
  medicament: Medicament;
  quantiteVendue: number;
  chiffreVentes: number;
  quantiteAchetee: number;
  coutAchats: number;
  margeBrute: number;
  valeurStockAchat: number;
  valeurStockVente: number;
  derniereVente?: string | null;
  dernierAchat?: string | null;
  mouvements: MouvementStock[];
}

export interface ApprovisionnementRequest {
  quantite: number;
  prixAchatUnitaire: number;
  fournisseur?: string;
  reference?: string;
  dateExpiration?: string;
}

export interface MontantParLibelle { libelle: string; nombre: number; montant: number; }

export interface PharmacieFinances {
  dateDebut: string;
  dateFin: string;
  chiffreVentes: number;
  coutAchats: number;
  margeBrute: number;
  encaisse: number;
  resteAEncaisser: number;
  nombreVentes: number;
  valeurStockAchat: number;
  valeurStockVente: number;
  encaissementsParMoyen: MontantParLibelle[];
  meilleuresVentes: MontantParLibelle[];
  mouvements: MouvementStock[];
}

export function libelleMouvement(type: TypeMouvement): string {
  return type === 'ENTREE_INITIALE' ? 'Stock initial' : type === 'ACHAT' ? 'Achat' : type === 'VENTE' ? 'Vente' : 'Ajustement';
}
