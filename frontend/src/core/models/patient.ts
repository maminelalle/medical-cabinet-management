export interface Patient {
  id: number;
  nom: string;
  prenom: string;
  dateNaissance: string;
  telephone?: string;
  email?: string;
  adresse?: string;
}

export interface PatientRequest {
  nom: string;
  prenom: string;
  dateNaissance: string;
  telephone: string;
  email: string;
  adresse: string;
}