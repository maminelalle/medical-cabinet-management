CREATE TABLE medicaments (
    id BIGSERIAL PRIMARY KEY,
    nom VARCHAR(150) NOT NULL,
    dosage VARCHAR(100),
    forme VARCHAR(100),
    stock_actuel INTEGER NOT NULL CHECK (stock_actuel >= 0),
    seuil_alerte INTEGER NOT NULL CHECK (seuil_alerte >= 0),
    prix_unitaire NUMERIC(12, 2) NOT NULL CHECK (prix_unitaire >= 0),
    actif BOOLEAN NOT NULL DEFAULT TRUE
);

CREATE TABLE dispensations (
    id BIGSERIAL PRIMARY KEY,
    prescription_id BIGINT NOT NULL UNIQUE REFERENCES prescriptions(id),
    patient_id BIGINT NOT NULL REFERENCES patients(id),
    dispensee_par BIGINT NOT NULL REFERENCES utilisateurs(id),
    date_dispensation TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE lignes_dispensation (
    id BIGSERIAL PRIMARY KEY,
    dispensation_id BIGINT NOT NULL REFERENCES dispensations(id),
    medicament_id BIGINT NOT NULL REFERENCES medicaments(id),
    quantite INTEGER NOT NULL CHECK (quantite > 0)
);

INSERT INTO medicaments (nom, dosage, forme, stock_actuel, seuil_alerte, prix_unitaire) VALUES
    ('Paracetamol', '500 mg', 'Comprime', 180, 30, 25.00),
    ('Amlodipine', '5 mg', 'Comprime', 90, 20, 85.00),
    ('Ibuprofene', '400 mg', 'Comprime', 120, 25, 40.00),
    ('Omeprazole', '20 mg', 'Gelule', 75, 15, 60.00),
    ('Fer + acide folique', 'Comprime', 'Comprime', 60, 15, 55.00);