/**
 * Configuration de l'application. A adapter au deploiement :
 * - apiUrl : adresse de l'API Spring Boot ;
 * - cabinet : coordonnees imprimees en en-tete des factures, recus, ordonnances et dossiers.
 */
export const environment = {
  apiUrl: 'http://localhost:8080/api',
  cabinet: {
    nom: 'Cabinet Médical',
    sousTitre: 'Cabinet de groupe',
    adresse: '',
    telephone: ''
  }
};
