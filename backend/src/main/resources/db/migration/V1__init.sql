CREATE TABLE utilisateurs (
    id BIGSERIAL PRIMARY KEY,
    email VARCHAR(255) NOT NULL UNIQUE,
    mot_de_passe VARCHAR(255) NOT NULL,
    role VARCHAR(20) NOT NULL CHECK (role IN ('ACCUEIL', 'MEDECIN', 'DIRECTION')),
    actif BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE medecins (
    id BIGSERIAL PRIMARY KEY,
    utilisateur_id BIGINT NOT NULL UNIQUE REFERENCES utilisateurs(id),
    nom VARCHAR(100) NOT NULL,
    prenom VARCHAR(100) NOT NULL,
    specialite VARCHAR(150),
    numero_ordre VARCHAR(50)
);

CREATE TABLE patients (
    id BIGSERIAL PRIMARY KEY,
    nom VARCHAR(100) NOT NULL,
    prenom VARCHAR(100) NOT NULL,
    date_naissance DATE NOT NULL,
    telephone VARCHAR(30),
    email VARCHAR(255),
    adresse VARCHAR(255),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_patients_nom_prenom ON patients (nom, prenom);

CREATE TABLE rendez_vous (
    id BIGSERIAL PRIMARY KEY,
    patient_id BIGINT NOT NULL REFERENCES patients(id),
    medecin_id BIGINT NOT NULL REFERENCES medecins(id),
    date_heure TIMESTAMP NOT NULL,
    motif VARCHAR(500),
    statut VARCHAR(20) NOT NULL CHECK (statut IN ('PLANIFIE', 'CONFIRME', 'EN_COURS', 'TERMINE', 'ANNULE', 'ABSENT')),
    created_by BIGINT REFERENCES utilisateurs(id),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX idx_rendez_vous_medecin_date ON rendez_vous (medecin_id, date_heure);

CREATE TABLE consultations (
    id BIGSERIAL PRIMARY KEY,
    rendez_vous_id BIGINT NOT NULL UNIQUE REFERENCES rendez_vous(id),
    compte_rendu TEXT NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE prescriptions (
    id BIGSERIAL PRIMARY KEY,
    consultation_id BIGINT NOT NULL UNIQUE REFERENCES consultations(id),
    date_prescription DATE NOT NULL,
    instructions TEXT
);

CREATE TABLE lignes_prescription (
    id BIGSERIAL PRIMARY KEY,
    prescription_id BIGINT NOT NULL REFERENCES prescriptions(id),
    medicament VARCHAR(255) NOT NULL,
    posologie VARCHAR(255),
    duree VARCHAR(100)
);

CREATE TABLE catalogue_actes (
    id BIGSERIAL PRIMARY KEY,
    libelle VARCHAR(150) NOT NULL,
    type VARCHAR(50) NOT NULL,
    montant_defaut NUMERIC(12, 2) NOT NULL CHECK (montant_defaut >= 0),
    actif BOOLEAN NOT NULL DEFAULT TRUE
);

CREATE TABLE factures (
    id BIGSERIAL PRIMARY KEY,
    patient_id BIGINT NOT NULL REFERENCES patients(id),
    date_facture DATE NOT NULL,
    montant_total NUMERIC(12, 2) NOT NULL CHECK (montant_total >= 0),
    statut VARCHAR(20) NOT NULL CHECK (statut IN ('EN_ATTENTE', 'PARTIELLE', 'PAYEE', 'ANNULEE')),
    created_by BIGINT REFERENCES utilisateurs(id),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE lignes_facture (
    id BIGSERIAL PRIMARY KEY,
    facture_id BIGINT NOT NULL REFERENCES factures(id),
    catalogue_acte_id BIGINT REFERENCES catalogue_actes(id),
    libelle VARCHAR(150) NOT NULL,
    type_acte VARCHAR(50) NOT NULL,
    montant NUMERIC(12, 2) NOT NULL CHECK (montant >= 0)
);

CREATE TABLE paiements (
    id BIGSERIAL PRIMARY KEY,
    facture_id BIGINT NOT NULL REFERENCES factures(id),
    montant NUMERIC(12, 2) NOT NULL CHECK (montant > 0),
    date_paiement TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    moyen_paiement VARCHAR(50),
    enregistre_par BIGINT REFERENCES utilisateurs(id)
);