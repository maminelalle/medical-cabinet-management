# Dossier de conception — Application de gestion d'un cabinet médical

Stage — Licence 3 Informatique · Durée : 1 mois · Stack imposée : Spring Boot · Angular · PostgreSQL


## Sommaire


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

Le patient n'est pas un utilisateur du système : il n'apparaît que comme donnée gérée par les trois rôles ci-dessous, il n'existe pas d'espace patient dans le périmètre du cœur.


| Acteur | Rôle applicatif | Cas d'utilisation principaux |
|---|---|---|
| Accueil / Caisse | ACCUEIL | Authentification, gestion des fiches patients, prise et modification de rendez-vous, consultation du planning des médecins, création de factures multi-actes, encaissement, suivi des statuts de paiement. |
| Médecin | MEDECIN | Authentification, consultation de son propre planning, accès au dossier et à l'historique de ses patients, rédaction de compte-rendu et de prescription. |
| Direction (DG) | DIRECTION | Authentification, consultation des tableaux de bord : consultations, chiffre d'affaires par type d'acte, taux d'impayés, activité par médecin. |


## 3. Processus métier clé — le fil conducteur

Ce parcours forme la colonne vertébrale du stage : chaque semaine du planning (section 10) construit une portion de ce parcours de bout en bout.

- L'accueil crée ou retrouve la fiche du patient qui se présente.
- L'accueil prend un rendez-vous : patient, médecin, créneau et motif ; le rendez-vous est créé au statut PLANIFIE.
- Le médecin consulte son planning du jour, puis ouvre le dossier du patient (historique des consultations précédentes).
- Le médecin rédige le compte-rendu et la prescription ; le rendez-vous passe alors au statut TERMINE.
- L'accueil / la caisse crée la facture à partir d'un ou plusieurs actes (la consultation, éventuellement d'autres actes).
- L'accueil / la caisse encaisse le paiement, total ou partiel ; le statut de la facture est recalculé automatiquement.
- La direction consulte à tout moment des indicateurs qui se mettent à jour à partir de ces mêmes données.

## 4. Modèle de données


### 4.1 Entités du cœur


| Entité | Rôle | Points clés |
|---|---|---|
| Utilisateur | Compte de connexion | Porte le champ rôle (ACCUEIL / MEDECIN / DIRECTION), mot de passe hashé (BCrypt). |
| Medecin | Extension d'un utilisateur de rôle MEDECIN | Relation un-à-un avec Utilisateur ; sépare les données professionnelles (spécialité, n° d'ordre) du compte de connexion. |
| Patient | Fiche patient | Identité et coordonnées ; pas de compte de connexion. |
| RendezVous | Créneau de consultation | Lie Patient et Medecin ; porte un statut (voir 4.2). |
| Consultation | Compte-rendu médical | Relation un-à-un avec RendezVous : un rendez-vous honoré donne lieu à une consultation. |
| Prescription | Ordonnance | Relation un-à-un avec Consultation. |
| LignePrescription | Un médicament de l'ordonnance | Relation un-à-plusieurs avec Prescription — prépare naturellement l'extension pharmacie (section 9). |
| CatalogueActe | Référentiel des actes facturables | Table de référence : libellé, type, montant par défaut ; alimente les listes déroulantes côté facturation. |
| Facture | Facture patient | Peut regrouper plusieurs actes (consultation + autres) ; porte un statut de paiement. |
| LigneFacture | Un acte facturé | Relation un-à-plusieurs avec Facture. Référence optionnelle vers CatalogueActe : le montant peut être ajusté au cas par cas. |
| Paiement | Un encaissement | Relation un-à-plusieurs avec Facture, ce qui permet les paiements partiels ou échelonnés. |


### 4.2 États du rendez-vous

Le champ statut de RendezVous prend les valeurs PLANIFIE, CONFIRME, EN_COURS, TERMINE, ANNULE ou ABSENT. Un rendez-vous suit en principe la séquence PLANIFIE → CONFIRME → EN_COURS → TERMINE ; il peut être détourné vers ANNULE ou ABSENT à tout moment avant sa tenue. Le passage à TERMINE ouvre la possibilité de créer la Consultation associée, elle-même préalable à la facturation.


### 4.3 États de la facture

Le champ statut de Facture prend les valeurs EN_ATTENTE, PARTIELLE, PAYEE ou ANNULEE. Ce statut est recalculé côté serveur à chaque insertion d'un Paiement : la somme des paiements comparée au montant total détermine si la facture reste PARTIELLE ou passe à PAYEE.


### 4.4 Pourquoi ce découpage plutôt qu'un modèle plus simple

Un modèle plus simple (une seule table Acte sans catalogue, une facture réduite à un seul montant) suffirait fonctionnellement, mais empêcherait de répondre proprement à deux exigences explicites du sujet : les tableaux de bord « chiffre d'affaires par type d'acte » (qui nécessitent que chaque ligne facturée porte un type) et le suivi des impayés avec paiements partiels (qui nécessite de séparer Facture et Paiement, liées en un-à-plusieurs).


## 5. Architecture technique


### 5.1 Vue d'ensemble

L'application suit une architecture trois tiers classique : un client Angular (SPA) consomme une API REST Spring Boot en JSON, authentifiée par un jeton JWT transmis dans l'en-tête Authorization ; l'API Spring Boot dialogue avec la base PostgreSQL via JPA/Hibernate.


### 5.2 Back-end Spring Boot — organisation en couches


| Package | Contenu |
|---|---|
| config | CORS, sécurité, documentation OpenAPI/Swagger, jeu de données de démonstration (CommandLineRunner). |
| security | SecurityConfig, filtre JWT, UserDetailsService, gestion des rôles. |
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
| features/accueil | Gestion des patients, rendez-vous, facturation, encaissement. |
| features/medecin | Planning médecin, dossier patient, consultation, prescription. |
| features/direction | Tableau de bord. |

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

## 6. Spécification des API REST


### 6.1 Authentification


| Méthode | Endpoint | Accès | Description |
|---|---|---|---|
| POST | /api/auth/login | Public | Authentification, retourne un jeton JWT. |


### 6.2 Patients


| Méthode | Endpoint | Rôles | Description |
|---|---|---|---|
| GET | /api/patients?q= | ACCUEIL, MEDECIN | Recherche et liste des patients. |
| GET | /api/patients/{id} | ACCUEIL, MEDECIN | Fiche patient. |
| POST | /api/patients | ACCUEIL | Créer un patient. |
| PUT | /api/patients/{id} | ACCUEIL | Modifier un patient. |
| GET | /api/patients/{id}/historique | MEDECIN | Historique des consultations du patient. |


### 6.3 Rendez-vous


| Méthode | Endpoint | Rôles | Description |
|---|---|---|---|
| GET | /api/rendezvous?... | ACCUEIL, MEDECIN | Liste filtrée par médecin, date ou statut (planning). |
| POST | /api/rendezvous | ACCUEIL | Créer un rendez-vous. |
| PUT | /api/rendezvous/{id} | ACCUEIL | Modifier ou annuler un rendez-vous. |
| PATCH | /api/rendezvous/{id}/statut | ACCUEIL, MEDECIN | Changer le statut (confirmé, en cours, absent...). |


### 6.4 Consultations et prescriptions


| Méthode | Endpoint | Rôles | Description |
|---|---|---|---|
| POST | /api/rendezvous/{id}/consultation | MEDECIN | Créer le compte-rendu, ce qui clôt le rendez-vous. |
| GET | /api/consultations/{id} | MEDECIN | Détail d'une consultation. |
| POST | /api/consultations/{id}/prescriptions | MEDECIN | Ajouter une prescription avec ses lignes. |


### 6.5 Catalogue des actes


| Méthode | Endpoint | Rôles | Description |
|---|---|---|---|
| GET | /api/actes-catalogue | ACCUEIL, DIRECTION | Liste du référentiel des actes. |
| POST | /api/actes-catalogue | DIRECTION | Ajouter un type d'acte. |


### 6.6 Facturation et paiements


| Méthode | Endpoint | Rôles | Description |
|---|---|---|---|
| GET | /api/factures?... | ACCUEIL, DIRECTION | Liste et suivi des factures. |
| GET | /api/factures/{id} | ACCUEIL, DIRECTION | Détail d'une facture. |
| POST | /api/factures | ACCUEIL | Créer une facture (une ou plusieurs lignes d'actes). |
| POST | /api/factures/{id}/paiements | ACCUEIL | Enregistrer un paiement, met à jour le statut. |


### 6.7 Tableau de bord (direction)


| Méthode | Endpoint | Rôles | Description |
|---|---|---|---|
| GET | /api/dashboard/consultations | DIRECTION | Nombre de consultations sur une période. |
| GET | /api/dashboard/chiffre-affaires | DIRECTION | Chiffre d'affaires agrégé par type d'acte. |
| GET | /api/dashboard/impayes | DIRECTION | Taux et liste des factures impayées. |
| GET | /api/dashboard/activite-medecins | DIRECTION | Nombre de consultations par médecin. |


## 7. Matrice des droits par rôle


| Ressource | ACCUEIL | MEDECIN | DIRECTION |
|---|---|---|---|
| Fiche patient — créer / modifier | Oui | Non | Non |
| Fiche patient — lecture | Oui | Oui (ses patients) | Non |
| Rendez-vous — créer / modifier | Oui | Statut seulement | Non |
| Planning médecin | Oui (tous) | Oui (le sien) | Non |
| Dossier médical / historique | Non | Oui | Non |
| Compte-rendu / prescription | Non | Oui | Non |
| Facture — créer | Oui | Non | Non |
| Paiement — encaisser | Oui | Non | Non |
| Tableau de bord | Non | Non | Oui |

Cette matrice se traduit directement en annotations @PreAuthorize côté back-end et en gardes de route (RoleGuard) côté front-end : les deux doivent rester synchronisés, le front-end masque l'interface mais le back-end reste la seule source de vérité pour la sécurité réelle.


## 8. Recommandations de sécurité et de qualité

Sans viser une conformité réglementaire complète des données de santé (hors périmètre d'un stage étudiant), quelques réflexes simples et peu coûteux sont à intégrer dès la conception :

- Traçabilité minimale : chaque rendez-vous, facture et paiement porte un champ « créé par » / « enregistré par », utile pour répondre à « qui a fait quoi » en cas de litige.
- Principe du moindre privilège : un médecin n'accède qu'aux patients avec lesquels il a un rendez-vous, ce filtrage doit être fait côté service, pas seulement côté interface.
- Aucune donnée médicale (compte-rendu, diagnostic, prescription) ne doit apparaître dans les journaux applicatifs.
- Mots de passe hashés (BCrypt) dès la semaine 1, jamais de mot de passe en clair, même dans le jeu de données de démonstration.
- Validation côté serveur systématique sur les DTO : ne jamais faire confiance uniquement à la validation du formulaire Angular.
Ces limites assumées (conformité RGPD complète hors périmètre du stage) peuvent être explicitement mentionnées dans le README du projet.


## 9. Extensions bonus — impact sur le modèle

Le modèle du cœur est conçu pour absorber les extensions sans refonte :

- Pharmacie : LignePrescription existe déjà avec un champ médicament en texte libre. L'extension consiste à ajouter une entité Médicament (catalogue et stock) et une entité Dispensation qui référence LignePrescription et décrémente le stock. Aucune modification du cœur n'est nécessaire.
- Laboratoire : ajouter une entité Examen liée à Consultation (un-à-plusieurs), avec un statut (demandé, en cours, résultat disponible) et un champ résultat.
- Bloc opératoire : ajouter une entité Intervention liée à Patient et Médecin, avec créneau et salle — indépendante du cœur, à ne démarrer qu'en tout dernier.
Rappel du sujet : ces trois modules ne doivent être commencés qu'une fois les points 1 à 6 terminés et stables. Un cœur complet et propre est préférable à des extensions inachevées.


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
- Jeu de données de démonstration couvrant les trois rôles.
- Collection Postman des endpoints.
- Démonstration du parcours complet, à date fixe.

## 12. Synthèse

Le cœur du sujet — authentification par rôle, patients, rendez-vous, dossier médical, facturation multi-actes, tableau de bord — forme un parcours cohérent de bout en bout qui peut être développé progressivement, semaine après semaine, sans dépendance externe bloquante. Le modèle de données est volontairement minimal mais structuré pour que les extensions bonus s'y greffent sans refonte, conformément à la consigne de cadrage du sujet : mieux vaut un cœur complet et propre qu'un ensemble de modules inachevés.
