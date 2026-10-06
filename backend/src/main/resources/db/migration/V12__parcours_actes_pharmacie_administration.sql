-- V12 : parcours accueil / medecin, actes programmes, liens factures, pharmacie (familles, mouvements),
-- references de paiement, sessions, journal d'activite et administration.

-- 1. Administration : role ADMIN et identite des comptes -----------------------------------------
ALTER TABLE utilisateurs DROP CONSTRAINT utilisateurs_role_check;
ALTER TABLE utilisateurs ADD CONSTRAINT utilisateurs_role_check
    CHECK (role IN ('ACCUEIL', 'MEDECIN', 'DIRECTION', 'PHARMACIEN', 'ADMIN'));
ALTER TABLE utilisateurs ADD COLUMN nom VARCHAR(100);
ALTER TABLE utilisateurs ADD COLUMN prenom VARCHAR(100);
ALTER TABLE utilisateurs ADD COLUMN telephone VARCHAR(30);
ALTER TABLE utilisateurs ADD COLUMN derniere_connexion TIMESTAMP WITH TIME ZONE;

UPDATE utilisateurs u SET nom = m.nom, prenom = m.prenom FROM medecins m WHERE m.utilisateur_id = u.id;
UPDATE utilisateurs SET nom = 'Accueil', prenom = 'Secrétariat' WHERE email = 'accueil@test.local' AND nom IS NULL;
UPDATE utilisateurs SET nom = 'Direction', prenom = 'Générale' WHERE email = 'direction@test.local' AND nom IS NULL;
UPDATE utilisateurs SET nom = 'Pharmacie', prenom = 'Pharmacien' WHERE email = 'pharmacien@test.local' AND nom IS NULL;

-- Mot de passe : password (BCrypt)
INSERT INTO utilisateurs (email, mot_de_passe, role, nom, prenom)
VALUES ('admin@test.local', '$2a$10$EP2iuGHyoeL6qnPxhbhYW.XcmP7c.E3OxCMSb2CfI..Yr13wHKc9u', 'ADMIN', 'Système', 'Administrateur')
ON CONFLICT (email) DO NOTHING;

CREATE TABLE parametres_cabinet (
    id BIGINT PRIMARY KEY,
    nom VARCHAR(150) NOT NULL,
    sous_titre VARCHAR(150),
    adresse VARCHAR(255),
    telephone VARCHAR(50),
    email VARCHAR(255),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);
INSERT INTO parametres_cabinet (id, nom, sous_titre) VALUES (1, 'Cabinet Médical', 'Cabinet de groupe');

-- 2. Rendez-vous : duree du creneau et horodatage reel de la consultation --------------------------
ALTER TABLE rendez_vous ADD COLUMN duree_minutes INTEGER NOT NULL DEFAULT 30 CHECK (duree_minutes BETWEEN 5 AND 480);
ALTER TABLE rendez_vous ADD COLUMN debut_consultation TIMESTAMP WITH TIME ZONE;
ALTER TABLE rendez_vous ADD COLUMN fin_consultation TIMESTAMP WITH TIME ZONE;

-- 3. Actes programmes par le medecin (chirurgie, traitement, examen...) -----------------------------
CREATE TABLE actes_programmes (
    id BIGSERIAL PRIMARY KEY,
    patient_id BIGINT NOT NULL REFERENCES patients(id),
    medecin_id BIGINT NOT NULL REFERENCES medecins(id),
    consultation_id BIGINT REFERENCES consultations(id),
    type VARCHAR(30) NOT NULL CHECK (type IN ('CHIRURGIE', 'TRAITEMENT', 'EXAMEN', 'HOSPITALISATION', 'SOINS', 'AUTRE')),
    intitule VARCHAR(200) NOT NULL,
    details TEXT,
    date_heure TIMESTAMP NOT NULL,
    duree_minutes INTEGER NOT NULL DEFAULT 60 CHECK (duree_minutes BETWEEN 5 AND 1440),
    lieu VARCHAR(100),
    statut VARCHAR(20) NOT NULL DEFAULT 'PLANIFIE' CHECK (statut IN ('PLANIFIE', 'REALISE', 'ANNULE')),
    resultat VARCHAR(20) CHECK (resultat IN ('REUSSI', 'PARTIEL', 'ECHEC')),
    compte_rendu TEXT,
    date_realisation TIMESTAMP,
    motif_annulation VARCHAR(500),
    created_by BIGINT REFERENCES utilisateurs(id),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX idx_actes_programmes_medecin_date ON actes_programmes (medecin_id, date_heure);
CREATE INDEX idx_actes_programmes_patient ON actes_programmes (patient_id);

-- 4. Factures reliees a leur origine (rendez-vous, acte programme, dispensation) ---------------------
ALTER TABLE factures ADD COLUMN rendez_vous_id BIGINT REFERENCES rendez_vous(id);
ALTER TABLE factures ADD COLUMN acte_programme_id BIGINT REFERENCES actes_programmes(id);
ALTER TABLE dispensations ADD COLUMN facture_id BIGINT REFERENCES factures(id);
CREATE INDEX idx_factures_rendez_vous ON factures (rendez_vous_id);
CREATE INDEX idx_factures_acte_programme ON factures (acte_programme_id);

-- 5. Paiements : reference de transaction (Bankily, Masrvi, Sedad, carte, virement) -----------------
ALTER TABLE paiements ADD COLUMN reference VARCHAR(100);

-- 6. Pharmacie : familles therapeutiques et mouvements de stock --------------------------------------
ALTER TABLE medicaments ADD COLUMN famille VARCHAR(100);
UPDATE medicaments SET famille = 'Antalgiques et anti-inflammatoires' WHERE famille IS NULL AND (nom ILIKE 'parac%' OR nom ILIKE 'ibupro%');
UPDATE medicaments SET famille = 'Cardiologie' WHERE famille IS NULL AND nom ILIKE 'amlodip%';
UPDATE medicaments SET famille = 'Gastro-entérologie' WHERE famille IS NULL AND nom ILIKE 'omepra%';
UPDATE medicaments SET famille = 'Vitamines et minéraux' WHERE famille IS NULL AND nom ILIKE 'fer%';

CREATE TABLE mouvements_stock (
    id BIGSERIAL PRIMARY KEY,
    medicament_id BIGINT NOT NULL REFERENCES medicaments(id),
    type VARCHAR(20) NOT NULL CHECK (type IN ('ENTREE_INITIALE', 'ACHAT', 'VENTE', 'AJUSTEMENT')),
    quantite INTEGER NOT NULL,
    stock_apres INTEGER NOT NULL,
    prix_unitaire NUMERIC(12, 2),
    montant NUMERIC(12, 2),
    fournisseur VARCHAR(150),
    reference VARCHAR(100),
    commentaire VARCHAR(255),
    dispensation_id BIGINT REFERENCES dispensations(id),
    utilisateur_id BIGINT REFERENCES utilisateurs(id),
    date_mouvement TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX idx_mouvements_stock_medicament ON mouvements_stock (medicament_id, date_mouvement);

-- Historique reconstitue : stock initial (stock actuel + quantites deja vendues), puis les ventes passees.
INSERT INTO mouvements_stock (medicament_id, type, quantite, stock_apres, prix_unitaire, montant, commentaire, date_mouvement)
SELECT m.id, 'ENTREE_INITIALE', m.stock_actuel + COALESCE(v.vendu, 0), m.stock_actuel + COALESCE(v.vendu, 0),
       m.prix_achat, m.prix_achat * (m.stock_actuel + COALESCE(v.vendu, 0)), 'Stock initial (reprise de l''historique)',
       COALESCE((SELECT MIN(d.date_dispensation) FROM dispensations d), CURRENT_TIMESTAMP) - INTERVAL '1 day'
FROM medicaments m
LEFT JOIN (SELECT medicament_id, SUM(quantite) AS vendu FROM lignes_dispensation GROUP BY medicament_id) v ON v.medicament_id = m.id;

INSERT INTO mouvements_stock (medicament_id, type, quantite, stock_apres, prix_unitaire, montant, dispensation_id, utilisateur_id, date_mouvement)
SELECT l.medicament_id, 'VENTE', -l.quantite,
       init.quantite - SUM(l.quantite) OVER (PARTITION BY l.medicament_id ORDER BY d.date_dispensation, l.id),
       m.prix_vente, m.prix_vente * l.quantite, d.id, d.dispensee_par, d.date_dispensation
FROM lignes_dispensation l
JOIN dispensations d ON d.id = l.dispensation_id
JOIN medicaments m ON m.id = l.medicament_id
JOIN mouvements_stock init ON init.medicament_id = l.medicament_id AND init.type = 'ENTREE_INITIALE';

-- 7. Sessions de connexion et journal d'activite -----------------------------------------------------
CREATE TABLE sessions_utilisateur (
    id BIGSERIAL PRIMARY KEY,
    utilisateur_id BIGINT NOT NULL REFERENCES utilisateurs(id),
    jeton_id VARCHAR(64) NOT NULL UNIQUE,
    adresse_ip VARCHAR(64),
    appareil VARCHAR(60),
    navigateur VARCHAR(60),
    systeme VARCHAR(60),
    user_agent VARCHAR(500),
    date_connexion TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    derniere_activite TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    date_fin TIMESTAMP WITH TIME ZONE,
    motif_fin VARCHAR(20) CHECK (motif_fin IN ('DECONNEXION', 'REVOQUEE', 'EXPIREE'))
);
CREATE INDEX idx_sessions_utilisateur ON sessions_utilisateur (utilisateur_id, date_connexion);

CREATE TABLE journal_activite (
    id BIGSERIAL PRIMARY KEY,
    utilisateur_id BIGINT REFERENCES utilisateurs(id),
    email VARCHAR(255),
    role VARCHAR(20),
    action VARCHAR(60) NOT NULL,
    description VARCHAR(500),
    methode VARCHAR(10),
    chemin VARCHAR(255),
    statut_http INTEGER,
    adresse_ip VARCHAR(64),
    date_action TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX idx_journal_activite_date ON journal_activite (date_action DESC);
