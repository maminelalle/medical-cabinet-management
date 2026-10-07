-- V14 : grille tarifaire geree par la direction, consultation de controle gratuite, soins (injection, perfusion...).

-- 1. Tarifs : tarif de consultation par specialite et description ------------------------------------
ALTER TABLE catalogue_actes ADD COLUMN specialite VARCHAR(150);
ALTER TABLE catalogue_actes ADD COLUMN description VARCHAR(255);
UPDATE catalogue_actes SET specialite = 'Medecine generale' WHERE libelle = 'Consultation de medecine generale';
INSERT INTO catalogue_actes (libelle, type, montant_defaut, specialite, description) VALUES
    ('Consultation de cardiologie', 'CONSULTATION', 7500, 'Cardiologie', 'Tarif de consultation du cardiologue'),
    ('Consultation de chirurgie', 'CONSULTATION', 7500, 'Chirurgie', 'Tarif de consultation du chirurgien'),
    ('Injection (intramusculaire ou intraveineuse)', 'SOINS', 1000, NULL, 'Produit apporté par le patient ou fourni par la pharmacie'),
    ('Pose de perfusion (sérum)', 'SOINS', 3000, NULL, 'Pose et surveillance de la perfusion'),
    ('Nébulisation (aérosol)', 'SOINS', 1500, NULL, NULL),
    ('Prise des constantes (tension, glycémie)', 'SOINS', 500, NULL, NULL);

-- 2. Consultation de controle gratuite (regle du cabinet, reglable par la direction) ------------------
ALTER TABLE parametres_cabinet ADD COLUMN controle_gratuit_actif BOOLEAN NOT NULL DEFAULT TRUE;
ALTER TABLE parametres_cabinet ADD COLUMN controle_gratuit_jours INTEGER NOT NULL DEFAULT 30
    CHECK (controle_gratuit_jours BETWEEN 1 AND 365);
ALTER TABLE parametres_cabinet ADD COLUMN controle_gratuit_nombre INTEGER NOT NULL DEFAULT 1
    CHECK (controle_gratuit_nombre BETWEEN 1 AND 10);

-- Un rendez-vous de controle reference la consultation payee qui l'a motive.
ALTER TABLE rendez_vous ADD COLUMN rendez_vous_origine_id BIGINT REFERENCES rendez_vous(id);
CREATE INDEX idx_rendez_vous_origine ON rendez_vous (rendez_vous_origine_id);

-- 3. Soins : injection, perfusion, pansement... (patient sur ordonnance du cabinet ou d'un prescripteur externe)
CREATE TABLE soins (
    id BIGSERIAL PRIMARY KEY,
    patient_id BIGINT NOT NULL REFERENCES patients(id),
    type VARCHAR(30) NOT NULL CHECK (type IN ('INJECTION', 'PERFUSION', 'PANSEMENT', 'NEBULISATION', 'CONSTANTES', 'AUTRE')),
    intitule VARCHAR(200) NOT NULL,
    produit VARCHAR(255),
    prescription_id BIGINT REFERENCES prescriptions(id),
    prescripteur_externe VARCHAR(200),
    date_heure TIMESTAMP NOT NULL,
    statut VARCHAR(20) NOT NULL DEFAULT 'EN_ATTENTE' CHECK (statut IN ('EN_ATTENTE', 'EN_COURS', 'TERMINE', 'ANNULE')),
    observations TEXT,
    debut TIMESTAMP WITH TIME ZONE,
    fin TIMESTAMP WITH TIME ZONE,
    realise_par BIGINT REFERENCES utilisateurs(id),
    created_by BIGINT REFERENCES utilisateurs(id),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX idx_soins_date ON soins (date_heure);
ALTER TABLE factures ADD COLUMN soin_id BIGINT REFERENCES soins(id);
