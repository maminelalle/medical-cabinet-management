# Dossier de conception — Application de gestion d'un cabinet médical

Stage — Licence 3 Informatique · Durée : 1 mois · Stack imposée : Spring Boot · Angular · PostgreSQL


## Sommaire

1. Analyse du besoin
2. Acteurs et cas d'utilisation
3. Processus métier clé — le fil conducteur
4. Modèle de données
5. Architecture technique
6. Spécification des API REST
7. Matrice des droits par rôle
8. Recommandations de sécurité et de qualité
9. Extensions bonus — impact sur le modèle
10. Planning détaillé affiné (4 semaines)
11. Livrables attendus
12. Synthèse
13. Internationalisation, personnalisation et mise à jour
14. Problèmes rencontrés et enseignements
15. Évolutions réalisées

## 1. Analyse du besoin


### 1.1 Problématique

Le cabinet gère aujourd'hui trois flux d'information de façon dispersée (papier, tableurs) : la prise de rendez-vous, le suivi médical et la facturation. Ces trois flux partagent un même pivot, le patient, et un même fil conducteur : rendez-vous → dossier → facture. L'absence de centralisation entraîne des risques classiques dans ce contexte : doublons de fiches patient, créneaux médecins en conflit, actes non facturés ou facturés en double, absence de visibilité sur les impayés et sur l'activité réelle du cabinet.


### 1.2 Ce que le sujet demande explicitement de ne pas faire

Le sujet est volontairement cadré pour tenir en un mois. Deux points de vigilance à garder en tête pendant toute la conception :

- L'hospitalisation et l'acte chirurgical ne sont pas des modules métier (pas de planning de lits, pas de planning de bloc) : ce sont uniquement des types d'actes facturables, au même titre que « consultation ». Il ne faut donc pas concevoir d'entités Hospitalisation ou Intervention dans le cœur du sujet — seulement un champ type sur l'acte facturé.
- Pharmacie, laboratoire et bloc opératoire sont des bonus. Ils ne doivent influencer ni le planning des quatre semaines, ni la complexité du modèle de données du cœur, si ce n'est en gardant les entités du cœur extensibles.

### 1.3 Contraintes et bonnes pratiques retenues

Pour ce type de projet (Spring Boot + Angular + PostgreSQL, application interne de gestion), l'architecture en couches Controller → Service → Repository reste l'approche standard et la plus documentée pour un projet de cette taille et de cette durée ; des alternatives comme l'architecture hexagonale ciblent des projets plus longs avec un besoin fort d'isolation du métier, ce qui dépasse le cadre d'un stage d'un mois.

Un dossier médical réel est soumis à des règles strictes de protection des données de santé (secret médical, durée de conservation légale, hébergement sécurisé). Sans viser une conformité réglementaire complète — hors périmètre d'un stage étudiant — la conception adopte dès le départ des réflexes protecteurs peu coûteux, détaillés en section 8.


## 2. Acteurs et cas d'utilisation

Le patient n'est pas un utilisateur du système : il n'apparaît que comme donnée gérée par les rôles ci-dessous, il n'existe pas d'espace patient. Le cœur du sujet prévoyait trois rôles ; la pharmacie interne puis l'administration en ont ajouté deux (mise à jour du 6 octobre 2026).


| Acteur | Rôle applicatif | Cas d'utilisation principaux |
|---|---|---|
| Accueil / Caisse | ACCUEIL | Gestion des fiches patients ; prise de rendez-vous avec création rapide du patient et choix d'un créneau libre du médecin ; modification, annulation et suppression de rendez-vous ; factures liées au rendez-vous ou à l'acte programmé ; encaissement avec référence de paiement ; suivi des factures à payer et payées ; présence des médecins ; export / import des dossiers ; soins (injection, perfusion, pansement...) et leur facture ; contrôle gratuit proposé automatiquement. |
| Médecin | MEDECIN | Son planning ; démarrer puis terminer une consultation ; compte-rendu ; ordonnance à partir du stock (recherche par nom ou famille) ; programmation d'actes (chirurgie, traitement, examen...) et compte-rendu de leur réalisation ; programmation du rendez-vous de contrôle ; réalisation des soins ; dossiers de ses patients. |
| Pharmacien | PHARMACIEN | Stock (import / export, fiche produit, approvisionnement, inventaire) ; vente des ordonnances avec paiement obligatoire ; finances de la pharmacie. |
| Direction (DG) | DIRECTION | Tableaux de bord (consultations, chiffre d'affaires par type d'acte, impayés, activité par médecin) ; présence des médecins ; lecture des dossiers, actes, factures et de la pharmacie ; grille tarifaire (tarifs par spécialité, soins, actes) et règle du contrôle gratuit ; employés et sessions. |
| Administrateur | ADMIN | Comptes et rôles ; sessions et appareils connectés (révocation) ; journal d'activité ; qui s'est connecté ou non ; coordonnées du cabinet ; lecture de toutes les données sans les comptes-rendus médicaux. |


## 3. Processus métier clé — le fil conducteur

Ce parcours forme la colonne vertébrale du stage : chaque semaine du planning (section 10) construit une portion de ce parcours de bout en bout.

- L'accueil retrouve le patient (nom, prénom, téléphone) ou crée sa fiche dans le même formulaire que le rendez-vous.
- L'accueil choisit un créneau libre du médecin (durée de 15 à 90 minutes) ; un créneau déjà réservé ne peut pas être attribué à un autre patient tant que le premier rendez-vous n'est pas annulé ou supprimé. Le rendez-vous est créé au statut PLANIFIE avec un numéro de file.
- Dès la prise de rendez-vous, l'accueil peut rédiger la facture liée au rendez-vous ; le patient paie tout de suite ou après sa consultation.
- Le médecin démarre la consultation à l'arrivée du patient (EN_COURS), rédige le compte-rendu et la termine (TERMINE).
- Le médecin rédige l'ordonnance en cherchant les médicaments du stock ; elle apparaît chez le pharmacien. Il peut programmer un acte (chirurgie, traitement...) à une date et une heure, puis en enregistrer le résultat.
- Le pharmacien vend l'ordonnance : le stock diminue, une facture pharmacie est créée et le paiement (moyen et référence) est enregistré.
- L'accueil / la caisse encaisse les factures (espèces, Bankily, Masrvi, Sedad, carte, virement, chèque) ; la référence est obligatoire hors espèces et le statut est recalculé automatiquement.
- La direction et l'administrateur suivent l'activité, la présence des médecins, les connexions et le journal d'activité.

## 4. Modèle de données


### 4.1 Entités du cœur


| Entité | Rôle | Points clés |
|---|---|---|
| Utilisateur | Compte de connexion | Rôle (ACCUEIL / MEDECIN / PHARMACIEN / DIRECTION / ADMIN), nom, prénom, téléphone, état actif, dernière connexion, mot de passe hashé (BCrypt). |
| Medecin | Extension d'un utilisateur de rôle MEDECIN | Relation un-à-un avec Utilisateur ; sépare les données professionnelles (spécialité, n° d'ordre) du compte de connexion. |
| Patient | Fiche patient | Identité et coordonnées ; pas de compte de connexion. |
| RendezVous | Créneau de consultation | Lie Patient et Medecin ; durée du créneau, numéro de file, heures réelles de début et de fin de consultation ; porte un statut (voir 4.2). |
| Consultation | Compte-rendu médical | Relation un-à-un avec RendezVous : un rendez-vous honoré donne lieu à une consultation. |
| Prescription | Ordonnance | Relation un-à-un avec Consultation. |
| LignePrescription | Un médicament de l'ordonnance | Relation un-à-plusieurs avec Prescription — prépare naturellement l'extension pharmacie (section 9). |
| CatalogueActe | Référentiel des actes facturables | Table de référence : libellé, type, montant par défaut ; alimente les listes déroulantes côté facturation. |
| Facture | Facture patient | Peut regrouper plusieurs actes ; porte un statut de paiement. Rattachée à son origine : un rendez-vous, un acte programmé ou une dispensation (une seule facture active par rendez-vous ou par acte). Motif, date et auteur d'annulation. |
| LigneFacture | Un acte facturé | Relation un-à-plusieurs avec Facture. Référence optionnelle vers CatalogueActe : le montant peut être ajusté au cas par cas. |
| Paiement | Un encaissement | Relation un-à-plusieurs avec Facture (paiements partiels). Moyen (espèces, Bankily, Masrvi, Sedad, carte, virement, chèque) et référence de transaction, obligatoire hors espèces. |
| ActeProgramme | Acte décidé par le médecin | Chirurgie, traitement, examen, hospitalisation, soins : date, durée, lieu, détails ; statut, résultat (réussi, partiel, échec) et compte-rendu de réalisation. Occupe l'agenda du médecin. |
| Medicament | Produit de la pharmacie | Famille thérapeutique, stock, seuil d'alerte, prix d'achat et de vente, fournisseur, expiration. |
| Dispensation | Vente d'une ordonnance | Une seule par prescription ; lignes vendues ; facture pharmacie générée et payée. |
| MouvementStock | Historique du stock | Stock initial, achat, vente, ajustement : quantité signée, stock résultant, valeur, référence, auteur. |
| SessionUtilisateur | Connexion | Une par jeton JWT émis : appareil, navigateur, système, adresse IP, dernière activité, fin (déconnexion, révocation, expiration). |
| JournalActivite | Traçabilité | Qui a fait quoi, quand et d'où ; jamais de donnée médicale. |
| ParametresCabinet | Coordonnées du cabinet | Nom, adresse, téléphone, email imprimés sur tous les documents. |


### 4.2 États du rendez-vous

Le champ statut de RendezVous prend les valeurs PLANIFIE, CONFIRME, EN_COURS, TERMINE, ANNULE ou ABSENT. Séquence : PLANIFIE → CONFIRME (accueil) → EN_COURS (le médecin démarre) → TERMINE (le médecin enregistre le compte-rendu). ANNULE et ABSENT libèrent le créneau. Un rendez-vous terminé ou annulé est clos ; un rendez-vous facturé ne peut être annulé qu'après annulation de sa facture. Un médecin n'a qu'une consultation en cours à la fois.


### 4.3 États de la facture

Le champ statut de Facture prend les valeurs EN_ATTENTE, PARTIELLE, PAYEE ou ANNULEE. Ce statut est recalculé côté serveur à chaque insertion d'un Paiement : la somme des paiements comparée au montant total détermine si la facture reste PARTIELLE ou passe à PAYEE. L'annulation exige un motif et est refusée dès qu'un paiement a été encaissé.


### 4.3 bis États d'un acte programmé

PLANIFIE → REALISE (le médecin saisit le résultat et le compte-rendu) ou ANNULE (médecin ou accueil, avec motif, refusé si l'acte est déjà facturé).

### 4.3 ter États d'un soin

EN_ATTENTE (enregistré par l'accueil) → EN_COURS (démarré par l'accueil ou un médecin) → TERMINE (observations). ANNULE possible tant que le soin n'est ni terminé ni facturé. Une seule facture active par soin.

### 4.3 quater Consultation de contrôle gratuite

Un rendez-vous de contrôle référence la consultation payée qui l'ouvre (`rendez_vous_origine_id`). Il est gratuit si : la règle de la direction est active, c'est le même patient et le même médecin, la consultation d'origine a eu lieu et sa facture est payée, le contrôle tombe dans le délai (30 jours par défaut) et le nombre de contrôles autorisés (1 par défaut) n'est pas atteint. L'accueil voit le droit au moment de la prise de rendez-vous et peut y renoncer ; le médecin peut programmer le contrôle en fin de consultation. La facture du contrôle vaut 0 MRU et est soldée à sa création.


### 4.4 Pourquoi ce découpage plutôt qu'un modèle plus simple

Un modèle plus simple (une seule table Acte sans catalogue, une facture réduite à un seul montant) suffirait fonctionnellement, mais empêcherait de répondre proprement à deux exigences explicites du sujet : les tableaux de bord « chiffre d'affaires par type d'acte » (qui nécessitent que chaque ligne facturée porte un type) et le suivi des impayés avec paiements partiels (qui nécessite de séparer Facture et Paiement, liées en un-à-plusieurs).


## 5. Architecture technique


### 5.1 Vue d'ensemble

L'application suit une architecture trois tiers classique : un client Angular (SPA) consomme une API REST Spring Boot en JSON, authentifiée par un jeton JWT transmis dans l'en-tête Authorization ; l'API Spring Boot dialogue avec la base PostgreSQL via JPA/Hibernate.


### 5.2 Back-end Spring Boot — organisation en couches


| Package | Contenu |
|---|---|
| config | CORS, sécurité, intercepteur du journal d'activité (JournalInterceptor). |
| security | SecurityConfig, filtre JWT (vérifie aussi la session), UserDetailsService, SessionService (sessions et appareils). |
| rapport | Génération des PDF avec JasperReports : gabarit unique `rapports/rapport-sections.jrxml` (dossier patient, inventaire pharmacie). |
| controller | Contrôleurs REST, un par ressource (PatientController, RendezVousController, ...). |
| dto | Objets d'entrée et de sortie de l'API ; les entités JPA ne sont jamais exposées directement. |
| service | Interfaces et implémentations, logique métier (par exemple le calcul du statut de facture). |
| repository | Interfaces Spring Data JPA (JpaRepository). |
| entity | Entités JPA, mapping des tables. |
| exception | Exceptions métier et gestionnaire global (@ControllerAdvice) pour des réponses d'erreur homogènes. |
| mapper | Conversion Entité ↔ DTO (MapStruct ou mapping manuel). |
| resources/db/migration | Scripts Flyway versionnés (V1__init.sql, V2__seed_data.sql, ...). |

Points de conception importants :

- Les contrôleurs ne renvoient jamais les entités JPA brutes, ce qui évite les boucles de sérialisation et l'exposition de champs sensibles comme le mot de passe hashé : on passe systématiquement par des DTO.
- La sécurité est appliquée à deux niveaux : filtrage des routes dans SecurityConfig et annotations @PreAuthorize au niveau des méthodes de service, pour éviter qu'un oubli de configuration n'ouvre un accès.
- Flyway (ou Liquibase) versionne le schéma dès la semaine 1 : chaque évolution du modèle est un script numéroté, jamais une modification manuelle de la base.

### 5.3 Front-end Angular — organisation en modules


| Dossier | Contenu |
|---|---|
| core/auth | AuthService, AuthGuard, RoleGuard, intercepteur JWT. |
| core/models | Interfaces TypeScript (Patient, RendezVous, Facture, ...). |
| shared | Composants réutilisables : barre de navigation, tableau paginé, modales de confirmation. |
| features/auth | Écran de connexion. |
| features/accueil | Patients, prise de rendez-vous (créneaux, création rapide du patient), planning. |
| features/facturation | Factures à payer / payées / annulées, création liée au rendez-vous ou à l'acte, encaissement. |
| features/medecin | Planning médecin, dossier patient, consultation (démarrer, terminer, ordonnance, programmation d'actes). |
| features/actes | Actes programmés : réalisation, facturation, annulation. |
| features/pharmacie | Stock, fiche produit, vente des ordonnances, finances. |
| features/admin | Tableau de bord, utilisateurs, sessions, journal, permissions, coordonnées du cabinet. |
| features/direction | Tableau de bord. |
| features/impression | Documents imprimables : facture, reçu, ordonnance, ticket, acte, dossier. |
| shared | Coquille (navigation par rôle), présence des médecins, recherche de médicaments. |

Points de conception importants :

- Un module Angular par rôle (accueil, medecin, direction), chargé en lazy loading : chaque profil ne télécharge que le code dont il a besoin, et le RoleGuard empêche l'accès direct par URL à un module non autorisé.
- Les appels HTTP passent par des services dédiés par ressource (PatientService, RendezVousService, FactureService) qui encapsulent les endpoints et exposent des Observable.
- Un intercepteur HTTP ajoute automatiquement le jeton JWT à chaque requête et redirige vers l'écran de connexion en cas de réponse 401.
- Les formulaires (fiche patient, prise de rendez-vous, facture) utilisent les formulaires réactifs (ReactiveFormsModule) pour la validation.

### 5.4 Sécurité et authentification

- Authentification stateless par JWT : l'endpoint de connexion renvoie un jeton signé contenant l'identifiant et le rôle de l'utilisateur.
- Mots de passe hashés avec BCrypt, jamais stockés ni renvoyés en clair.
- Chaque requête protégée passe par un filtre qui valide le jeton et alimente le contexte de sécurité Spring.
- Autorisation par rôle, appliquée à la fois sur les routes et sur les méthodes de service.
- Chaque connexion ouvre une session (appareil, navigateur, adresse IP) dont l'identifiant est porté par le jeton (claim `jti`) : la déconnexion, la révocation par l'administrateur ou la désactivation du compte rendent le jeton immédiatement inutilisable.
- Toutes les actions qui modifient des données, les connexions (y compris les échecs) et les exports PDF sont tracés dans le journal d'activité.
- Les erreurs suivent un format unique (GlobalExceptionHandler) ; le message est affiché tel quel dans l'interface.

## 6. Spécification des API REST


### 6.1 Authentification


| Méthode | Endpoint | Accès | Description |
|---|---|---|---|
| POST | /api/auth/login | Public | Authentification : ouvre une session (appareil, IP) et retourne un jeton JWT qui la référence. |
| POST | /api/auth/logout | Connecté | Ferme la session ; le jeton n'est plus accepté. |
| GET | /api/auth/ping | Connecté | Signal de présence (dernière activité). |


### 6.2 Patients


| Méthode | Endpoint | Rôles | Description |
|---|---|---|---|
| GET | /api/patients?q= | ACCUEIL, MEDECIN, DIRECTION, ADMIN | Recherche et liste des patients. |
| GET | /api/patients/{id} | ACCUEIL, MEDECIN, DIRECTION, ADMIN | Fiche patient. |
| POST | /api/patients | ACCUEIL | Créer un patient. |
| PUT | /api/patients/{id} | ACCUEIL | Modifier un patient. |
| DELETE | /api/patients/{id} | ACCUEIL | Supprimer un patient sans historique (409 sinon). |
| GET | /api/patients/{id}/historique | ACCUEIL, MEDECIN (ses patients), DIRECTION, ADMIN | Dossier complet ; compte-rendu masqué pour l'accueil et l'administrateur. |
| GET | /api/patients/{id}/dossier.pdf | ACCUEIL, MEDECIN, DIRECTION, ADMIN | Dossier complet en PDF (JasperReports). |
| POST | /api/patients/import | ACCUEIL, MEDECIN, DIRECTION, ADMIN | Import d'un dossier (JSON de transfert), sans doublon. |


### 6.3 Rendez-vous


| Méthode | Endpoint | Rôles | Description |
|---|---|---|---|
| GET | /api/rendezvous?... | ACCUEIL, MEDECIN (le sien), DIRECTION, ADMIN | Planning filtré par médecin, date ou statut ; inclut la facture du rendez-vous. |
| GET | /api/rendezvous/creneaux | Tous sauf pharmacien | Créneaux d'un médecin pour un jour (libres, réservés, passés). |
| POST | /api/rendezvous | ACCUEIL | Créer un rendez-vous, avec un patient existant ou un nouveau patient créé en même temps. |
| PUT | /api/rendezvous/{id} | ACCUEIL | Modifier ou reprogrammer un rendez-vous planifié ou confirmé. |
| DELETE | /api/rendezvous/{id} | ACCUEIL | Supprimer un rendez-vous sans consultation ni facture (libère le créneau). |
| PATCH | /api/rendezvous/{id}/statut | ACCUEIL, MEDECIN | Accueil : confirmer, annuler, absent ; médecin : démarrer (EN_COURS), terminer, absent. |


### 6.4 Consultations, prescriptions et actes programmés


| Méthode | Endpoint | Rôles | Description |
|---|---|---|---|
| POST | /api/rendezvous/{id}/consultation | MEDECIN | Compte-rendu d'une consultation démarrée ; termine le rendez-vous. |
| GET | /api/consultations/{id} | MEDECIN | Détail d'une consultation. |
| POST | /api/consultations/{id}/prescriptions | MEDECIN | Ajouter une ordonnance (lignes reliées ou non au stock). |
| GET | /api/ordonnances, /api/ordonnances/{id} | Tous (médecin : les siennes) | Ordonnances et statut de délivrance. |
| GET | /api/actes, /api/actes/{id} | ACCUEIL, MEDECIN (les siens), DIRECTION, ADMIN | Actes programmés. |
| POST / PUT | /api/actes, /api/actes/{id} | MEDECIN | Programmer ou modifier un acte (créneau vérifié). |
| POST | /api/actes/{id}/realisation | MEDECIN | Résultat, compte-rendu et date de réalisation. |
| POST | /api/actes/{id}/annulation | MEDECIN, ACCUEIL | Annulation avec motif (refusée si facturé). |


### 6.5 Catalogue des actes


| Méthode | Endpoint | Rôles | Description |
|---|---|---|---|
| GET | /api/actes-catalogue | ACCUEIL, MEDECIN, DIRECTION, ADMIN | Liste du référentiel des actes. |
| POST | /api/actes-catalogue | DIRECTION | Ajouter un tarif (avec spécialité pour une consultation). |
| PUT | /api/actes-catalogue/{id} | DIRECTION | Modifier, activer ou désactiver un tarif. |
| DELETE | /api/actes-catalogue/{id} | DIRECTION | Supprimer un tarif jamais facturé (sinon 409 : le désactiver). |
| GET / PUT | /api/parametres-cabinet/controle-gratuit | Lecture : tous ; modification : DIRECTION, ADMIN | Règle du contrôle gratuit (actif, délai, nombre). |
| GET | /api/rendezvous/controle-gratuit | ACCUEIL, MEDECIN, DIRECTION, ADMIN | Droit du patient à un contrôle gratuit avec ce médecin à cette date. |
| POST | /api/rendezvous/{id}/controle | MEDECIN propriétaire | Programmer le rendez-vous de contrôle en fin de consultation. |

### 6.5 bis Soins

| Méthode | Endpoint | Rôles | Description |
|---|---|---|---|
| GET | /api/soins?date&patientId&statut | ACCUEIL, MEDECIN, DIRECTION, ADMIN | Soins du jour ou d'un patient. |
| POST / PUT | /api/soins, /api/soins/{id} | ACCUEIL, MEDECIN | Enregistrer (patient existant ou créé sur place) ou modifier un soin en attente. |
| POST | /api/soins/{id}/demarrer, /terminer, /annulation | ACCUEIL, MEDECIN | Cycle de vie du soin. |


### 6.6 Facturation et paiements


| Méthode | Endpoint | Rôles | Description |
|---|---|---|---|
| GET | /api/factures?... | Tous les rôles | Liste et suivi des factures, avec leur origine. |
| GET | /api/factures/{id} | Tous les rôles | Détail d'une facture (lignes, paiements avec référence). |
| POST | /api/factures | ACCUEIL, PHARMACIEN | Créer une facture, rattachée ou non à un rendez-vous / un acte, encaissée immédiatement si un paiement est fourni. |
| POST | /api/factures/{id}/paiements | ACCUEIL, PHARMACIEN | Enregistrer un paiement (moyen et référence) ; met à jour le statut. |
| POST | /api/factures/{id}/annulation | ACCUEIL | Annuler une facture sans paiement, motif obligatoire. |


### 6.7 Tableau de bord, présence et administration


| Méthode | Endpoint | Rôles | Description |
|---|---|---|---|
| GET | /api/dashboard/consultations | DIRECTION, ADMIN | Nombre de consultations sur une période. |
| GET | /api/dashboard/chiffre-affaires | DIRECTION, ADMIN | Chiffre d'affaires agrégé par type d'acte. |
| GET | /api/dashboard/impayes | DIRECTION, ADMIN | Taux et liste des factures impayées. |
| GET | /api/dashboard/activite-medecins | DIRECTION, ADMIN | Nombre de consultations par médecin. |
| GET | /api/medecins/presence | ACCUEIL, DIRECTION, ADMIN | Médecins absents, disponibles, en consultation ou en retard. |
| GET / POST / PUT / PATCH | /api/admin/utilisateurs... | ADMIN | Comptes, rôles, activation, mot de passe. |
| GET / DELETE | /api/admin/sessions... | ADMIN | Sessions et appareils, révocation. |
| GET | /api/admin/journal, /api/admin/tableau-de-bord, /api/admin/permissions | ADMIN | Journal d'activité, connexions du jour, matrice des droits. |
| GET / PUT | /api/parametres-cabinet | Tous / ADMIN | Coordonnées imprimées sur les documents. |


### 6.8 Pharmacie


| Méthode | Endpoint | Rôles | Description |
|---|---|---|---|
| GET | /api/pharmacie/medicaments | MEDECIN, PHARMACIEN, DIRECTION, ADMIN | Stock (famille, prix, seuil). |
| GET | /api/pharmacie/medicaments/{id} | PHARMACIEN, DIRECTION, ADMIN | Fiche produit : ventes, achats, marge, mouvements. |
| POST / PUT | /api/pharmacie/medicaments, .../import, .../{id} | PHARMACIEN, DIRECTION | Ajout, import, modification. |
| PATCH / POST | .../{id}/stock, .../{id}/approvisionnements | PHARMACIEN, DIRECTION | Inventaire et achats, tracés en mouvements. |
| GET | /api/pharmacie/prescriptions | PHARMACIEN, DIRECTION, ADMIN | Ordonnances à délivrer. |
| POST | /api/pharmacie/dispensations | PHARMACIEN | Vente : stock, facture et paiement obligatoire. |
| GET | /api/pharmacie/finances, .../inventaire.pdf | PHARMACIEN, DIRECTION, ADMIN | Finances de la période ; inventaire PDF. |


## 7. Matrice des droits par rôle


| Ressource | ACCUEIL | MEDECIN | PHARMACIEN | DIRECTION | ADMIN |
|---|---|---|---|---|---|
| Fiche patient — créer / modifier | Oui | Non | Non | Non | Non |
| Dossier patient — lecture | Oui (sans compte-rendu) | Oui (ses patients) | Non | Oui | Oui (sans compte-rendu) |
| Rendez-vous — créer / modifier / supprimer | Oui | Non | Non | Non | Non |
| Rendez-vous — démarrer / terminer | Non | Oui (les siens) | Non | Non | Non |
| Planning | Oui (tous) | Oui (le sien) | Non | Lecture | Lecture |
| Compte-rendu / ordonnance / acte programmé | Non | Oui | Non | Lecture | Lecture (sans rapport) |
| Facture — créer / encaisser | Oui | Non | Oui (pharmacie) | Non | Non |
| Facture — annuler | Oui | Non | Non | Non | Non |
| Pharmacie — stock et vente | Non | Lecture du stock | Oui | Lecture et gestion du stock | Lecture |
| Tableau de bord direction | Non | Non | Non | Oui | Oui |
| Présence des médecins | Oui | Non | Non | Oui | Oui |
| Comptes, sessions, journal, permissions | Non | Non | Non | Employés et sessions (hors comptes ADMIN) | Oui |
| Soins — enregistrer / réaliser | Oui (et facturer) | Oui | Non | Lecture | Lecture |
| Grille tarifaire et règle du contrôle gratuit | Lecture | Lecture | Non | Oui | Règle uniquement |
| Rendez-vous de contrôle | Proposé à la prise de RDV | Programmation en fin de consultation | Non | Lecture | Lecture |
| Apparence de l'interface (nom, logo, couleurs) | Non | Non | Non | Oui | Oui |

Cette matrice se traduit directement en annotations @PreAuthorize côté back-end et en gardes de route (RoleGuard) côté front-end : les deux doivent rester synchronisés, le front-end masque l'interface mais le back-end reste la seule source de vérité pour la sécurité réelle.


## 8. Recommandations de sécurité et de qualité

Sans viser une conformité réglementaire complète des données de santé (hors périmètre d'un stage étudiant), quelques réflexes simples et peu coûteux sont à intégrer dès la conception :

- Traçabilité minimale : chaque rendez-vous, facture et paiement porte un champ « créé par » / « enregistré par », utile pour répondre à « qui a fait quoi » en cas de litige.
- Principe du moindre privilège : un médecin n'accède qu'aux patients avec lesquels il a un rendez-vous, ce filtrage doit être fait côté service, pas seulement côté interface.
- Aucune donnée médicale (compte-rendu, diagnostic, prescription) ne doit apparaître dans les journaux applicatifs.
- Mots de passe hashés (BCrypt) dès la semaine 1, jamais de mot de passe en clair, même dans le jeu de données de démonstration.
- Validation côté serveur systématique sur les DTO : ne jamais faire confiance uniquement à la validation du formulaire Angular.
- Réalisé : journal d'activité (connexions, échecs de connexion, créations, paiements, annulations, exports), sessions révocables et désactivation immédiate d'un compte ; le compte-rendu médical est masqué pour l'accueil et l'administrateur.
Ces limites assumées (conformité RGPD complète hors périmètre du stage) peuvent être explicitement mentionnées dans le README du projet.


## 9. Extensions bonus — impact sur le modèle

Le modèle du cœur est conçu pour absorber les extensions sans refonte :

- Pharmacie : LignePrescription existe déjà avec un champ médicament en texte libre. L'extension consiste à ajouter une entité Médicament (catalogue et stock) et une entité Dispensation qui référence LignePrescription et décrémente le stock. Aucune modification du cœur n'est nécessaire.
- Laboratoire : ajouter une entité Examen liée à Consultation (un-à-plusieurs), avec un statut (demandé, en cours, résultat disponible) et un champ résultat.
- Bloc opératoire : ajouter une entité Intervention liée à Patient et Médecin, avec créneau et salle — indépendante du cœur, à ne démarrer qu'en tout dernier.
Rappel du sujet : ces trois modules ne doivent être commencés qu'une fois les points 1 à 6 terminés et stables. Un cœur complet et propre est préférable à des extensions inachevées.

État au 6 octobre 2026 : le cœur étant terminé, la **pharmacie** est livrée (Médicament avec famille, Dispensation, MouvementStock, vente avec paiement, finances), ainsi qu'une version simple du bloc opératoire sous la forme d'**actes programmés** (chirurgie, traitement, examen, hospitalisation, soins) rattachés au patient, au médecin et à la consultation, avec créneau, lieu, résultat et facturation. Le laboratoire reste à faire.


## 10. Planning détaillé affiné (4 semaines)


### Semaine 1 — Fondations

- Jours 1-2 : mise en place du projet Spring Boot (structure de packages, PostgreSQL, Flyway), du projet Angular (structure de modules), Git initialisé.
- Jours 2-3 : script Flyway V1__init.sql à partir du modèle de données, entités JPA et repositories.
- Jours 3-4 : authentification JWT (Spring Security), écran de connexion Angular, AuthGuard et RoleGuard.
- Jour 5 : endpoints Patient (CRUD) et écran Angular « Fiche patient ».

### Semaine 2 — API complète du cœur

- Jours 1-2 : endpoints RendezVous (création, planning filtré par médecin ou date, changement de statut).
- Jour 3 : endpoints Consultation et Prescription, sécurisés par rôle MEDECIN.
- Jours 4-5 : endpoints Facture (multi-lignes), Paiement (calcul automatique du statut), CatalogueActe. Tests des endpoints via Postman, collection conservée comme livrable.

### Semaine 3 — Front-end Angular

- Jours 1-2 : module accueil — recherche et création de patient, prise de rendez-vous, planning.
- Jour 3 : module accueil — écran facturation et encaissement.
- Jours 4-5 : module médecin — planning, dossier patient, formulaire de compte-rendu et de prescription. Branchement complet sur l'API, gestion des erreurs et des états de chargement.

### Semaine 4 — Pilotage, finitions, livrables

- Jours 1-2 : endpoints du tableau de bord (requêtes agrégées) et module direction (indicateurs : consultations, chiffre d'affaires par type d'acte, taux d'impayés, activité par médecin).
- Jour 3 : jeu de données de démonstration réaliste (script Flyway V2__seed_data.sql) couvrant les trois rôles, plusieurs médecins, patients, rendez-vous à différents statuts, factures payées, partielles et impayées.
- Jour 4 : rédaction du README (installation back, front, base), export du schéma de base de données.
- Jour 5 : recette globale du parcours de bout en bout, corrections, préparation de la démonstration. Extension pharmacie uniquement si tout le reste est stable.

## 11. Livrables attendus

- Dépôt Git avec un historique de commits réguliers.
- README d'installation : back-end, front-end, base de données, variables d'environnement.
- Scripts Flyway constituant un schéma versionné et reproductible.
- Schéma de la base de données (export ou diagramme).
- Jeu de données de démonstration couvrant tous les rôles, avec des noms mauritaniens (migration V13).
- Collection Postman des endpoints (`postman/cabinet-medical.postman_collection.json`, 80 requêtes).
- Tests d'intégration des règles métier et des droits (20 tests).
- Démonstration du parcours complet, à date fixe.

## 12. Synthèse

Le cœur du sujet — authentification par rôle, patients, rendez-vous, dossier médical, facturation multi-actes, tableau de bord — forme un parcours cohérent de bout en bout qui peut être développé progressivement, semaine après semaine, sans dépendance externe bloquante. Le modèle de données est volontairement minimal mais structuré pour que les extensions bonus s'y greffent sans refonte, conformément à la consigne de cadrage du sujet : mieux vaut un cœur complet et propre qu'un ensemble de modules inachevés.

## 13. Internationalisation, personnalisation et mise à jour

### 13.1 Interface en arabe

- **Choix d'architecture** : plutôt que de réécrire chaque gabarit avec des clés de traduction, l'interface est écrite en français et traduite à l'affichage. `TraductionService` observe le DOM (`MutationObserver`) et remplace chaque texte et attribut (`placeholder`, `title`, `aria-label`) par sa traduction tirée du dictionnaire `core/i18n/ar.ts` (≈ 1 400 entrées, chargé à la demande). Le texte d'origine est gardé : le retour au français est immédiat. Les textes composés de valeurs dynamiques sont traduits expression par expression (y compris les noms de jours et de mois).
- **Lecture de droite à gauche** : `dir="rtl"` sur `<html>`, CSS converties en propriétés logiques (`*-inline-start` / `*-inline-end`), police Cairo, pas d'espacement entre lettres en arabe.
- **Limites assumées** : les données saisies (noms, motifs, libellés du catalogue créés par l'utilisateur) ne sont pas traduites ; un nouveau texte de l'interface doit être ajouté au dictionnaire.

### 13.2 Personnalisation de l'interface

- Données : colonnes `nom_interface`, `couleur_principale`, `couleur_accent`, `couleur_bouton` (format `#RRGGBB` contrôlé en base et dans l'API) et `logo` (data URL d'image, 300 Ko au plus) dans `parametres_cabinet` (V16).
- API : `GET /api/parametres-cabinet/apparence` public (la page de connexion doit afficher le nom et le logo avant authentification), `PUT` réservé à ADMIN et DIRECTION, journalisé.
- Frontend : `ApparenceService` applique les couleurs en variables CSS sur `<html>` (`--brand`, `--brand-dark`, `--brand-soft`, `--accent`..., nuances calculées), met à jour le titre et l'icône de l'onglet, et garde la dernière apparence sur le poste pour l'afficher dès le chargement. L'aperçu est appliqué en direct, sans enregistrement, et annulé si l'on quitte la page.

### 13.3 Appliquer une mise à jour

Les migrations Flyway s'appliquent automatiquement au démarrage du backend : après `git pull`, **redémarrer le backend**, puis recharger le navigateur (Ctrl+F5). Une migration appliquée n'est jamais modifiée (contrôle d'empreinte) ; chaque évolution de schéma est une nouvelle migration. Le détail des commandes est dans le README.

## 14. Problèmes rencontrés et enseignements

Les problèmes rencontrés pendant le développement sont tracés dans des issues GitHub fermées ([https://github.com/maminelalle/medical-cabinet-management/issues](https://github.com/maminelalle/medical-cabinet-management/issues?q=is%3Aissue)) et résumés dans le README (« Problèmes rencontrés et solutions »).

| Issue | Problème | Cause | Solution |
|---|---|---|---|
| [#1](https://github.com/maminelalle/medical-cabinet-management/issues/1) | Flyway refusait de démarrer (« checksum mismatch ») | Une migration déjà appliquée avait été modifiée. | Migrations V1 à V4 restaurées à l'identique ; toute évolution passe par une nouvelle migration (V5 à V16). |
| [#2](https://github.com/maminelalle/medical-cabinet-management/issues/2) | Supprimer un patient avec historique provoquait une erreur 500 | Aucune vérification métier avant la suppression. | Refus 409 avec message clair ; format d'erreur unifié (`GlobalExceptionHandler`). |
| [#3](https://github.com/maminelalle/medical-cabinet-management/issues/3) | Une facture saisie par erreur ne pouvait pas être annulée | Pas de cycle d'annulation dans le modèle. | Annulation avec motif, date et auteur (V11), refusée si un paiement existe. |
| [#4](https://github.com/maminelalle/medical-cabinet-management/issues/4) | Un créneau réservé pouvait être donné à un autre patient | Seule l'heure exacte était contrôlée, sans durée ni actes programmés. | `DisponibiliteService` contrôle les chevauchements (409) ; grille de créneaux libres / réservés / passés. |
| [#5](https://github.com/maminelalle/medical-cabinet-management/issues/5) | Bouton d'impression mal placé à côté des notifications | Action globale au lieu d'une action contextuelle. | Impression de la facture, du reçu et de l'ordonnance depuis le détail de la facture. |
| [#6](https://github.com/maminelalle/medical-cabinet-management/issues/6) | « Bonjour » affiché sans le nom à l'accueil (avec un emoji) | Le profil ne renvoyait le nom que pour les médecins. | `ProfilService` renvoie le nom de tous les comptes ; emoji supprimé. |
| [#7](https://github.com/maminelalle/medical-cabinet-management/issues/7) | Anciennes ventes pharmacie affichées comme « facture libre » | Origine déduite seulement du lien dispensation, absent des anciennes données. | Facture dont toutes les lignes sont PHARMACIE reconnue comme vente pharmacie. |
| [#8](https://github.com/maminelalle/medical-cabinet-management/issues/8) | Départ d'un employé détecté en 30 s ; fermeture de flux SSE ignorée en test | `complete()` ne déclenche pas le rappel dans MockMvc ; détection sur le battement de 30 s. | Méthode `fermer()` (retirer puis compléter) ; vérification des connexions toutes les 5 s (départ détecté en ~6 s). |
| [#9](https://github.com/maminelalle/medical-cabinet-management/issues/9) | Test et Postman attendaient un refus 403 pour la direction sur les employés | Règle d'accès élargie à la direction sans mise à jour des vérifications. | Vérifications portées sur le médecin et sur le journal d'administration. |
| [#10](https://github.com/maminelalle/medical-cabinet-management/issues/10) | Contrôle gratuit : paiement de 0 MRU impossible | Un paiement doit être strictement positif (voulu). | Facture d'un total nul soldée (PAYEE) dès sa création. |
| [#11](https://github.com/maminelalle/medical-cabinet-management/issues/11) | Mise en page cassée en arabe (droite à gauche) | CSS écrites en propriétés physiques (`margin-left`, `right`...). | Conversion en propriétés logiques, police Cairo, pas d'espacement entre lettres en arabe. |
| [#12](https://github.com/maminelalle/medical-cabinet-management/issues/12) | Noms d'exemple avec « Ould » et « Mint » | Choix des données de démonstration. | Noms simplifiés (Lalle Mohamed, Ahmed Sidi...) : migration V15, tests, documentation, Postman. |
| [#14](https://github.com/maminelalle/medical-cabinet-management/issues/14) | Impression : le tableau de bord apparaissait sur la facture ou le reçu | Règles `@media print` encapsulées dans le composant d'impression par Angular. | Règles d'impression globales : seul le document s'imprime (vérifié en PDF, français et arabe). |
| [#15](https://github.com/maminelalle/medical-cabinet-management/issues/15) | Erreur sur « outDir » dans `tsconfig.json` (VS Code) | TypeScript 6 de l'éditeur exige `rootDir` dès que `outDir` est défini. | `"rootDir": "./src"` ajouté ; `ng build` inchangé. |
| [#16](https://github.com/maminelalle/medical-cabinet-management/issues/16) | Design system : choisir un logo ou une couleur ne changeait rien | Formulaire réservé à l'admin (verrouillé pour la direction), backend non redémarré après la mise à jour, composants de démonstration statiques. | Personnalisation ouverte à la direction, avertissement « Serveur à redémarrer », page entièrement interactive. |

Enseignements pour la suite du projet :

1. **Base de données** : une migration Flyway appliquée est figée ; chaque évolution est une nouvelle migration, appliquée au redémarrage du backend.
2. **Sécurité** : toute modification d'une règle d'accès (`@PreAuthorize`) est reportée dans la matrice des droits, les tests d'intégration et la collection Postman.
3. **Frontend** : les styles qui concernent toute la page (impression, thème, langue) sont globaux ; les styles de composant sont encapsulés par Angular.
4. **Internationalisation** : propriétés CSS logiques et textes centralisés dans un dictionnaire pour ajouter une langue sans réécrire les écrans.
5. **Vérification** : chaque évolution est testée à travers l'API réelle (tests d'intégration) et dans le navigateur (recette des écrans, impression en PDF).

## 15. Évolutions réalisées

| Date | Évolution | Migration |
|---|---|---|
| 5 octobre 2026 | Dossier patient complet (consultations, ordonnances, rendez-vous, factures), import / export, page Ordonnances, impressions (facture, reçu, ordonnance, ticket, dossier) | — |
| 5 octobre 2026 | Points obligatoires de l'audit : suppression de patient protégée, modification de rendez-vous, annulation de facture, gestion globale des erreurs, tests, collection Postman, configuration | V11 |
| 6 octobre 2026 | Accueil : création rapide du patient avec le rendez-vous, grille des créneaux et refus des chevauchements, facture liée au rendez-vous et encaissement immédiat ou différé, factures à payer / payées, références de paiement (Bankily, Masrvi, Sedad...), présence des médecins | V12 |
| 6 octobre 2026 | Médecin : démarrer / terminer la consultation, ordonnance depuis le stock (recherche par nom ou famille), actes programmés avec résultat et compte-rendu | V12 |
| 6 octobre 2026 | Pharmacie : familles, mouvements de stock, fiche produit, approvisionnement, inventaire, vente avec paiement obligatoire, finances, export Excel et PDF | V12 |
| 6 octobre 2026 | Administration : rôle ADMIN, comptes, sessions et appareils, journal d'activité, permissions, coordonnées du cabinet ; exports PDF avec JasperReports | V12 |
| 6 octobre 2026 | Données de démonstration avec des noms mauritaniens | V13 |
| 7 octobre 2026 | Direction : paramètres des employés (ajout, modification, suppression, sessions) ; présence en temps réel par flux SSE | — |
| 7 octobre 2026 | Grille tarifaire de la direction (tarif de consultation par spécialité, soins), consultation de contrôle gratuite réglable, soins au cabinet (injection, perfusion, pansement, nébulisation, constantes) | V14 |
| 7 octobre 2026 | Interface bilingue français / arabe avec lecture de droite à gauche | — |
| 7 octobre 2026 | Noms de démonstration simplifiés (sans « Ould » ni « Mint ») | V15 |
| 7 octobre 2026 | Impression limitée au document (facture, reçu, ordonnance...) | — |
| 7 octobre 2026 | Personnalisation de l'interface (administrateur et direction) : nom, sous-titre, logo et couleurs appliqués à toute l'application ; design system interactif | V16 |
