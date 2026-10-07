# Cabinet Médical

Application interne de gestion d'un cabinet médical. Le projet centralise les patients, les rendez-vous, les consultations, les prescriptions, les actes facturables, les factures et les paiements.

## Stack technique

- Frontend : Angular 20, TypeScript, Reactive Forms, routing lazy-loaded
- Backend : Spring Boot 4, Java 17, API REST
- Base de données : PostgreSQL
- ORM : Spring Data JPA / Hibernate
- Migrations : Flyway
- Authentification : JWT
- Sécurité : Spring Security avec rôles et `@PreAuthorize`
- Rapports PDF : JasperReports 7 (dossier patient, inventaire pharmacie)
- Export Excel côté navigateur : SheetJS (xlsx)
- Tests backend : JUnit / Spring Boot Test avec H2 (20 tests d'intégration)

## Architecture

Le backend suit une architecture en couches :

```text
Controller -> Service -> Repository -> PostgreSQL
                 |
              DTO / Entity
```

Le frontend est organisé par domaines :

```text
frontend/src/
├── app/                 Routes et configuration Angular
├── core/
│   ├── auth/            AuthService, guards, interceptor JWT
│   ├── models/          Interfaces TypeScript
│   ├── patients/        PatientService
│   ├── rendez-vous/     RendezVousService
│   └── factures/        FactureService
├── features/
│   ├── auth/            Connexion
│   ├── accueil/         Dashboard, patients, rendez-vous
│   ├── facturation/     Liste, détail et création de factures
│   └── direction/       Dashboard direction
└── shared/layout/       Shell, sidebar, topbar, redirection par rôle
```

## Installation

### Prérequis

- Java 17 ou supérieur
- Node.js 20 ou supérieur
- PostgreSQL
- Maven Wrapper fourni dans `backend`

### Base PostgreSQL

Créer la base :

```sql
CREATE DATABASE cabinet_medical;
```

Le backend utilise par défaut :

```text
URL      jdbc:postgresql://localhost:5432/cabinet_medical
Utilisateur postgres
Mot de passe postgres
```

Variables d'environnement disponibles :

| Variable | Valeur par défaut | Rôle |
|---|---|---|
| `DB_URL` | `jdbc:postgresql://localhost:5432/cabinet_medical` | URL PostgreSQL |
| `DB_USERNAME` | `postgres` | Utilisateur DB |
| `DB_PASSWORD` | `postgres` | Mot de passe DB |
| `JWT_SECRET` | valeur de développement | Clé de signature JWT |
| `JWT_EXPIRATION_MS` | `86400000` | Durée de validité du token |

Flyway exécute automatiquement les migrations `V1__init.sql`, `V2__seed_data.sql` et les versions suivantes au démarrage.

### Démarrer le backend

```powershell
cd backend
./mvnw.cmd spring-boot:run
```

API disponible sur `http://localhost:8080`.

### Démarrer le frontend

```powershell
cd frontend
npm install
npm start
```

Application disponible sur `http://localhost:4200`.

### Configuration du frontend

Le fichier `frontend/src/environments/environment.ts` centralise :

- `apiUrl` : adresse de l'API (par défaut `http://localhost:8080/api`) ;
- `cabinet` : nom, sous-titre, adresse et téléphone imprimés en en-tête des factures, reçus, ordonnances et dossiers.

### Collection Postman

`postman/cabinet-medical.postman_collection.json` contient les 96 requêtes de l'API, rangées en 11 dossiers. Importer la collection dans Postman, exécuter d'abord « 0. Authentification » (un jeton est enregistré par rôle), puis les dossiers dans l'ordre : les identifiants créés sont réutilisés automatiquement. La variable `baseUrl` vaut `http://localhost:8080`.

## Comptes de démonstration

La migration de seed crée les comptes suivants. Le mot de passe est `password` pour chacun.

| Rôle | Email | Nom | Accès principal |
|---|---|---|---|
| Accueil / Caisse | `accueil@test.local` | Khadijetou Mint Salem | Patients, rendez-vous, factures, paiements |
| Médecin (médecine générale) | `medecin@test.local` | Dr. Mohamed Ould Cheikh | Planning personnel, consultations, prescriptions, dossiers |
| Médecin (cardiologie) | `cardio@test.local` | Dr. Zeinabou Mint Ahmed | Planning personnel, consultations, prescriptions, dossiers |
| Médecin (chirurgie) | `chirurgie@test.local` | Dr. Sidi Ould Brahim | Planning personnel, consultations, prescriptions, dossiers |
| Direction | `direction@test.local` | Ahmed Ould Abdallahi | Dashboard, indicateurs, lecture factures et actes |
| Pharmacien | `pharmacien@test.local` | Moulaye Ould Sidaty | Stock, fiches produits, vente des ordonnances, finances pharmacie |
| Administrateur | `admin@test.local` | El Hacen Ould Mohamed Lemine | Comptes, rôles, sessions et appareils, journal d'activité, coordonnées du cabinet |

Le mot de passe est `password` pour tous les comptes (hash BCrypt en base, jamais de mot de passe en clair).

Patients de démonstration (Nouakchott) : Lalle Ould Mohamed, Mohamed Ould Sidi, Fatimetou Mint Ahmedou, Brahim Ould Ely, Aichetou Mint Cheikh, Ahmedou Ould Bakar, Mariem Mint Moctar et Cheikh Ould Sidi Mohamed.

## Parcours par interface

### Accueil / Caisse

- **Prise de rendez-vous** (`/rendez-vous/nouveau`) : recherche du patient (nom, prénom, téléphone) ou **création rapide du patient** dans le même formulaire ; grille des créneaux du médecin (libres, réservés, passés). Un créneau réservé ne peut pas être donné à un autre patient tant que le premier rendez-vous n'est pas annulé ou supprimé (chevauchement contrôlé avec la durée du rendez-vous et les actes programmés).
- Après la prise de rendez-vous : **« Rédiger la facture maintenant »**, impression du ticket de passage ou nouveau rendez-vous. Le patient paie tout de suite ou après sa consultation.
- **Facturation** (`/factures`) : onglets **À payer / Payées / Annulées** ; chaque facture indique son origine (rendez-vous, acte programmé, pharmacie). Paiement par espèces, Bankily, Masrvi, Sedad, carte, virement ou chèque, **référence de transaction obligatoire hors espèces**.
- Planning : facture du rendez-vous (ou bouton « Facturer »), modification, suppression (libère le créneau).
- **Médecins au cabinet** (tableau de bord) : connecté ou non, en consultation, disponible ou en retard.
- **Actes programmés** (`/actes`) : chirurgies et traitements programmés par les médecins, génération de leur facture.
- **Soins et injections** (`/soins`) : le patient vient pour une injection, une perfusion, un pansement, une nébulisation ou une prise de constantes, souvent avec l'ordonnance d'un autre médecin et son propre produit. L'accueil enregistre le soin (patient existant ou créé sur place), l'équipe le **démarre** puis le **termine** avec ses observations, l'accueil le **facture** au tarif fixé par la direction. Mise à jour en temps réel sur tous les postes.
- **Contrôle gratuit** : lors de la prise de rendez-vous, si le patient a payé une consultation chez **le même médecin** dans le délai fixé par la direction (30 jours par défaut, 1 contrôle), un bandeau le signale et le rendez-vous est rattaché à cette consultation ; la facture est pré-remplie à **0 MRU** et soldée aussitôt. L'accueil peut décocher pour facturer une nouvelle consultation. Dans le planning, un badge « Contrôle gratuit » repère ces rendez-vous.
- **Tarif de consultation par spécialité** : la facture d'un rendez-vous reprend automatiquement le tarif de la spécialité du médecin (cardiologie, chirurgie...) ou, à défaut, le tarif général.

### Médecin

- Planning : **▶ Démarrer** la consultation à l'arrivée du patient, puis rédaction du compte-rendu et **■ Terminer**.
- Une fois la consultation terminée : **ordonnance** avec recherche des médicaments par nom, dosage ou **famille** et disponibilité en stock (saisie libre possible hors stock), puis **programmation d'un acte** (chirurgie, traitement, examen, hospitalisation, soins) à une date et une heure, avec détails et lieu. L'accueil et la direction le voient ; il s'imprime.
- Après l'acte : compte-rendu, **résultat (réussi, partiel, échec)** et date de réalisation ; l'accueil le facture.
- **Programmer un contrôle** en fin de consultation : le rendez-vous est rattaché à la consultation et devient gratuit pour le patient une fois celle-ci payée (hors délai, c'est un suivi facturé).
- Les médecins peuvent aussi réaliser les **soins** (injection, perfusion...).

### Direction

- **Tarifs et gratuité** (`/direction/tarifs`) : grille tarifaire complète (consultation générale et **par spécialité**, soins, hospitalisation, chirurgie, examens...) : ajout, modification, activation / désactivation, suppression d'un tarif jamais facturé. Réglage de la **consultation de contrôle gratuite** : activée ou non, délai en jours, nombre de contrôles par consultation payée.
- **Paramètres · employés** (`/direction/parametres`) : ajout, modification, désactivation et suppression des employés, sessions et appareils, rôles. Les comptes administrateurs ne sont pas visibles.
- **Présence en temps réel** : arrivées et départs des employés, médecins en consultation, disponibles ou en retard s'affichent automatiquement (flux SSE `/api/evenements`), sans recharger la page.

### Pharmacien

- Stock avec recherche par nom ou famille, **import / export Excel**, **inventaire PDF** (JasperReports).
- **Fiche produit** (`/pharmacie/medicaments/:id`) : ventes, achats, marge, valeur du stock et **historique des mouvements** ; approvisionnement fournisseur et correction d'inventaire tracés.
- **Vente d'une ordonnance** : délivrance, facture et **paiement obligatoire** (moyen et référence) enregistrés ensemble.
- Onglet **Finances** : ventes, achats, marge brute, encaissements par moyen de paiement, meilleures ventes, mouvements de la période.

### Administrateur

- Tableau de bord : comptes, **qui s'est connecté aujourd'hui et qui ne s'est pas connecté**, utilisateurs en ligne, présence des médecins, activité récente.
- **Utilisateurs** : création (y compris médecins), modification, rôle, activation / désactivation (déconnexion immédiate), réinitialisation du mot de passe.
- **Connexions et appareils** : sessions ouvertes, appareil, navigateur, système, adresse IP, dernière activité ; **révocation** d'une session.
- **Journal d'activité** : toutes les actions (connexions et échecs, créations, paiements, annulations, exports PDF...), sans aucune donnée médicale.
- **Rôles et permissions** et **coordonnées du cabinet** imprimées sur tous les documents.

### Langue arabe

- Bouton **العربية / Français** dans la barre du haut et sur la page de connexion. Le choix est mémorisé sur le poste.
- En arabe, toute l'interface est traduite (navigation, écrans, boutons, messages, statuts, dates, messages d'erreur du serveur) et passe en **lecture de droite à gauche** (`dir="rtl"`, police Cairo). Les feuilles de style utilisent des propriétés logiques (`margin-inline-start`...) : la mise en page s'inverse d'elle-même.
- Le dictionnaire (`frontend/src/core/i18n/ar.ts`) n'est téléchargé que si l'arabe est choisi ; les données saisies (noms, motifs) restent telles quelles.

### Export et import des dossiers

- **Export PDF** du dossier complet (identité, consultations et ordonnances, actes programmés, rendez-vous, factures) généré par **JasperReports** (`GET /api/patients/{id}/dossier.pdf`), et inventaire pharmacie en PDF.
- Le fichier **JSON de transfert** reste le format réimportable dans l'application (`POST /api/patients/import`) : un PDF est un document de lecture, il ne peut pas être réimporté de façon fiable.

## Semaine 1 : fondations livrées

### Projet et base de données

- Initialisation Spring Boot et Angular.
- Configuration PostgreSQL et Flyway.
- Migration `V1__init.sql` avec les tables du cœur métier : utilisateurs, médecins, patients, rendez-vous, consultations, prescriptions, catalogue des actes, factures, lignes de facture et paiements.
- Entités JPA et repositories correspondants.

### Authentification et sécurité

- Connexion `POST /api/auth/login`.
- Mots de passe vérifiés avec BCrypt.
- Génération d'un JWT contenant l'identifiant et le rôle.
- Filtre JWT côté Spring Security.
- `AuthService`, `AuthGuard`, `RoleGuard` et intercepteur HTTP Angular.
- Autorisation backend par rôle, le frontend ne constituant pas la sécurité principale.

### Patients

- Recherche et liste des patients.
- Création, modification et suppression.
- Validation des DTO côté serveur.
- Première interface Angular de gestion des patients, ensuite intégrée au design system médical.

## Semaine 2 : API complète du cœur livrée

### Rendez-vous

- `GET /api/rendezvous` avec filtres par date, médecin et statut.
- `POST /api/rendezvous` pour créer un rendez-vous au statut `PLANIFIE`.
- `PUT /api/rendezvous/{id}` pour modifier un rendez-vous.
- `PATCH /api/rendezvous/{id}/statut` pour confirmer, démarrer, terminer, annuler ou déclarer absent.
- Détection des créneaux déjà occupés pour un médecin.
- Un médecin ne consulte que son propre planning.
- Endpoint `GET /api/medecins` pour alimenter le formulaire de prise de rendez-vous.

### Consultation et prescription

- `POST /api/rendezvous/{id}/consultation` réservé au médecin concerné.
- Passage automatique du rendez-vous à `TERMINE` après enregistrement du compte-rendu.
- `GET /api/consultations/{id}` avec contrôle du médecin propriétaire.
- `POST /api/consultations/{id}/prescriptions`.
- Une prescription peut contenir plusieurs lignes : médicament, posologie et durée.
- Les DTOs empêchent l'exposition directe des entités JPA.

### Catalogue des actes

- `GET /api/actes-catalogue` pour l'accueil et la direction.
- `POST /api/actes-catalogue` réservé à la direction.
- Chaque acte contient un libellé, un type, un montant par défaut et un état actif.
- Les types restent des types de facturation : `CONSULTATION`, `HOSPITALISATION`, `CHIRURGIE` et `AUTRE`. Aucun module hospitalisation ou bloc opératoire n'a été créé.

### Facturation et paiements

- Factures multi-lignes avec un ou plusieurs actes.
- Total calculé côté serveur à partir des lignes.
- Référence optionnelle vers le catalogue, avec possibilité d'ajuster le montant.
- `GET /api/factures` avec filtre par statut.
- `GET /api/factures/{id}` avec lignes, paiements, montant payé et reste à payer.
- `POST /api/factures` réservé à l'accueil.
- `POST /api/factures/{id}/paiements` réservé à l'accueil.
- Paiements partiels ou complets.
- Refus des paiements supérieurs au reste dû.
- Recalcul automatique du statut : `EN_ATTENTE`, `PARTIELLE`, `PAYEE` ou `ANNULEE`.

### Dossier patient côté médecin

- `GET /api/rendezvous/{id}` : contexte d'un rendez-vous (patient, date, motif, statut).
- `GET /api/patients/{id}/historique` : identité du patient, historique des consultations avec leurs ordonnances et prochains rendez-vous.
- Accès réservé au rôle `MEDECIN` et limité aux patients avec lesquels le médecin a un rendez-vous (moindre privilège appliqué côté service).
- L'écran `/dossier/:patientId` affiche la chronologie des consultations, les ordonnances détaillées et les créneaux à venir.
- L'écran `/rendez-vous/:id/consultation` permet de saisir le compte-rendu puis l'ordonnance ; si le compte-rendu existe déjà, seule l'ordonnance peut être complétée.

### Tableau de bord direction (indicateurs calculés sur données réelles)

- `GET /api/dashboard/consultations?dateDebut&dateFin` : consultations et rendez-vous de la période, répartition par médecin.
- `GET /api/dashboard/chiffre-affaires?dateDebut&dateFin` : chiffre d'affaires facturé, encaissé, restant et répartition par type d'acte (issu des lignes de facture).
- `GET /api/dashboard/impayes` : taux d'impayés et liste des factures restant à encaisser.
- `GET /api/dashboard/activite-medecins?dateDebut&dateFin` : rendez-vous et consultations par médecin.
- Tous ces endpoints sont réservés au rôle `DIRECTION` (contrôle vérifié : un médecin reçoit `403`).

### Jeu de données de démonstration

La migration `V4__demo_data.sql` (détaillée dans [SCHEMA-BDD.md](SCHEMA-BDD.md)) crée un jeu de données complet et daté relativement à l'exécution : catalogue de 11 actes facturables, 3 médecins (médecine générale, cardiologie, chirurgie), 8 patients, 14 rendez-vous (terminés, en cours, confirmés, planifiés, absent, annulé), 5 consultations avec ordonnances et 9 factures (payées, partielles, en attente, annulée) accompagnées de 6 paiements cohérents avec les statuts.

La migration `V5__pharmacie.sql` ajoute 5 références de médicaments et leur stock initial. `V6` ajoute le rôle pharmacien, `V7` relie les prescriptions existantes au stock, `V8` corrige son compte de démonstration, `V9` ajoute les prix d'achat/vente, fournisseur et expiration, `V10` ajoute le numéro de file quotidien, `V11` le motif, la date et l'auteur de l'annulation d'une facture, et `V12` le rôle administrateur, les actes programmés, les liens facture → rendez-vous / acte / dispensation, les références de paiement, les familles et mouvements de stock, les sessions et le journal d'activité, `V13` remplace les noms de démonstration par des noms mauritaniens, et `V14` ajoute la grille tarifaire par spécialité et les tarifs de soins, la règle du contrôle gratuit, le lien rendez-vous de contrôle → consultation d'origine et la table `soins`. Une dispensation décrémente le stock et crée automatiquement une facture pharmacie imprimable.

## Interface Angular livrée

### Design system

- Jetons CSS centralisés dans `src/styles.css` : surfaces, bordures, textes, couleurs fonctionnelles, rayons, ombres, gabarit et échelle typographique.
- Composants globaux réutilisables : en-têtes de page, cartes, cartes d'indicateurs, tableaux, badges de statut, boutons (principal, secondaire, discret, icône, ligne), formulaires et filtres, alertes, états vide/erreur/chargement, avatars et barres de progression.
- Coquille d'administration : barre latérale blanche repliable regroupée par domaine (pilotage, parcours patient, finance, système), topbar avec recherche, notifications, profil et déconnexion.
- Typographie Manrope, responsive design et affichage conditionnel selon le rôle.
- Page de démonstration `/design-system` présentant l'ensemble des composants (accessible à tous les rôles connectés).

### Écrans disponibles

| Route | Rôles | Fonction |
|---|---|---|
| `/login` | Tous | Connexion JWT |
| `/accueil` | ACCUEIL | Dashboard accueil, KPI, rendez-vous et actions rapides |
| `/medecin/dashboard` | MEDECIN | Tableau de bord médical, activité du jour et accès rapides |
| `/patients` | ACCUEIL, MEDECIN, DIRECTION | Liste des patients, accès au dossier, import d'un dossier (JSON) ; création/modification réservées à l'accueil |
| `/rendez-vous` | ACCUEIL, MEDECIN | Planning filtrable, état loading/error et planning médecin |
| `/direction/rendez-vous` | DIRECTION | Planning global des rendez-vous en lecture seule |
| `/rendez-vous/nouveau` | ACCUEIL | Création avec patient, médecin, date, heure et motif |
| `/rendez-vous/:id/consultation` | MEDECIN | Compte-rendu de consultation et ordonnance |
| `/dossier/:patientId` | ACCUEIL, MEDECIN, DIRECTION | Dossier complet (identité, consultations, ordonnances, rendez-vous, factures), export/import JSON et impression ; compte-rendu masqué pour l'accueil |
| `/ordonnances` | Tous les rôles | Liste des ordonnances (le médecin ne voit que les siennes), statut de délivrance, impression |
| `/impression/{type}/:id` | Selon document | Documents imprimables (`facture`, `recu`, `ordonnance`, `rendez-vous`, `dossier`) |
| `/factures` | ACCUEIL, MEDECIN, PHARMACIEN, DIRECTION | Liste, recherche et détail ; depuis le détail : imprimer la facture, le reçu de paiement ou une ordonnance du patient ; encaissement accueil/pharmacie |
| `/factures/nouveau` | ACCUEIL | Création d'une facture multi-actes |
| `/direction/dashboard` | DIRECTION | Indicateurs réels, chiffre d'affaires par type d'acte, impayés, activité par médecin |
| `/pharmacie` | PHARMACIEN, DIRECTION, ADMIN | Stock, import/export, vente des ordonnances avec paiement, finances |
| `/pharmacie/medicaments/:id` | PHARMACIEN, DIRECTION, ADMIN | Fiche produit : ventes, achats, mouvements, approvisionnement |
| `/actes` | ACCUEIL, MEDECIN, DIRECTION, ADMIN | Actes programmés : réalisation, facturation, annulation |
| `/rendez-vous/:id/modifier` | ACCUEIL | Modification / reprogrammation |
| `/admin`, `/admin/utilisateurs`, `/admin/sessions`, `/admin/journal`, `/admin/permissions`, `/admin/cabinet` | ADMIN | Administration |
| `/parametres` | Tous les rôles | Profil connecté, rôle et accès applicatifs |
| `/design-system` | Tous | Vitrine des composants de l'interface |

La redirection après connexion dépend du rôle : accueil vers `/accueil`, médecin vers `/medecin/dashboard`, pharmacien vers `/pharmacie`, direction vers `/direction/dashboard`, administrateur vers `/admin`.

## Endpoints REST

| Domaine | Méthode et endpoint | Accès |
|---|---|---|
| Auth | `POST /api/auth/login` | Public |
| Patients | `GET /api/patients`, `POST`, `PUT`, `DELETE` | Selon rôle |
| Médecins | `GET /api/medecins` | ACCUEIL, MEDECIN |
| Rendez-vous | `GET /api/rendezvous` et `GET /api/rendezvous/{id}` | ACCUEIL, MEDECIN, DIRECTION en lecture |
| Rendez-vous | `POST /api/rendezvous` | ACCUEIL |
| Rendez-vous | `PUT /api/rendezvous/{id}` (rendez-vous planifié ou confirmé uniquement) | ACCUEIL |
| Statut | `PATCH /api/rendezvous/{id}/statut` | ACCUEIL, MEDECIN |
| Consultation | `POST /api/rendezvous/{id}/consultation` | MEDECIN propriétaire |
| Consultation | `GET /api/consultations/{id}` | MEDECIN propriétaire |
| Prescription | `POST /api/consultations/{id}/prescriptions` | MEDECIN propriétaire |
| Dossier patient | `GET /api/patients/{id}/historique` | ACCUEIL (sans compte-rendu), MEDECIN (ses patients), DIRECTION |
| Dossier patient | `POST /api/patients/import` | ACCUEIL, MEDECIN, DIRECTION |
| Ordonnances | `GET /api/ordonnances?patientId=`, `GET /api/ordonnances/{id}` | ACCUEIL, MEDECIN (les siennes), PHARMACIEN, DIRECTION |
| Dashboard | `GET /api/dashboard/consultations` | DIRECTION |
| Dashboard | `GET /api/dashboard/chiffre-affaires` | DIRECTION |
| Dashboard | `GET /api/dashboard/impayes` | DIRECTION |
| Dashboard | `GET /api/dashboard/activite-medecins` | DIRECTION |
| Catalogue | `GET /api/actes-catalogue` | ACCUEIL, MEDECIN, DIRECTION |
| Catalogue | `POST /api/actes-catalogue`, `PUT /api/actes-catalogue/{id}`, `DELETE /api/actes-catalogue/{id}` (refusé si déjà facturé) | DIRECTION |
| Catalogue | `GET /api/actes-catalogue?tous=true` (grille complète, tarifs inactifs compris) | ACCUEIL, MEDECIN, DIRECTION, ADMIN |
| Contrôle gratuit | `GET /api/rendezvous/controle-gratuit?patientId&medecinId&date` | ACCUEIL, MEDECIN, DIRECTION, ADMIN |
| Contrôle gratuit | `POST /api/rendezvous/{id}/controle` (le médecin programme le contrôle) | MEDECIN propriétaire |
| Contrôle gratuit | `GET /api/parametres-cabinet/controle-gratuit` (tous), `PUT` | DIRECTION, ADMIN |
| Soins | `GET /api/soins?date&patientId&statut`, `GET /api/soins/{id}` | ACCUEIL, MEDECIN, DIRECTION, ADMIN |
| Soins | `POST /api/soins`, `PUT /api/soins/{id}`, `POST /api/soins/{id}/demarrer`, `/terminer`, `/annulation` | ACCUEIL, MEDECIN |
| Temps réel | `GET /api/evenements?jeton=` (flux SSE : présence, consultations, soins) | Utilisateur connecté |
| Factures | `GET /api/factures`, `GET /api/factures/{id}` | ACCUEIL, MEDECIN, PHARMACIEN, DIRECTION |
| Factures | `POST /api/factures` | ACCUEIL, PHARMACIEN |
| Factures | `POST /api/factures/{id}/annulation` (motif obligatoire, refusée si un paiement existe) | ACCUEIL |
| Paiements | `POST /api/factures/{id}/paiements` | ACCUEIL, PHARMACIEN |
| Patients | `DELETE /api/patients/{id}` refusé (`409`) si le patient a un historique | ACCUEIL |
| Pharmacie | `GET /api/pharmacie/medicaments` | MEDECIN, PHARMACIEN, DIRECTION |
| Pharmacie | `GET /api/pharmacie/prescriptions` | PHARMACIEN, DIRECTION |
| Pharmacie | `POST /api/pharmacie/medicaments`, `PATCH /api/pharmacie/medicaments/{id}/stock` | PHARMACIEN, DIRECTION |
| Pharmacie | `POST /api/pharmacie/dispensations` | PHARMACIEN |
| Pharmacie | `POST /api/pharmacie/medicaments/import` | PHARMACIEN, DIRECTION |
| Rendez-vous | `GET /api/rendezvous/creneaux?medecinId&date&dureeMinutes`, `DELETE /api/rendezvous/{id}` | ACCUEIL (créneaux : tous) |
| Actes programmés | `GET /api/actes`, `GET /api/actes/{id}` | ACCUEIL, MEDECIN (les siens), DIRECTION, ADMIN |
| Actes programmés | `POST /api/actes`, `PUT /api/actes/{id}`, `POST /api/actes/{id}/realisation` | MEDECIN |
| Actes programmés | `POST /api/actes/{id}/annulation` | MEDECIN, ACCUEIL |
| Dossier PDF | `GET /api/patients/{id}/dossier.pdf` | ACCUEIL, MEDECIN, DIRECTION, ADMIN |
| Pharmacie | `GET /api/pharmacie/medicaments/{id}`, `PUT /api/pharmacie/medicaments/{id}`, `POST .../{id}/approvisionnements` | PHARMACIEN, DIRECTION (lecture : ADMIN) |
| Pharmacie | `GET /api/pharmacie/finances`, `GET /api/pharmacie/medicaments/inventaire.pdf` | PHARMACIEN, DIRECTION, ADMIN |
| Présence | `GET /api/medecins/presence` | ACCUEIL, DIRECTION, ADMIN |
| Session | `POST /api/auth/logout`, `GET /api/auth/ping` | Utilisateur connecté |
| Administration | `/api/admin/tableau-de-bord`, `/utilisateurs`, `/sessions`, `/journal`, `/permissions` | ADMIN |
| Cabinet | `GET /api/parametres-cabinet` (tous), `PUT` (ADMIN) | Selon méthode |

### Format des erreurs

Toutes les erreurs de l'API (`GlobalExceptionHandler`) ont le même format :

```json
{ "horodatage": "...", "statut": 409, "erreur": "Conflict", "message": "Seul un rendez-vous planifié ou confirmé peut être modifié", "chemin": "/api/rendezvous/12", "champs": {} }
```

`champs` détaille les erreurs de validation (champ → message). Le frontend affiche directement `message`.

## Vérifications effectuées

Backend :

```powershell
cd backend
./mvnw.cmd test
```

Résultat : 26 tests, `BUILD SUCCESS`. `ParcoursMetierIntegrationTest` et `ParcoursCompletIntegrationTest` vérifient à travers l'API réelle (JWT compris, base H2) :

- paiement partiel puis complet, recalcul du statut, refus d'un paiement supérieur au reste dû (`422`) ;
- annulation de facture : motif obligatoire, refus si déjà annulée ou déjà encaissée ;
- droits par rôle (`403`) et refus d'un mauvais mot de passe (`401`) ;
- accès au dossier limité aux patients du médecin, compte-rendu masqué pour l'accueil ;
- refus de supprimer un patient qui a un historique ;
- modification de rendez-vous : créneau occupé et statut non modifiable refusés ;
- import de dossier puis réimport sans doublon, export PDF du dossier ;
- créneau réservé refusé à un autre patient sauf annulation, création du patient avec le rendez-vous ;
- facture unique par rendez-vous, référence obligatoire hors espèces ;
- démarrer puis terminer une consultation, programmer / réaliser / facturer un acte ;
- vente pharmacie : stock, paiement, mouvements et finances ;
- administration : comptes, révocation de session, compte désactivé, journal, présence des médecins ;
- direction : gestion des employés et de leurs sessions, flux temps réel (SSE) ;
- grille tarifaire : création, modification, désactivation, suppression refusée pour un tarif déjà facturé ;
- contrôle gratuit : refus tant que la consultation n'est pas payée, autre médecin ou hors délai non éligibles, un seul contrôle par défaut puis deux après réglage par la direction, facture à 0 soldée, contrôle programmé par le médecin ;
- soins : enregistrement, démarrage, fin, facture rattachée au soin (unique), droits par rôle.

Frontend :

```powershell
cd frontend
npm run build
```

Résultat : compilation Angular réussie avec les routes lazy-loaded (planning, consultation, dossier patient, design system).

## Scénario de démonstration final

1. **Accueil** (`accueil@test.local`, Khadijetou Mint Salem) : « Prendre un rendez-vous », créer le patient *Sidi Ould Ahmed* directement dans le formulaire, choisir un créneau libre du Dr. Mohamed Ould Cheikh, puis « Rédiger la facture maintenant » et encaisser par Bankily avec sa référence.
2. **Médecin** (`medecin@test.local`, Dr. Mohamed Ould Cheikh) : dans « Mon planning », **▶ Démarrer** la consultation, rédiger le compte-rendu, **■ Terminer**, écrire l'ordonnance en cherchant « antalgiques » dans le stock, puis programmer une chirurgie avec date, heure et lieu.
3. **Pharmacien** (`pharmacien@test.local`, Moulaye Ould Sidaty) : onglet « Ordonnances à délivrer », vendre l'ordonnance avec un paiement Masrvi et sa référence ; vérifier la fiche produit (stock et mouvements) et l'onglet Finances.
4. **Accueil** : dans « Actes programmés », générer la facture de la chirurgie après sa réalisation ; dans « Facturation », vérifier les onglets À payer / Payées.
5. **Direction** (`direction@test.local`, Ahmed Ould Abdallahi) : indicateurs du mois et présence des médecins.
6. **Administrateur** (`admin@test.local`, El Hacen Ould Mohamed Lemine) : connexions du jour, appareils connectés, journal d'activité de tout le scénario.

Migrations et parcours de bout en bout (backend démarré sur PostgreSQL) :

- Flyway applique `V1` à `V4` au démarrage ; `Successfully applied 1 migration to schema "public", now at version v4`.
- `GET /api/dashboard/consultations` → 5 consultations et 14 rendez-vous ; `GET /api/dashboard/chiffre-affaires` → 247 500 MRU facturés, 132 000 MRU encaissés, répartis sur 7 types d'acte ; `GET /api/dashboard/impayes` → 5 factures impayées sur 8, soit 63 %.
- Scénario médecin complet : `PATCH /api/rendezvous/{id}/statut`, puis `POST /api/rendezvous/{id}/consultation` (rendez-vous clôturé), puis `POST /api/consultations/{id}/prescriptions`, enfin `GET /api/patients/{id}/historique` qui renvoie les consultations avec leurs ordonnances ; une seconde consultation sur le même rendez-vous est refusée (`409`).
- Contrôle de rôle : un utilisateur `MEDECIN` reçoit `403` sur `/api/dashboard/**`.

## État d'avancement du cœur du sujet

| Exigence | État |
|---|---|
| Authentification et droits par rôle | Terminé (JWT, `SecurityConfig`, `@PreAuthorize`, guards Angular) |
| Gestion des patients | Terminé (CRUD, recherche, fiche) |
| Prise de rendez-vous et planning médecin | Terminé (contrôle de créneau, filtres, changement de statut) |
| Dossier patient côté médecin (compte-rendu, prescription) | Terminé (API + écrans `/rendez-vous/:id/consultation` et `/dossier/:patientId`) |
| Facturation multi-actes et paiements | Terminé (statut recalculé côté serveur) |
| Tableau de bord direction | Terminé (4 endpoints agrégés sur données réelles) |
| Jeu de données de démonstration | Terminé (`V4__demo_data.sql`, pharmacie `V5` à `V10`, noms mauritaniens `V13`, tarifs et soins `V14`) |
| Pharmacie interne | Terminé (stock, mouvements, vente avec paiement, finances) |
| Actes programmés (chirurgie, traitement...) | Terminé (programmation, réalisation, facturation) |
| Administration | Terminé (comptes, sessions, journal, permissions) |

## Extensions bonus

Livrées après stabilisation du cœur :

- pharmacie interne : familles, stock et mouvements, fiche produit, vente des ordonnances avec paiement, finances ;
- actes programmés : chirurgie, traitement, examen, hospitalisation et soins planifiés à un créneau du médecin, avec compte-rendu, résultat et facture ;
- administration : comptes, sessions, journal d'activité, permissions ;
- soins au cabinet (injection, perfusion, pansement, nébulisation, constantes) ;
- grille tarifaire de la direction et consultation de contrôle gratuite ;
- interface bilingue français / arabe (RTL).

Restent possibles : laboratoire (demande d'examen, résultat rattaché au dossier), gestion des salles et des lits, espace patient.

Le cœur suit le parcours :

```text
Patient -> Rendez-vous -> Consultation -> Prescription -> Pharmacie -> Facture -> Paiement -> Dashboard
```
