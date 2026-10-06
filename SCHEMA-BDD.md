# Schéma de la base de données — Cabinet Médical

Base PostgreSQL `cabinet_medical`, schéma versionné par **Flyway** (`backend/src/main/resources/db/migration`).

| Migration | Contenu |
|---|---|
| `V1__init.sql` | Schéma du cœur métier : 11 tables, contraintes `CHECK` sur les statuts, index `idx_patients_nom_prenom` et `idx_rendez_vous_medecin_date` |
| `V2__seed_data.sql` | Comptes de démonstration (3 rôles) + 1 médecin |
| `V3__fix_test_account_passwords.sql` | Correction du mot de passe de démonstration (sans réécrire V2) |
| `V4__demo_data.sql` | Jeu de données de démonstration complet : catalogue d'actes, 3 médecins, 8 patients, 14 rendez-vous, 5 consultations, 4 prescriptions, 9 factures, paiements et statuts cohérents |
| `V5__pharmacie.sql` | Catalogue initial de médicaments, stock, tables de dispensation liées aux prescriptions |
| `V6__pharmacien_role.sql` | Ajout du rôle PHARMACIEN et du compte de démonstration |
| `V7__link_prescriptions_to_medicaments.sql` | Référencement des lignes de prescription vers le stock |
| `V8__fix_pharmacien_demo_password.sql` | Correction du mot de passe de démonstration du pharmacien |
| `V9__pharmacy_commercial_fields.sql` | Prix achat/vente, fournisseur et date d'expiration des médicaments |
| `V10__appointment_queue_number.sql` | Numéro de file quotidien pour les reçus de rendez-vous |

## Diagramme entité-association

```mermaid
erDiagram
    UTILISATEURS ||--o| MEDECINS : "profil medecin"
    UTILISATEURS ||--o{ RENDEZ_VOUS : "cree"
    UTILISATEURS ||--o{ FACTURES : "cree"
    UTILISATEURS ||--o{ PAIEMENTS : "encaisse"
    PATIENTS ||--o{ RENDEZ_VOUS : "concerne"
    PATIENTS ||--o{ FACTURES : "facture"
    MEDECINS ||--o{ RENDEZ_VOUS : "consulte"
    RENDEZ_VOUS ||--o| CONSULTATIONS : "donne lieu a"
    CONSULTATIONS ||--o| PRESCRIPTIONS : "prescrit"
    PRESCRIPTIONS ||--o{ LIGNES_PRESCRIPTION : "contient"
    PRESCRIPTIONS ||--o| DISPENSATIONS : "est delivree"
    DISPENSATIONS ||--o{ LIGNES_DISPENSATION : "contient"
    MEDICAMENTS ||--o{ LIGNES_DISPENSATION : "sort du stock"
    FACTURES ||--o{ LIGNES_FACTURE : "regroupe"
    FACTURES ||--o{ PAIEMENTS : "encaisse"
    CATALOGUE_ACTES ||--o{ LIGNES_FACTURE : "reprend le tarif"
    RENDEZ_VOUS ||--o{ FACTURES : "est facture par"
    PATIENTS ||--o{ ACTES_PROGRAMMES : "subit"
    MEDECINS ||--o{ ACTES_PROGRAMMES : "programme"
    CONSULTATIONS ||--o{ ACTES_PROGRAMMES : "decide"
    ACTES_PROGRAMMES ||--o{ FACTURES : "est facture par"
    DISPENSATIONS ||--o| FACTURES : "genere"
    MEDICAMENTS ||--o{ MOUVEMENTS_STOCK : "historique"
    DISPENSATIONS ||--o{ MOUVEMENTS_STOCK : "sorties de vente"
    UTILISATEURS ||--o{ SESSIONS_UTILISATEUR : "se connecte"
    UTILISATEURS ||--o{ JOURNAL_ACTIVITE : "agit"

    UTILISATEURS {
        bigint id PK
        varchar email UK
        varchar mot_de_passe "hash BCrypt"
        varchar role "ACCUEIL | MEDECIN | DIRECTION | PHARMACIEN | ADMIN"
        boolean actif
        timestamptz created_at
    }
    MEDECINS {
        bigint id PK
        bigint utilisateur_id FK,UK
        varchar nom
        varchar prenom
        varchar specialite
        varchar numero_ordre
    }
    PATIENTS {
        bigint id PK
        varchar nom
        varchar prenom
        date date_naissance
        varchar telephone
        varchar email
        varchar adresse
        timestamptz created_at
        timestamptz updated_at
    }
    RENDEZ_VOUS {
        bigint id PK
        bigint patient_id FK
        bigint medecin_id FK
        timestamp date_heure "creneau unique par medecin"
        varchar motif
        integer numero_file
        varchar statut "PLANIFIE | CONFIRME | EN_COURS | TERMINE | ANNULE | ABSENT"
        bigint created_by FK
        timestamptz created_at
    }
    CONSULTATIONS {
        bigint id PK
        bigint rendez_vous_id FK,UK "un-à-un"
        text compte_rendu
        timestamptz created_at
    }
    PRESCRIPTIONS {
        bigint id PK
        bigint consultation_id FK,UK "un-à-un"
        date date_prescription
        text instructions
    }
    LIGNES_PRESCRIPTION {
        bigint id PK
        bigint prescription_id FK
        bigint medicament_id FK
        varchar medicament
        varchar posologie
        varchar duree
    }
    MEDICAMENTS {
        bigint id PK
        varchar nom
        varchar dosage
        varchar forme
        integer stock_actuel
        integer seuil_alerte
        numeric prix_unitaire
        numeric prix_achat
        numeric prix_vente
        varchar fournisseur
        date date_expiration
        boolean actif
    }
    DISPENSATIONS {
        bigint id PK
        bigint prescription_id FK,UK
        bigint patient_id FK
        bigint dispensee_par FK
        timestamptz date_dispensation
    }
    LIGNES_DISPENSATION {
        bigint id PK
        bigint dispensation_id FK
        bigint medicament_id FK
        integer quantite
    }
    CATALOGUE_ACTES {
        bigint id PK
        varchar libelle
        varchar type "CONSULTATION | HOSPITALISATION | CHIRURGIE | LABORATOIRE | IMAGERIE | PHARMACIE | SOINS"
        numeric montant_defaut
        boolean actif
    }
    FACTURES {
        bigint id PK
        bigint patient_id FK
        date date_facture
        numeric montant_total
        varchar statut "EN_ATTENTE | PARTIELLE | PAYEE | ANNULEE"
        bigint created_by FK
        timestamptz created_at
    }
    LIGNES_FACTURE {
        bigint id PK
        bigint facture_id FK
        bigint catalogue_acte_id FK "nullable : montant ajustable"
        varchar libelle
        varchar type_acte
        numeric montant
    }
    PAIEMENTS {
        bigint id PK
        bigint facture_id FK
        numeric montant
        timestamptz date_paiement
        varchar moyen_paiement "ESPECES | BANKILY | MASRVI | SEDAD | CARTE | VIREMENT | CHEQUE"
        varchar reference "obligatoire hors especes"
        bigint enregistre_par FK
    }
    ACTES_PROGRAMMES {
        bigint id PK
        bigint patient_id FK
        bigint medecin_id FK
        bigint consultation_id FK "nullable"
        varchar type "CHIRURGIE | TRAITEMENT | EXAMEN | HOSPITALISATION | SOINS | AUTRE"
        varchar intitule
        text details
        timestamp date_heure
        int duree_minutes
        varchar statut "PLANIFIE | REALISE | ANNULE"
        varchar resultat "REUSSI | PARTIEL | ECHEC"
        text compte_rendu
    }
    MOUVEMENTS_STOCK {
        bigint id PK
        bigint medicament_id FK
        varchar type "ENTREE_INITIALE | ACHAT | VENTE | AJUSTEMENT"
        int quantite "signee"
        int stock_apres
        numeric montant
        bigint dispensation_id FK "nullable"
        bigint utilisateur_id FK
    }
    SESSIONS_UTILISATEUR {
        bigint id PK
        bigint utilisateur_id FK
        varchar jeton_id UK "claim jti du JWT"
        varchar adresse_ip
        varchar appareil
        varchar navigateur
        timestamptz derniere_activite
        timestamptz date_fin
        varchar motif_fin "DECONNEXION | REVOQUEE | EXPIREE"
    }
    JOURNAL_ACTIVITE {
        bigint id PK
        bigint utilisateur_id FK
        varchar action
        varchar description "jamais de donnee medicale"
        varchar adresse_ip
        timestamptz date_action
    }
    PARAMETRES_CABINET {
        bigint id PK "ligne unique"
        varchar nom
        varchar adresse
        varchar telephone
    }
```

## Règles de gestion portées par le schéma

- **Un rendez-vous = un créneau médecin** : chaque rendez-vous et chaque acte programmé occupe `[date_heure, date_heure + duree_minutes[` ; `DisponibiliteService` refuse (409) tout chevauchement avec un rendez-vous non annulé / non absent ou un acte planifié du même médecin.
- **Facture reliée à son origine** : `factures.rendez_vous_id`, `factures.acte_programme_id` ou `dispensations.facture_id` ; une seule facture non annulée par rendez-vous ou par acte. Un rendez-vous ou un acte facturé ne peut être annulé qu'après annulation de sa facture.
- **Stock tracé** : tout changement de `medicaments.stock_actuel` (stock initial, achat, vente, correction d'inventaire) crée une ligne `mouvements_stock` avec le stock résultant et la valeur.
- **Sessions** : le JWT porte l'identifiant de session (`jti`) ; une session fermée (déconnexion, révocation par l'administrateur, compte désactivé) invalide son jeton.
- **Un rendez-vous honoré donne au plus une consultation** : `consultations.rendez_vous_id` est `UNIQUE` ; la création clôture le rendez-vous (`TERMINE`).
- **Une consultation porte au plus une ordonnance** : `prescriptions.consultation_id` est `UNIQUE`.
- **Une facture regroupe plusieurs actes** : relation un-à-plusieurs `factures → lignes_facture` ; chaque ligne porte `libelle`, `type_acte` et `montant` (le type d'acte couvre consultation, hospitalisation, acte chirurgical, etc.).
- **Les paiements sont séparés de la facture** : relation un-à-plusieurs `factures → paiements`, ce qui autorise les règlements partiels ; le statut (`EN_ATTENTE`, `PARTIELLE`, `PAYEE`) est recalculé côté serveur à chaque encaissement.
- **Tracabilité** : `rendez_vous.created_by`, `factures.created_by` et `paiements.enregistre_par` référencent `utilisateurs` (qui a fait quoi).
- **Pharmacie** : une prescription ne peut être dispensée qu'une fois ; chaque ligne vérifie le stock disponible puis le décrémente dans la même transaction. Les médicaments sous le seuil d'alerte sont signalés dans l'interface.
- **Extensions restantes sans refonte** : laboratoire (`examens` rattachés à `consultations`) et bloc opératoire (`interventions` rattachées à `patients`/`medecins`).

## Export du schéma

Pour régénérer le schéma depuis PostgreSQL :

```powershell
pg_dump --schema-only --no-owner --dbname=cabinet_medical > schema.sql
```