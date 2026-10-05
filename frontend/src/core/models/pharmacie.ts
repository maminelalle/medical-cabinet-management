export interface Medicament {
  id: number;
  nom: string;
  dosage?: string;
  forme?: string;
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

export interface DispensationRequest {
  prescriptionId: number;
  lignes: DispensationLineRequest[];
}

export interface DispensationResponse {
  id: number;
  prescriptionId: number;
  patientId: number;
  factureId: number;
  dateDispensation: string;
  medicaments: Medicament[];
}
