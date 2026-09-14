# Cabinet médical

Application de gestion d'un cabinet médical, basée sur Spring Boot, Angular et PostgreSQL.

## Démarrage du backend

Prérequis : Java 17 et PostgreSQL.

Créer la base `cabinet_medical`, puis lancer :

```powershell
cd backend
./mvnw.cmd spring-boot:run
```

Variables disponibles : `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`, `JWT_SECRET` et `JWT_EXPIRATION_MS`.
Les migrations Flyway sont exécutées automatiquement au démarrage.

Si PostgreSQL est installé sur Windows mais que la base n'existe pas encore :

```sql
CREATE DATABASE cabinet_medical;
```

## Démarrage du frontend

Prérequis : Node.js 20 ou plus récent.

```powershell
cd frontend
npm install
npm start
```

L'application est disponible sur `http://localhost:4200` et consomme l'API sur `http://localhost:8080`.

## Comptes de test

La migration `V2__seed_data.sql` crée les comptes suivants. Le mot de passe de démonstration est `password` pour chacun.

| Rôle | Email |
|---|---|
| Accueil | `accueil@test.local` |
| Médecin | `medecin@test.local` |
| Direction | `direction@test.local` |

## Semaine 1 livrée

- socle Spring Boot, PostgreSQL et migration Flyway V1 ;
- entités `Utilisateur`, `Medecin` et `Patient` avec repositories ;
- authentification JWT et autorisation par rôle ;
- endpoints Patient ;
- socle Angular avec connexion, JWT interceptor, `AuthGuard`, `RoleGuard` et fiche patient.