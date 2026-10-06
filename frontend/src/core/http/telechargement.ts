import { HttpClient } from '@angular/common/http';
import { Observable, map } from 'rxjs';

/**
 * Telecharge un fichier protege par JWT (le jeton est ajoute par l'intercepteur) et le propose
 * a l'enregistrement sous {@code nomFichier}.
 */
export function telecharger(http: HttpClient, url: string, nomFichier: string): Observable<void> {
  return http.get(url, { responseType: 'blob' }).pipe(map((contenu) => enregistrer(contenu, nomFichier)));
}

export function enregistrer(contenu: Blob, nomFichier: string): void {
  const lien = document.createElement('a');
  lien.href = URL.createObjectURL(contenu);
  lien.download = nomFichier;
  lien.click();
  setTimeout(() => URL.revokeObjectURL(lien.href), 1000);
}
