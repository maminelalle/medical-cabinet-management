import { HttpErrorResponse } from '@angular/common/http';

/** Message lisible d'une erreur de l'API (format ErreurResponse du backend), ou le message par defaut. */
export function messageErreur(reponse: unknown, defaut: string): string {
  if (reponse instanceof HttpErrorResponse) {
    if (reponse.status === 0) return 'Le serveur est injoignable. Vérifiez que le backend est démarré.';
    const corps = reponse.error as { message?: string; champs?: Record<string, string> } | null;
    const champs = corps?.champs ? Object.entries(corps.champs).map(([champ, message]) => `${champ} : ${message}`).join(', ') : '';
    if (corps?.message) return champs ? `${corps.message} (${champs})` : corps.message;
  }
  return defaut;
}
