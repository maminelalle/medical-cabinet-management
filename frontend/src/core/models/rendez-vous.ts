export type StatutRendezVous = 'PLANIFIE' | 'CONFIRME' | 'EN_COURS' | 'TERMINE' | 'ANNULE' | 'ABSENT';

export interface RendezVous {
  id: number;
  patientId: number;
  patientNom: string;
  patientPrenom: string;
  medecinId: number;
  medecinNom: string;
  medecinPrenom: string;
  dateHeure: string;
  motif?: string;
  statut: StatutRendezVous;
  createdAt: string;
}