import { DossierExport, DossierPatient } from '../models/consultation';

export const FORMAT_DOSSIER = 'cabinet-medical.dossier-patient';

/** Telecharge le dossier complet du patient au format JSON (reimportable dans l'application). */
export function exporterDossier(dossier: DossierPatient, auteur: string): void {
  const contenu: DossierExport = { format: FORMAT_DOSSIER, version: 1, exporteLe: new Date().toISOString(), exportePar: auteur, ...dossier };
  const blob = new Blob([JSON.stringify(contenu, null, 2)], { type: 'application/json' });
  const lien = document.createElement('a');
  const nom = `${dossier.patient.nom}-${dossier.patient.prenom}`.normalize('NFD').replace(/[^\w-]+/g, '_').toLowerCase();
  lien.href = URL.createObjectURL(blob);
  lien.download = `dossier-${nom}-${new Date().toISOString().slice(0, 10)}.json`;
  lien.click();
  URL.revokeObjectURL(lien.href);
}

/** Lit un fichier de dossier exporte et verifie qu'il contient au minimum l'identite du patient. */
export async function lireFichierDossier(fichier: File): Promise<Partial<DossierExport>> {
  let contenu: Partial<DossierExport>;
  try {
    contenu = JSON.parse(await fichier.text());
  } catch {
    throw new Error('Le fichier n’est pas un JSON valide.');
  }
  const patient = contenu?.patient;
  if (!patient?.nom || !patient?.prenom || !patient?.dateNaissance) {
    throw new Error('Le fichier ne contient pas de dossier patient (nom, prénom et date de naissance requis).');
  }
  return contenu;
}
