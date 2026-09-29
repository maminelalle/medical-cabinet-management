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
- Tests backend : JUnit / Spring Boot Test avec H2

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

## Comptes de démonstration

La migration de seed crée les comptes suivants. Le mot de passe est `password` pour chacun.

| Rôle | Email | Accès principal |
|---|---|---|
| Accueil / Caisse | `accueil@test.local` | Patients, rendez-vous, factures, paiements |
| Médecin (médecine générale) | `medecin@test.local` | Planning personnel, consultations, prescriptions, dossiers |
| Médecin (cardiologie) | `cardio@test.local` | Planning personnel, consultations, prescriptions, dossiers |
| Médecin (chirurgie) | `chirurgie@test.local` | Planning personnel, consultations, prescriptions, dossiers |
| Direction | `direction@test.local` | Dashboard, indicateurs, lecture factures et actes |

Le mot de passe est `password` pour tous les comptes (hash BCrypt en base, jamais de mot de passe en clair).

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
| `/patients` | ACCUEIL, MEDECIN | Liste et gestion administrative des patients |
| `/rendez-vous` | ACCUEIL, MEDECIN | Planning filtrable, état loading/error et planning médecin |
| `/rendez-vous/nouveau` | ACCUEIL | Création avec patient, médecin, date, heure et motif |
| `/rendez-vous/:id/consultation` | MEDECIN | Compte-rendu de consultation et ordonnance |
| `/dossier/:patientId` | MEDECIN | Dossier médical : historique, ordonnances, prochains rendez-vous |
| `/factures` | ACCUEIL, DIRECTION | Liste, détail et encaissement |
| `/factures/nouveau` | ACCUEIL | Création d'une facture multi-actes |
| `/direction/dashboard` | DIRECTION | Indicateurs réels, chiffre d'affaires par type d'acte, impayés, activité par médecin |
| `/design-system` | Tous | Vitrine des composants de l'interface |

La redirection après connexion dépend du rôle : accueil vers `/accueil`, médecin vers `/rendez-vous`, direction vers `/direction/dashboard`.

## Endpoints REST

| Domaine | Méthode et endpoint | Accès |
|---|---|---|
| Auth | `POST /api/auth/login` | Public |
| Patients | `GET /api/patients`, `POST`, `PUT`, `DELETE` | Selon rôle |
| Médecins | `GET /api/medecins` | ACCUEIL, MEDECIN |
| Rendez-vous | `GET/POST /api/rendezvous` | ACCUEIL, MEDECIN selon action |
| Rendez-vous | `PUT /api/rendezvous/{id}` | ACCUEIL |
| Rendez-vous | `GET /api/rendezvous/{id}` | ACCUEIL, MEDECIN |
| Statut | `PATCH /api/rendezvous/{id}/statut` | ACCUEIL, MEDECIN |
| Consultation | `POST /api/rendezvous/{id}/consultation` | MEDECIN propriétaire |
| Consultation | `GET /api/consultations/{id}` | MEDECIN propriétaire |
| Prescription | `POST /api/consultations/{id}/prescriptions` | MEDECIN propriétaire |
| Dossier patient | `GET /api/patients/{id}/historique` | MEDECIN (ses patients) |
| Dashboard | `GET /api/dashboard/consultations` | DIRECTION |
| Dashboard | `GET /api/dashboard/chiffre-affaires` | DIRECTION |
| Dashboard | `GET /api/dashboard/impayes` | DIRECTION |
| Dashboard | `GET /api/dashboard/activite-medecins` | DIRECTION |
| Catalogue | `GET /api/actes-catalogue` | ACCUEIL, DIRECTION |
| Catalogue | `POST /api/actes-catalogue` | DIRECTION |
| Factures | `GET /api/factures`, `GET /api/factures/{id}` | ACCUEIL, DIRECTION |
| Factures | `POST /api/factures` | ACCUEIL |
| Paiements | `POST /api/factures/{id}/paiements` | ACCUEIL |

## Vérifications effectuées

Backend :

```powershell
cd backend
./mvnw.cmd test
```

Résultat : test de contexte Spring réussi, `BUILD SUCCESS`.

Frontend :

```powershell
cd frontend
npm run build
```

Résultat : compilation Angular réussie avec les routes lazy-loaded (planning, consultation, dossier patient, design system).

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
| Jeu de données de démonstration | Terminé (`V4__demo_data.sql`) |

## Extensions bonus (non commencées)

Conformément à la consigne de cadrage, les modules bonus ne sont pas développés tant que le cœur n'est pas stabilisé :

- pharmacie interne (catalogue de médicaments, stock, dispensation liée à une prescription) ;
- laboratoire (demande d'examen, résultat rattaché au dossier) ;
- bloc opératoire (planning des interventions avec créneau et salle) ;
- espace patient.

L'hospitalisation et l'acte chirurgical sont couverts comme **types d'actes facturables** (`HOSPITALISATION`, `CHIRURGIE`), sans module de planning de bloc ou de lits.

Le cœur suit le parcours :

```text
Patient -> Rendez-vous -> Consultation -> Prescription -> Facture -> Paiement -> Dashboard
```
